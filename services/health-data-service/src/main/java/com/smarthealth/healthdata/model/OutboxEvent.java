package com.smarthealth.healthdata.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

@Document(collection = "outbox_events")
public class OutboxEvent {

    @Id
    private String id;

    private String topic;

    @Indexed
    private String eventKey;

    private String payload;

    private Instant createdAt = Instant.now();

    @Indexed
    private Instant publishedAt;

    public OutboxEvent() {
    }

    public static OutboxEvent of(String topic, String key, String payload) {
        OutboxEvent e = new OutboxEvent();
        e.id = UUID.randomUUID().toString();
        e.topic = topic;
        e.eventKey = key;
        e.payload = payload;
        e.createdAt = Instant.now();
        return e;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public String getEventKey() { return eventKey; }
    public void setEventKey(String eventKey) { this.eventKey = eventKey; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getPublishedAt() { return publishedAt; }
    public void markPublished() { this.publishedAt = Instant.now(); }
}
