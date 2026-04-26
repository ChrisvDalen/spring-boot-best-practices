package com.example.bestpractices.outbox;

import com.example.bestpractices.messaging.UserEventType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Best practices demonstrated:
 * - Transactional Outbox Pattern: written in the SAME database transaction as the domain
 *   aggregate. If the transaction rolls back, the event is never created. If it commits,
 *   the event is guaranteed to be eventually published — even if the broker is down.
 * - UUID primary key avoids serial-sequence contention under high write load
 * - Index on (status, created_at) supports the polling query efficiently
 * - retryCount + errorMessage enable operational visibility without tailing logs
 * - payload stored as JSON TEXT so the processor can deserialise to any event type
 */
@Entity
@Table(
    name = "outbox_events",
    indexes = @Index(name = "idx_outbox_status_created", columnList = "status, created_at")
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, length = 100)
    private String aggregateType;

    @Column(nullable = false)
    private Long aggregateId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private UserEventType eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status = OutboxStatus.PENDING;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant processedAt;

    @Column(nullable = false)
    private int retryCount = 0;

    @Column(length = 1000)
    private String errorMessage;

    public static OutboxEvent create(String aggregateType, Long aggregateId,
                                     UserEventType eventType, String payload) {
        OutboxEvent e = new OutboxEvent();
        e.aggregateType = aggregateType;
        e.aggregateId = aggregateId;
        e.eventType = eventType;
        e.payload = payload;
        return e;
    }
}
