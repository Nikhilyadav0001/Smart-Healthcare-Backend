package com.smarthealth.userprofile.model;

import jakarta.persistence.*;

@Entity
@Table(name = "processed_events")
public class ProcessedEvent {
    @Id
    @Column(name = "event_id", length = 36)
    private String eventId;

    protected ProcessedEvent() {

    }
    public ProcessedEvent(String eventId) {
        this.eventId = eventId;
    }
}