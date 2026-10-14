package com.smarthealth.userprofile.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false)
    private String topic;

    @Column(name = "event_key", nullable = false)
    private String eventKey;

    @Column(nullable = false)
    private String payload;

    @Column(name = "published_at")
    private Instant publishedAt;

    protected OutboxEvent() {

    }

    public static OutboxEvent of(String topic, String key, String payload) {
        OutboxEvent e = new OutboxEvent();
        e.id = UUID.randomUUID().toString();
        e.topic = topic;
        e.eventKey = key;
        e.payload = payload;
        return e;
    }

    public String getTopic() {
        return topic; }
    public String getEventKey() {
        return eventKey; }
    public String getPayload() {
        return payload; }
    public void markPublished() {
        this.publishedAt = Instant.now(); }
}