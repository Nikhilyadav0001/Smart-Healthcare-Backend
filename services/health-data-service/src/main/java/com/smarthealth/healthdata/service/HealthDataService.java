package com.smarthealth.healthdata.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthealth.healthdata.dto.CreateRecordRequest;
import com.smarthealth.healthdata.dto.HealthRecordResponse;
import com.smarthealth.healthdata.model.AccessAudit;
import com.smarthealth.healthdata.model.HealthRecordEntity;
import com.smarthealth.healthdata.model.OutboxEvent;
import com.smarthealth.healthdata.repository.AccessAuditRepository;
import com.smarthealth.healthdata.repository.HealthRecordRepository;
import com.smarthealth.healthdata.repository.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class HealthDataService {

    private static final Logger log = LoggerFactory.getLogger(HealthDataService.class);

    private final HealthRecordRepository healthRecordRepository;
    private final OutboxRepository outboxRepository;
    private final AccessAuditRepository accessAuditRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public HealthDataService(
            HealthRecordRepository healthRecordRepository,
            OutboxRepository outboxRepository,
            AccessAuditRepository accessAuditRepository) {
        this.healthRecordRepository = healthRecordRepository;
        this.outboxRepository = outboxRepository;
        this.accessAuditRepository = accessAuditRepository;
    }

    @Transactional
    @CacheEvict(value = "latest_health_records", key = "#patientId")
    public HealthRecordResponse createRecord(UUID patientId, CreateRecordRequest req) {
        HealthRecordEntity entity = new HealthRecordEntity();
        entity.setPatientId(patientId.toString());
        entity.setRecordedAt(Instant.now());
        entity.setAgeYears(req.getAgeYears());
        entity.setSex(req.getSex().toUpperCase());
        entity.setHeightCm(req.getHeightCm());
        entity.setWeightKg(req.getWeightKg());

        Double bmi = req.getBmi();
        if (bmi == null && req.getHeightCm() != null && req.getWeightKg() != null && req.getHeightCm() > 0) {
            double hMeters = req.getHeightCm() / 100.0;
            bmi = Math.round((req.getWeightKg() / (hMeters * hMeters)) * 10.0) / 10.0;
        }
        entity.setBmi(bmi);

        entity.setSystolicBp(req.getSystolicBp());
        entity.setDiastolicBp(req.getDiastolicBp());
        entity.setFastingGlucoseMgDl(req.getFastingGlucoseMgDl());
        entity.setHba1cPercent(req.getHba1cPercent());
        entity.setTotalCholesterolMgDl(req.getTotalCholesterolMgDl());
        entity.setHdlMgDl(req.getHdlMgDl());
        entity.setLdlMgDl(req.getLdlMgDl());
        entity.setSmokingStatus(req.getSmokingStatus() != null ? req.getSmokingStatus().toUpperCase() : "NEVER");
        entity.setActivityLevel(req.getActivityLevel() != null ? req.getActivityLevel().toUpperCase() : "MODERATE");
        entity.setHasHypertension(Boolean.TRUE.equals(req.getHasHypertension()));
        entity.setHasHeartDisease(Boolean.TRUE.equals(req.getHasHeartDisease()));
        entity.setFamilyHistoryDiabetes(Boolean.TRUE.equals(req.getFamilyHistoryDiabetes()));

        entity = healthRecordRepository.save(entity);

        // Record outbox event in same transaction
        try {
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("eventId", UUID.randomUUID().toString());
            event.put("schemaVersion", 1);
            event.put("occurredAt", Instant.now().toString());
            event.put("patientId", patientId.toString());
            event.put("recordId", entity.getId());

            String payload = objectMapper.writeValueAsString(event);
            OutboxEvent outbox = OutboxEvent.of("health.record.created", patientId.toString(), payload);
            outboxRepository.save(outbox);
            log.info("Saved health.record.created outbox event for recordId={}", entity.getId());
        } catch (Exception e) {
            log.error("Failed to construct health record outbox event: {}", e.getMessage(), e);
        }

        return HealthRecordResponse.fromEntity(entity);
    }

    @Cacheable(value = "latest_health_records", key = "#patientId", unless = "#result == null")
    public Optional<HealthRecordResponse> getLatestRecord(UUID patientId) {
        return healthRecordRepository.findFirstByPatientIdOrderByRecordedAtDesc(patientId.toString())
                .map(HealthRecordResponse::fromEntity);
    }

    public List<HealthRecordResponse> getRecordsForPatient(UUID patientId) {
        return healthRecordRepository.findByPatientIdOrderByRecordedAtDesc(patientId.toString())
                .stream()
                .map(HealthRecordResponse::fromEntity)
                .toList();
    }

    public List<HealthRecordResponse> getDoctorAuditedRecords(UUID patientId, String doctorId, String ipAddress) {
        AccessAudit audit = AccessAudit.of(patientId.toString(), doctorId, "READ_RECORD", ipAddress);
        accessAuditRepository.save(audit);
        log.info("Audited doctor access for patientId={} by doctorId={}", patientId, doctorId);

        return getRecordsForPatient(patientId);
    }

    public Optional<HealthRecordResponse> getRecordById(String id) {
        return healthRecordRepository.findById(id)
                .map(HealthRecordResponse::fromEntity);
    }

    @Transactional
    @CacheEvict(value = "latest_health_records", key = "#patientId")
    public void deletePatientData(UUID patientId) {
        healthRecordRepository.deleteByPatientId(patientId.toString());
        log.info("Deleted health records for patientId={}", patientId);
    }
}
