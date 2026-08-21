package com.example.bestpractices.outbox;

import com.example.bestpractices.messaging.UserEvent;
import com.example.bestpractices.messaging.UserEventType;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Best practices demonstrated:
 * - Propagation.MANDATORY enforces the caller contract: this method MUST be invoked
 *   inside an already-open transaction. If called outside one (e.g. missing @Transactional
 *   on the caller), Spring throws IllegalTransactionStateException at runtime — catching
 *   the bug immediately rather than silently losing events.
 * - Separating outbox persistence into its own service keeps UserService focused
 *   on business logic while still guaranteeing the write-together semantic.
 */
@Service
@RequiredArgsConstructor
public class OutboxEventService {

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void saveEvent(String aggregateType, Long aggregateId,
                          UserEventType eventType, UserEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            repository.save(OutboxEvent.create(aggregateType, aggregateId, eventType, payload));
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to serialise outbox event payload", e);
        }
    }
}
