package com.smarthealth.prediction.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "ml_predictions")
public class MlPredictionEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id = UUID.randomUUID();

    @Column(name = "patient_id")
    private UUID patientId;

    @Column(name = "patient_name")
    private String patientName;

    @Column(name = "disease", nullable = false)
    private String disease;

    @Column(name = "probability", nullable = false)
    private Double probability;

    @Column(name = "threshold", nullable = false)
    private Double threshold;

    @Column(name = "is_at_risk", nullable = false)
    private Boolean isAtRisk;

    @Column(name = "risk_level")
    private String riskLevel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "top_factors", columnDefinition = "jsonb")
    private List<String> topFactors;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "recommendations", columnDefinition = "jsonb")
    private List<String> recommendations;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "features", columnDefinition = "jsonb")
    private Map<String, Object> features;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    public MlPredictionEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getPatientId() { return patientId; }
    public void setPatientId(UUID patientId) { this.patientId = patientId; }
    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public String getDisease() { return disease; }
    public void setDisease(String disease) { this.disease = disease; }
    public Double getProbability() { return probability; }
    public void setProbability(Double probability) { this.probability = probability; }
    public Double getThreshold() { return threshold; }
    public void setThreshold(Double threshold) { this.threshold = threshold; }
    public Boolean getIsAtRisk() { return isAtRisk; }
    public void setIsAtRisk(Boolean isAtRisk) { this.isAtRisk = isAtRisk; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public List<String> getTopFactors() { return topFactors; }
    public void setTopFactors(List<String> topFactors) { this.topFactors = topFactors; }
    public List<String> getRecommendations() { return recommendations; }
    public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
    public Map<String, Object> getFeatures() { return features; }
    public void setFeatures(Map<String, Object> features) { this.features = features; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
