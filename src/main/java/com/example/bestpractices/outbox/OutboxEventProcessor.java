package com.example.bestpractices.outbox;

import com.example.bestpractices.messaging.UserEvent;
import com.example.bestpractices.messaging.UserEventPublisher;
import com.example.bestpractices.metrics.UserMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Best practices demonstrated:
 * - @Scheduled(fixedDelay) — next run starts only after the previous one finishes,
 *   preventing concurrent executions on the same node from fighting over the same rows
 * - Batch processing (configurable size) — avoids loading unbounded sets into memory
 * - Explicit retry budget (MAX_RETRIES) + DEAD status — prevents infinite retry storms
 * - @Transactional on the poll loop means status updates and the read happen in one
 *   unit of work; if publishing throws, the status update rolls back too
 * - In a multi-node deployment you would replace this with SELECT FOR UPDATE SKIP LOCKED
 *   or use a distributed lock (Redisson) so only one node processes each event
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventProcessor {

    private static final int BATCH_SIZE = 100;
    private static final int MAX_RETRIES = 3;

    private final OutboxEventRepository repository;
    private final UserEventPublisher publisher;
    private final ObjectMapper objectMapper;
    private final UserMetrics userMetrics;

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:1000}")
    @Transactional
    public void processOutbox() {
        List<OutboxEvent> batch = repository.findPendingBatch(PageRequest.of(0, BATCH_SIZE));
        if (batch.isEmpty()) return;

        log.debug("Outbox poll: processing {} events", batch.size());

        for (OutboxEvent outboxEvent : batch) {
            try {
                UserEvent userEvent = objectMapper.readValue(outboxEvent.getPayload(), UserEvent.class);
                publisher.publish(userEvent);

                outboxEvent.setStatus(OutboxStatus.PUBLISHED);
                outboxEvent.setProcessedAt(Instant.now());
                userMetrics.recordOutboxPublished();

            } catch (Exception ex) {
                int attempts = outboxEvent.getRetryCount() + 1;
                outboxEvent.setRetryCount(attempts);
                outboxEvent.setErrorMessage(truncate(ex.getMessage(), 990));

                if (attempts >= MAX_RETRIES) {
                    outboxEvent.setStatus(OutboxStatus.DEAD);
                    log.error("Outbox event id={} moved to DEAD after {} retries: {}",
                            outboxEvent.getId(), attempts, ex.getMessage());
                    userMetrics.recordOutboxDead();
                } else {
                    outboxEvent.setStatus(OutboxStatus.FAILED);
                    log.warn("Outbox event id={} attempt {}/{} failed: {}",
                            outboxEvent.getId(), attempts, MAX_RETRIES, ex.getMessage());
                }
            }
        }
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }
}
