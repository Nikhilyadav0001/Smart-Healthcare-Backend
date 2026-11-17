package com.smarthealth.healthdata.kafka;

import com.smarthealth.healthdata.model.OutboxEvent;
import com.smarthealth.healthdata.repository.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxRepository repo;
    private final KafkaTemplate<String, String> kafka;

    public OutboxPublisher(OutboxRepository repo, KafkaTemplate<String, String> kafka) {
        this.repo = repo;
        this.kafka = kafka;
    }

    @Scheduled(fixedDelay = 1000)
    public void publishPending() {
        List<OutboxEvent> pending = repo.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc();
        for (OutboxEvent e : pending) {
            try {
                kafka.send(e.getTopic(), e.getEventKey(), e.getPayload()).get(5, TimeUnit.SECONDS);
                e.markPublished();
                repo.save(e);
                log.info("Published health-data outbox event to topic={} key={}", e.getTopic(), e.getEventKey());
            } catch (Exception ex) {
                log.error("Failed to publish outbox event id={} to topic={}: {}", e.getId(), e.getTopic(), ex.getMessage());
                break;
            }
        }
    }
}
