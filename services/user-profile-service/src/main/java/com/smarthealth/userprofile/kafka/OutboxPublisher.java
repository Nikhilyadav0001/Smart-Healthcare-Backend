package com.smarthealth.userprofile.kafka;

import java.util.concurrent.TimeUnit;

import com.smarthealth.userprofile.model.OutboxEvent;
import com.smarthealth.userprofile.repository.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional(rollbackFor = Exception.class)
    public void publishPending() throws Exception {
        for (OutboxEvent e : repo.lockBatch(50)) {
            kafka.send(e.getTopic(), e.getEventKey(), e.getPayload()).get(5, TimeUnit.SECONDS);
            e.markPublished();
            log.info("Published outbox event to {} key={}", e.getTopic(), e.getEventKey());
        }
    }
}