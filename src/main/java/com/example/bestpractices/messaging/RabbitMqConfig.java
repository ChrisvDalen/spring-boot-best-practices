package com.example.bestpractices.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Best practices demonstrated:
 * - Topic exchange enables routing by pattern (user.* catches all user events)
 * - Dead Letter Exchange (DLX) + Dead Letter Queue (DLQ) — messages that cannot
 *   be processed after max retries are parked in the DLQ for inspection / replay
 * - Publisher confirms (mandatory=true + confirmCallback) detect silent message loss
 * - Manual consumer ack mode — message stays in-flight until consumer explicitly acks
 *   or nacks, preventing data loss on consumer crash
 * - JacksonJsonMessageConverter gives human-readable JSON on the wire instead of
 *   Java serialisation blobs; decouples producers from consumer class paths
 * - prefetchCount=10 limits how many unacked messages a consumer holds at once,
 *   acting as a per-consumer rate limiter
 */
@Slf4j
@Configuration
public class RabbitMqConfig {

    // ── Exchange names ─────────────────────────────────────────────────────────
    public static final String USERS_EXCHANGE = "users.events";
    public static final String DEAD_LETTER_EXCHANGE = "users.dlx";

    // ── Queue names ────────────────────────────────────────────────────────────
    public static final String USERS_QUEUE = "users.queue";
    public static final String DEAD_LETTER_QUEUE = "users.dlq";

    // ── Routing keys ───────────────────────────────────────────────────────────
    public static final String RK_USER_CREATED = "user.created";
    public static final String RK_USER_UPDATED = "user.updated";
    public static final String RK_USER_DELETED = "user.deleted";

    // ── Exchanges ──────────────────────────────────────────────────────────────

    @Bean
    TopicExchange usersExchange() {
        return ExchangeBuilder.topicExchange(USERS_EXCHANGE).durable(true).build();
    }

    @Bean
    DirectExchange deadLetterExchange() {
        return ExchangeBuilder.directExchange(DEAD_LETTER_EXCHANGE).durable(true).build();
    }

    // ── Queues ─────────────────────────────────────────────────────────────────

    @Bean
    Queue usersQueue() {
        return QueueBuilder.durable(USERS_QUEUE)
                // Rejected/expired messages are re-routed to the DLX
                .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DEAD_LETTER_QUEUE)
                // Messages older than 5 minutes are considered stale and DLQ'd
                .withArgument("x-message-ttl", 300_000)
                .build();
    }

    @Bean
    Queue deadLetterQueue() {
        // DLQ itself has no further DLX — it's the end of the line
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    // ── Bindings ───────────────────────────────────────────────────────────────

    @Bean
    Binding createdBinding(TopicExchange usersExchange, Queue usersQueue) {
        return BindingBuilder.bind(usersQueue).to(usersExchange).with(RK_USER_CREATED);
    }

    @Bean
    Binding updatedBinding(TopicExchange usersExchange, Queue usersQueue) {
        return BindingBuilder.bind(usersQueue).to(usersExchange).with(RK_USER_UPDATED);
    }

    @Bean
    Binding deletedBinding(TopicExchange usersExchange, Queue usersQueue) {
        return BindingBuilder.bind(usersQueue).to(usersExchange).with(RK_USER_DELETED);
    }

    @Bean
    Binding dlqBinding(DirectExchange deadLetterExchange, Queue deadLetterQueue) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(DEAD_LETTER_QUEUE);
    }

    // ── Infrastructure beans ───────────────────────────────────────────────────

    @Bean
    MessageConverter jacksonJsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                  MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        // mandatory=true triggers a ReturnCallback if no queue accepts the message
        template.setMandatory(true);
        template.setReturnsCallback(returned ->
                log.error("Message returned unroutable: exchange={} routingKey={} replyText={}",
                        returned.getExchange(), returned.getRoutingKey(), returned.getReplyText()));
        template.setConfirmCallback((correlation, ack, reason) -> {
            if (!ack) {
                log.error("Publisher confirm NACK: correlation={} reason={}", correlation, reason);
            }
        });
        return template;
    }

    @Bean
    SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        // MANUAL ack — consumer calls channel.basicAck/basicNack explicitly
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        // defaultRequeueRejected=false sends nacked messages to DLX instead of re-queuing
        factory.setDefaultRequeueRejected(false);
        // Limit unacked messages per consumer to avoid overwhelming slow processors
        factory.setPrefetchCount(10);
        return factory;
    }
}
