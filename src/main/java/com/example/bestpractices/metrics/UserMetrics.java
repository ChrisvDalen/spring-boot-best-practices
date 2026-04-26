package com.example.bestpractices.metrics;

import io.micrometer.core.instrument.*;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Best practices demonstrated:
 * - Centralise all business metrics in one class so naming conventions and tags are consistent
 * - Counter for events (created, deleted, outbox states) — monotonically increasing, used for rate queries
 * - Timer for latency-sensitive operations (findById) — gives p50/p95/p99 in Prometheus/Grafana
 * - Gauge for point-in-time state (pending outbox depth) — useful for lag alerting
 * - Descriptive metric names follow Micrometer conventions: noun.verb.unit (users.created.total)
 * - Tags allow slicing dashboards without creating separate metrics per variant
 */
@Component
public class UserMetrics {

    private final Counter userCreatedCounter;
    private final Counter userDeletedCounter;
    private final Counter deadLetterCounter;
    private final Counter outboxPublishedCounter;
    private final Counter outboxDeadCounter;
    private final Timer userFindTimer;

    public UserMetrics(MeterRegistry registry) {
        this.userCreatedCounter = Counter.builder("users.created.total")
                .description("Total number of users successfully created")
                .register(registry);

        this.userDeletedCounter = Counter.builder("users.deleted.total")
                .description("Total number of users deleted")
                .register(registry);

        this.deadLetterCounter = Counter.builder("messaging.dead_letters.total")
                .description("Total number of dead-letter messages received")
                .tag("queue", "users.dlq")
                .register(registry);

        this.outboxPublishedCounter = Counter.builder("outbox.published.total")
                .description("Total outbox events successfully published to the broker")
                .register(registry);

        this.outboxDeadCounter = Counter.builder("outbox.dead.total")
                .description("Total outbox events that exhausted retries and moved to DEAD status")
                .register(registry);

        this.userFindTimer = Timer.builder("users.find.duration")
                .description("Time taken to retrieve a single user (cache miss path)")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }

    public void recordUserCreated() {
        userCreatedCounter.increment();
    }

    public void recordUserDeleted() {
        userDeletedCounter.increment();
    }

    public void recordDeadLetterReceived() {
        deadLetterCounter.increment();
    }

    public void recordOutboxPublished() {
        outboxPublishedCounter.increment();
    }

    public void recordOutboxDead() {
        outboxDeadCounter.increment();
    }

    public <T> T timeUserFind(Supplier<T> operation) {
        return userFindTimer.record(operation);
    }
}
