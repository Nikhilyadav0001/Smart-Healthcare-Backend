package com.smarthealth.userprofile.repository;

import java.util.List;

import com.smarthealth.userprofile.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxRepository extends JpaRepository<OutboxEvent, String> {
    @Query(value = "SELECT * FROM outbox_events WHERE published_at IS NULL ORDER BY created_at LIMIT :n FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    List<OutboxEvent> lockBatch(@Param("n") int n);
}