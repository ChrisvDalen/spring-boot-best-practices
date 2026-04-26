package com.example.bestpractices.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Best practices demonstrated:
 * - Single-responsibility: knows only how to convert a UserEvent to an AMQP message
 * - Routing key map avoids a switch statement and makes adding new event types a 1-line change
 * - Logs at DEBUG before send and INFO after — lets you trace message flow without noise
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    private static final Map<UserEventType, String> ROUTING_KEYS = Map.of(
            UserEventType.CREATED, RabbitMqConfig.RK_USER_CREATED,
            UserEventType.UPDATED, RabbitMqConfig.RK_USER_UPDATED,
            UserEventType.DELETED, RabbitMqConfig.RK_USER_DELETED
    );

    public void publish(UserEvent event) {
        String routingKey = ROUTING_KEYS.get(event.eventType());
        log.debug("Publishing {} event for userId={} routingKey={}",
                event.eventType(), event.userId(), routingKey);
        rabbitTemplate.convertAndSend(RabbitMqConfig.USERS_EXCHANGE, routingKey, event);
        log.info("Published event eventId={} type={} userId={}",
                event.eventId(), event.eventType(), event.userId());
    }
}
