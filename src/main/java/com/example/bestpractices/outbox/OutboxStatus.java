package com.example.bestpractices.outbox;

public enum OutboxStatus {
    /** Saved in same transaction as the domain write; not yet published. */
    PENDING,
    /** Successfully published to the message broker. */
    PUBLISHED,
    /** Failed at least once; still within retry budget. */
    FAILED,
    /** Exhausted all retries; requires manual intervention. */
    DEAD
}
