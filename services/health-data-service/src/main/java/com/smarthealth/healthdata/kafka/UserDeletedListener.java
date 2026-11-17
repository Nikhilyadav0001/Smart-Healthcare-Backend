package com.smarthealth.healthdata.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthealth.healthdata.model.ProcessedEvent;
import com.smarthealth.healthdata.repository.ProcessedEventRepository;
import com.smarthealth.healthdata.service.HealthDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UserDeletedListener {

    private static final Logger log = LoggerFactory.getLogger(UserDeletedListener.class);

    private final HealthDataService healthDataService;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public UserDeletedListener(HealthDataService healthDataService, ProcessedEventRepository processedEventRepository) {
        this.healthDataService = healthDataService;
        this.processedEventRepository = processedEventRepository;
    }

    @KafkaListener(topics = "user.deleted", groupId = "health-data-service")
    public void onUserDeleted(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String eventId = root.path("eventId").asText();
            String userId = root.path("userId").asText();

            if (eventId != null && !eventId.isBlank()) {
                if (processedEventRepository.existsById(eventId)) {
                    log.info("user.deleted event already processed eventId={}, skipping", eventId);
                    return;
                }
            }

            log.info("Consuming user.deleted for userId={}", userId);
            healthDataService.deletePatientData(UUID.fromString(userId));

            if (eventId != null && !eventId.isBlank()) {
                processedEventRepository.save(new ProcessedEvent(eventId));
            }

        } catch (Exception e) {
            log.error("Failed to process user.deleted event: {}", e.getMessage(), e);
        }
    }
}
