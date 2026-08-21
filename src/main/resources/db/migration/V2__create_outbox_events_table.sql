-- Transactional Outbox table — see OutboxEvent.java for the pattern description.
--
-- Best practices demonstrated:
-- - UUID primary key (VARCHAR(36)) avoids serial-sequence hot-spots under concurrent inserts
-- - Composite index on (status, created_at) matches the polling query exactly:
--   WHERE status IN ('PENDING','FAILED') ORDER BY created_at ASC
-- - TEXT payload stores JSON; no schema coupling between outbox and event shape
-- - retry_count + error_message provide operational visibility for on-call engineers

CREATE TABLE outbox_events
(
    id             VARCHAR(36)  NOT NULL PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id   BIGINT       NOT NULL,
    event_type     VARCHAR(50)  NOT NULL,
    payload        TEXT         NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    processed_at   TIMESTAMP WITH TIME ZONE,
    retry_count    INT          NOT NULL DEFAULT 0,
    error_message  VARCHAR(1000)
);

CREATE INDEX idx_outbox_status_created ON outbox_events (status, created_at);
