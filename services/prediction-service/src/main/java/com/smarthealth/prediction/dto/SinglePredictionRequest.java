package com.smarthealth.prediction.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public class SinglePredictionRequest {

    @JsonProperty("patient_id")
    private UUID patientId = UUID.randomUUID();

    @JsonProperty("patient_name")
    private String patientName = "Anonymous Patient";

    @JsonProperty("patient_email")
    private String patientEmail;

    private String disease;

    private PatientFeaturesDto features;

    public UUID getPatientId() { return patientId != null ? patientId : UUID.randomUUID(); }
    public void setPatientId(UUID patientId) { this.patientId = patientId; }
    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public String getPatientEmail() { return patientEmail; }
    public void setPatientEmail(String patientEmail) { this.patientEmail = patientEmail; }
    public String getDisease() { return disease; }
    public void setDisease(String disease) { this.disease = disease; }
    public PatientFeaturesDto getFeatures() { return features != null ? features : new PatientFeaturesDto(); }
    public void setFeatures(PatientFeaturesDto features) { this.features = features; }
}
