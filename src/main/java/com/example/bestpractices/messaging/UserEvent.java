package com.example.bestpractices.messaging;

import com.example.bestpractices.user.dto.UserResponse;

import java.time.Instant;
import java.util.UUID;

/**
 * Best practices demonstrated:
 * - Immutable event record — events are facts; they never change after creation
 * - Carries its own eventId for idempotent downstream consumers
 * - correlationId threads the event back to the originating HTTP request in logs
 * - occurredAt uses Instant (UTC) so consumers in any timezone see a consistent wall-clock
 */
public record UserEvent(
        String eventId,
        UserEventType eventType,
        Long userId,
        String username,
        String email,
        String firstName,
        String lastName,
        String correlationId,
        Instant occurredAt
) {
    public static UserEvent of(UserEventType type, UserResponse user, String correlationId) {
        return new UserEvent(
                UUID.randomUUID().toString(),
                type,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                correlationId,
                Instant.now()
        );
    }

    /** Deleted-event constructor — carries only identity, no PII. */
    public static UserEvent deleted(Long userId, String correlationId) {
        return new UserEvent(
                UUID.randomUUID().toString(),
                UserEventType.DELETED,
                userId,
                null,
                null,
                null,
                null,
                correlationId,
                Instant.now()
        );
    }
}
