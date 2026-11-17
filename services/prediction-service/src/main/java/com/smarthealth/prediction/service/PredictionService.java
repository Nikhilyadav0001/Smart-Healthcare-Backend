package com.smarthealth.prediction.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthealth.health.v1.HealthRecord;
import com.smarthealth.prediction.dto.*;
import com.smarthealth.prediction.grpc.HealthDataServiceClient;
import com.smarthealth.prediction.grpc.ReportServiceClient;
import com.smarthealth.prediction.ml.ModelEngine;
import com.smarthealth.prediction.ml.RulesEngine;
import com.smarthealth.prediction.model.MlPredictionEntity;
import com.smarthealth.prediction.model.OutboxEvent;
import com.smarthealth.prediction.repository.MlPredictionRepository;
import com.smarthealth.prediction.repository.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class PredictionService {

    private static final Logger log = LoggerFactory.getLogger(PredictionService.class);

    private final ModelEngine modelEngine;
    private final RulesEngine rulesEngine;
    private final MlPredictionRepository repository;
    private final OutboxRepository outboxRepository;
    private final ReportServiceClient reportServiceClient;
    private final HealthDataServiceClient healthServiceClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PredictionService(
            ModelEngine modelEngine,
            RulesEngine rulesEngine,
            MlPredictionRepository repository,
            OutboxRepository outboxRepository,
            ReportServiceClient reportServiceClient,
            HealthDataServiceClient healthServiceClient) {
        this.modelEngine = modelEngine;
        this.rulesEngine = rulesEngine;
        this.repository = repository;
        this.outboxRepository = outboxRepository;
        this.reportServiceClient = reportServiceClient;
        this.healthServiceClient = healthServiceClient;
    }

    public Set<String> getAvailableModels() {
        return modelEngine.getAvailableModels();
    }

    public Map<String, Double> getThresholds() {
        Map<String, Double> map = new LinkedHashMap<>();
        for (String m : modelEngine.getAvailableModels()) {
            map.put(m, modelEngine.getThreshold(m));
        }
        return map;
    }

    @Transactional
    public PredictionResponse predictAll(MultiPredictionRequest request) {
        PatientFeaturesDto features = resolveFeatures(request.getPatientId(), request.getFeatures());
        Map<String, Object> featureMap = features.toFeatureMap();

        List<String> targetDiseases = List.of("diabetes_risk", "heart_disease_risk", "hypertension_risk");

        Map<String, DiseaseRiskReport> reports = new LinkedHashMap<>();
        double sumProb = 0.0;
        int alertCount = 0;
        MlPredictionEntity primaryEntity = null;

        for (String disease : targetDiseases) {
            double prob = modelEngine.predictProbability(disease, featureMap);
            double threshold = modelEngine.getThreshold(disease);
            boolean isAtRisk = prob >= threshold;
            String riskLevel = rulesEngine.computeRiskLevel(prob, threshold);
            List<String> factors = rulesEngine.extractTopFactors(disease, features);
            List<String> recs = rulesEngine.generateRecommendations(disease, riskLevel, features);

            reports.put(disease, new DiseaseRiskReport(disease, prob, threshold, isAtRisk, riskLevel, factors, recs));
            sumProb += prob;
            if (isAtRisk) {
                alertCount++;
            }

            // Persist evaluation to PostgreSQL
            MlPredictionEntity entity = new MlPredictionEntity();
            entity.setPatientId(request.getPatientId());
            entity.setPatientName(request.getPatientName());
            entity.setDisease(disease);
            entity.setProbability(prob);
            entity.setThreshold(threshold);
            entity.setIsAtRisk(isAtRisk);
            entity.setRiskLevel(riskLevel);
            entity.setTopFactors(factors);
            entity.setRecommendations(recs);
            entity.setFeatures(featureMap);
            entity.setCreatedAt(Instant.now());
            entity = repository.save(entity);

            if (primaryEntity == null) {
                primaryEntity = entity;
            }

            // Record outbox event for Kafka (prediction.completed)
            saveOutboxEvent(entity, request.getPatientEmail());
        }

        double avgRisk = sumProb / targetDiseases.size();
        double healthScore = Math.max(5.0, Math.min(99.0, 100.0 - (avgRisk * 80.0) - (alertCount * 5.0)));
        healthScore = Math.round(healthScore * 10.0) / 10.0;

        String summary;
        if (alertCount >= 2) {
            summary = String.format("High multi-morbid risk profile detected across %d categories. Immediate clinical physician follow-up required.", alertCount);
        } else if (alertCount == 1) {
            summary = "Elevated risk flagged in 1 health domain exceeding decision thresholds. Targeted intervention recommended.";
        } else if (healthScore < 80.0) {
            summary = "Borderline metabolic markers detected. Proactive dietary and cardiovascular management advised.";
        } else {
            summary = "All evaluated parameters within optimal/acceptable predictive margins. Continue preventive lifestyle management.";
        }

        PredictionResponse response = new PredictionResponse();
        if (primaryEntity != null) {
            response.setPredictionId(primaryEntity.getId());
        }
        response.setPatientId(request.getPatientId());
        response.setPatientName(request.getPatientName());
        response.setPatientEmail(request.getPatientEmail());
        response.setEvaluatedAt(Instant.now());
        response.setPredictions(reports);
        response.setOverallHealthScore(healthScore);
        response.setSummary(summary);

        return response;
    }

    @Transactional
    public PredictionResponse predictSingle(SinglePredictionRequest request) {
        String disease = request.getDisease();
        PatientFeaturesDto features = resolveFeatures(request.getPatientId(), request.getFeatures());
        Map<String, Object> featureMap = features.toFeatureMap();

        double prob = modelEngine.predictProbability(disease, featureMap);
        double threshold = modelEngine.getThreshold(disease);
        boolean isAtRisk = prob >= threshold;
        String riskLevel = rulesEngine.computeRiskLevel(prob, threshold);
        List<String> factors = rulesEngine.extractTopFactors(disease, features);
        List<String> recs = rulesEngine.generateRecommendations(disease, riskLevel, features);

        DiseaseRiskReport report = new DiseaseRiskReport(disease, prob, threshold, isAtRisk, riskLevel, factors, recs);

        MlPredictionEntity entity = new MlPredictionEntity();
        entity.setPatientId(request.getPatientId());
        entity.setPatientName(request.getPatientName());
        entity.setDisease(disease);
        entity.setProbability(prob);
        entity.setThreshold(threshold);
        entity.setIsAtRisk(isAtRisk);
        entity.setRiskLevel(riskLevel);
        entity.setTopFactors(factors);
        entity.setRecommendations(recs);
        entity.setFeatures(featureMap);
        entity.setCreatedAt(Instant.now());
        entity = repository.save(entity);

        // Record outbox event for Kafka
        saveOutboxEvent(entity, request.getPatientEmail());

        double healthScore = Math.round((100.0 - (prob * 70.0)) * 10.0) / 10.0;
        String summary = isAtRisk
                ? String.format("Elevated %s risk flagged above threshold (%.2f >= %.2f).", disease, prob, threshold)
                : String.format("%s risk is within acceptable physiological margin.", disease);

        PredictionResponse response = new PredictionResponse();
        response.setPredictionId(entity.getId());
        response.setPatientId(request.getPatientId());
        response.setPatientName(request.getPatientName());
        response.setPatientEmail(request.getPatientEmail());
        response.setEvaluatedAt(Instant.now());
        response.setPredictions(Map.of(disease, report));
        response.setOverallHealthScore(healthScore);
        response.setSummary(summary);

        return response;
    }

    public List<MlPredictionEntity> getHistory(UUID patientId) {
        return repository.findByPatientIdOrderByCreatedAtDesc(patientId);
    }

    public Optional<MlPredictionEntity> getPrediction(UUID predictionId) {
        return repository.findById(predictionId);
    }

    public byte[] generateReportPdf(UUID predictionId) {
        MlPredictionEntity entity = repository.findById(predictionId)
                .orElseThrow(() -> new NoSuchElementException("Prediction record not found for id: " + predictionId));

        return reportServiceClient.generateClinicalReportPdf(
                entity.getPatientName() != null ? entity.getPatientName() : "Patient",
                entity.getDisease(),
                entity.getProbability(),
                entity.getRiskLevel(),
                entity.getTopFactors(),
                entity.getRecommendations()
        );
    }

    private void saveOutboxEvent(MlPredictionEntity entity, String patientEmail) {
        try {
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("eventId", UUID.randomUUID().toString());
            event.put("schemaVersion", 1);
            event.put("occurredAt", Instant.now().toString());
            event.put("predictionId", entity.getId().toString());
            event.put("patientId", entity.getPatientId() != null ? entity.getPatientId().toString() : "");
            event.put("patientName", entity.getPatientName() != null ? entity.getPatientName() : "Patient");
            event.put("patientEmail", patientEmail != null ? patientEmail : "patient@smarthealth.local");
            event.put("disease", entity.getDisease());
            event.put("probability", entity.getProbability());
            event.put("riskLevel", entity.getRiskLevel());
            event.put("isAtRisk", entity.getIsAtRisk());
            event.put("topFactors", entity.getTopFactors());
            event.put("recommendations", entity.getRecommendations());

            String payload = objectMapper.writeValueAsString(event);
            OutboxEvent outbox = OutboxEvent.of("prediction.completed", entity.getPatientId() != null ? entity.getPatientId().toString() : "", payload);
            outboxRepository.save(outbox);
            log.info("Saved prediction.completed outbox event for predictionId={}", entity.getId());
        } catch (Exception e) {
            log.error("Failed to construct outbox event: {}", e.getMessage(), e);
        }
    }

    private PatientFeaturesDto resolveFeatures(UUID patientId, PatientFeaturesDto provided) {
        if (provided != null && provided.getAge() != null) {
            return provided;
        }

        if (patientId != null) {
            try {
                Optional<HealthRecord> recordOpt = healthServiceClient.getLatestRecord(patientId);
                if (recordOpt.isPresent() && !recordOpt.get().getId().isBlank()) {
                    log.info("Fetched vitals via gRPC from health-data-service for patientId={}", patientId);
                    return mapProtoToFeatures(recordOpt.get());
                }
            } catch (Exception e) {
                log.warn("Could not fetch remote health record for patientId={}: {}", patientId, e.getMessage());
            }
        }

        return provided != null ? provided : new PatientFeaturesDto();
    }

    private PatientFeaturesDto mapProtoToFeatures(HealthRecord r) {
        PatientFeaturesDto dto = new PatientFeaturesDto();
        dto.setAge(r.getAgeYears() > 0 ? r.getAgeYears() : 45);
        dto.setSex(r.getSexValue() == 2 ? "Female" : "Male");
        dto.setHeightCm(r.getHeightCm() > 0 ? r.getHeightCm() : 168.0);
        dto.setWeightKg(r.getWeightKg() > 0 ? r.getWeightKg() : 70.0);
        dto.setBmi(r.getBmi() > 0 ? r.getBmi() : 24.5);
        dto.setSystolicBp(r.getSystolicBp() > 0 ? (double) r.getSystolicBp() : 120.0);
        dto.setDiastolicBp(r.getDiastolicBp() > 0 ? (double) r.getDiastolicBp() : 80.0);
        dto.setGlucoseMgDl(r.getFastingGlucoseMgDl() > 0 ? r.getFastingGlucoseMgDl() : 95.0);
        dto.setCholesterolMgDl(r.getTotalCholesterolMgDl() > 0 ? r.getTotalCholesterolMgDl() : 190.0);
        dto.setSmoking(r.getSmokingStatusValue() == 2 ? 1 : 0);
        dto.setPhysicalActivity(r.getActivityLevelValue() > 1 ? "Moderate" : "Sedentary");
        dto.setFamilyHistoryDiabetes(r.getFamilyHistoryDiabetes() ? 1 : 0);
        dto.setFamilyHistoryHeartDisease(r.getHasHeartDisease() ? 1 : 0);
        return dto;
    }
}
