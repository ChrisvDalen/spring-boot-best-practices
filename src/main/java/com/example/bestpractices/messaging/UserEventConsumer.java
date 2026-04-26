package com.example.bestpractices.messaging;

import com.example.bestpractices.metrics.UserMetrics;
import com.example.bestpractices.resilience.ExternalEmailService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Best practices demonstrated:
 * - Manual acknowledgement (AcknowledgeMode.MANUAL set in RabbitMqConfig) — message
 *   is not removed from the broker until we explicitly call basicAck; a crash before
 *   ack causes the broker to re-deliver it to another consumer
 * - basicNack(tag, false, false) on processing failure — second false means do NOT
 *   re-queue; the message goes to the DLX → DLQ for inspection instead of looping
 * - Separate @RabbitListener on the DLQ for observability/alerting on dead letters
 * - Switch expression over event type keeps handler routing exhaustive and compiler-checked
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventConsumer {

    private final UserMetrics userMetrics;
    private final ExternalEmailService externalEmailService;

    @RabbitListener(queues = RabbitMqConfig.USERS_QUEUE,
            containerFactory = "rabbitListenerContainerFactory")
    public void handle(UserEvent event,
                       Channel channel,
                       @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.info("Received event eventId={} type={} userId={}",
                event.eventId(), event.eventType(), event.userId());
        try {
            switch (event.eventType()) {
                case CREATED -> handleCreated(event);
                case UPDATED -> handleUpdated(event);
                case DELETED -> handleDeleted(event);
            }
            channel.basicAck(deliveryTag, false);
        } catch (Exception ex) {
            log.error("Failed to process event eventId={}: {}", event.eventId(), ex.getMessage(), ex);
            // requeue=false → DLX → DLQ
            channel.basicNack(deliveryTag, false, false);
        }
    }

    /** Separate listener — dead letters are processed for alerting/replay, never silently dropped. */
    @RabbitListener(queues = RabbitMqConfig.DEAD_LETTER_QUEUE)
    public void handleDeadLetter(UserEvent event) {
        log.error("DEAD LETTER received: eventId={} type={} userId={}",
                event.eventId(), event.eventType(), event.userId());
        userMetrics.recordDeadLetterReceived();
        // In production: page on-call, store in dead_letter_events table, or push to alerting system
    }

    private void handleCreated(UserEvent event) {
        log.info("Processing USER_CREATED: userId={} username={}", event.userId(), event.username());
        userMetrics.recordUserCreated();
        // Trigger welcome email through circuit-breaker-protected external service
        externalEmailService.sendWelcomeEmail(event.email(), event.firstName());
    }

    private void handleUpdated(UserEvent event) {
        log.info("Processing USER_UPDATED: userId={}", event.userId());
        // Could trigger downstream projections, search index updates, etc.
    }

    private void handleDeleted(UserEvent event) {
        log.info("Processing USER_DELETED: userId={}", event.userId());
        userMetrics.recordUserDeleted();
        // Could trigger GDPR erasure jobs, downstream cascade deletes, etc.
    }
}
