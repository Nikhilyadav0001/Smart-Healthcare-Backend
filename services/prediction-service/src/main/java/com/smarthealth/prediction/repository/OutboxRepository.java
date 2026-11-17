package com.smarthealth.prediction.repository;

import com.smarthealth.prediction.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEvent, String> {

    @Query(value = "SELECT * FROM outbox_events WHERE published_at IS NULL ORDER BY created_at LIMIT :n FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    List<OutboxEvent> lockBatch(@Param("n") int n);
}
