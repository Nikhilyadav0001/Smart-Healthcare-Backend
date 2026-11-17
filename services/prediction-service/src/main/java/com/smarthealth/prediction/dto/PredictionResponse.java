package com.smarthealth.prediction.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class PredictionResponse {

    @JsonProperty("prediction_id")
    private UUID predictionId = UUID.randomUUID();

    @JsonProperty("patient_id")
    private UUID patientId;

    @JsonProperty("patient_name")
    private String patientName;

    @JsonProperty("patient_email")
    private String patientEmail;

    @JsonProperty("evaluated_at")
    private Instant evaluatedAt = Instant.now();

    private Map<String, DiseaseRiskReport> predictions;

    @JsonProperty("overall_health_score")
    private Double overallHealthScore;

    private String summary;

    public UUID getPredictionId() { return predictionId; }
    public void setPredictionId(UUID predictionId) { this.predictionId = predictionId; }
    public UUID getPatientId() { return patientId; }
    public void setPatientId(UUID patientId) { this.patientId = patientId; }
    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public String getPatientEmail() { return patientEmail; }
    public void setPatientEmail(String patientEmail) { this.patientEmail = patientEmail; }
    public Instant getEvaluatedAt() { return evaluatedAt; }
    public void setEvaluatedAt(Instant evaluatedAt) { this.evaluatedAt = evaluatedAt; }
    public Map<String, DiseaseRiskReport> getPredictions() { return predictions; }
    public void setPredictions(Map<String, DiseaseRiskReport> predictions) { this.predictions = predictions; }
    public Double getOverallHealthScore() { return overallHealthScore; }
    public void setOverallHealthScore(Double overallHealthScore) { this.overallHealthScore = overallHealthScore; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
}
