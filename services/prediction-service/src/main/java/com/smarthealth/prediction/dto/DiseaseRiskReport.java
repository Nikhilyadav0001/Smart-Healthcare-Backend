package com.smarthealth.prediction.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class DiseaseRiskReport {

    private String disease;
    private Double probability;
    private Double threshold;

    @JsonProperty("is_at_risk")
    private Boolean isAtRisk;

    @JsonProperty("risk_level")
    private String riskLevel;

    @JsonProperty("top_factors")
    private List<String> topFactors;

    private List<String> recommendations;

    public DiseaseRiskReport() {}

    public DiseaseRiskReport(String disease, Double probability, Double threshold,
                             Boolean isAtRisk, String riskLevel,
                             List<String> topFactors, List<String> recommendations) {
        this.disease = disease;
        this.probability = probability;
        this.threshold = threshold;
        this.isAtRisk = isAtRisk;
        this.riskLevel = riskLevel;
        this.topFactors = topFactors;
        this.recommendations = recommendations;
    }

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
}
