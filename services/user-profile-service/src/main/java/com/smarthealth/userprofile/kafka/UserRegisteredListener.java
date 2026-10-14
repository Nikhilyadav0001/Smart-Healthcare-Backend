package com.smarthealth.userprofile.kafka;

import com.smarthealth.userprofile.service.RegistrationHandler;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class UserRegisteredListener {
    private static final Logger log = LoggerFactory.getLogger(UserRegisteredListener.class);
    private final RegistrationHandler handler;

    public UserRegisteredListener(RegistrationHandler handler) {
        this.handler = handler;
    }

    @KafkaListener(topics = "user.registered")
    public void onMessage(ConsumerRecord<String, String> rec) {
        try {
            handler.handle(rec.value());
        } catch (IllegalArgumentException e) {
            // Bad business input -> tell auth-service via the saga. Transient errors (DB down) propagate and Kafka retries.
            log.warn("Rejecting user.registered key={}: {}", rec.key(), e.getMessage());
            handler.recordFailure(rec.key(), e.getMessage());
        }
    }
}