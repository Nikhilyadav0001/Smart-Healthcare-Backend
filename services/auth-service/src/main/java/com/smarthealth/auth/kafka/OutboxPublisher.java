package com.smarthealth.auth.kafka;

import com.smarthealth.auth.model.OutboxEvent;
import com.smarthealth.auth.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            @Autowired(required = false) KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        if (kafkaTemplate == null) {
            log.warn("KafkaTemplate bean not found; outbox publishing is disabled.");
        } else {
            log.info("OutboxPublisher initialized with KafkaTemplate.");
        }
    }

    @Scheduled(fixedDelay = 1000)
    @Transactional(rollbackFor = Exception.class)
    public void publishPending() {
        if (kafkaTemplate == null) {
            return;
        }

        List<OutboxEvent> pending;
        try {
            pending = outboxEventRepository.findPendingEvents(20);
        } catch (Exception ex) {
            log.error("Failed to query pending outbox events: {}", ex.getMessage(), ex);
            return;
        }

        if (pending == null || pending.isEmpty()) {
            return;
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        for (OutboxEvent event : pending) {
            try {
                String topic = event.getEventType();
                String key = event.getAggregateId() != null
                        ? event.getAggregateId().toString()
                        : UUID.randomUUID().toString();

                kafkaTemplate.send(topic, key, event.getPayload()).get(5, TimeUnit.SECONDS);

                event.setStatus("PUBLISHED");
                event.setPublishedAt(now);
                outboxEventRepository.save(event);
                log.info("Published outbox event id={} type={} key={}", event.getId(), topic, key);
            } catch (Exception ex) {
                log.error("Failed to publish outbox event id={}: {}", event.getId(), ex.getMessage());
                event.setAttempts(event.getAttempts() + 1);
                event.setAvailableAt(now.plusSeconds(5L * Math.min(event.getAttempts(), 10)));
                try {
                    outboxEventRepository.save(event);
                } catch (Exception saveEx) {
                    log.error("Failed to update outbox event attempt count: {}", saveEx.getMessage());
                }
            }
        }
    }
}
