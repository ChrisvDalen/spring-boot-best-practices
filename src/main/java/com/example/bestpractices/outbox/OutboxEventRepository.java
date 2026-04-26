package com.example.bestpractices.outbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {

    @Query("SELECT e FROM OutboxEvent e WHERE e.status IN ('PENDING', 'FAILED') ORDER BY e.createdAt ASC")
    List<OutboxEvent> findPendingBatch(Pageable pageable);

    long countByStatus(OutboxStatus status);
}
