package com.smarthealth.healthdata.dto;

import com.smarthealth.healthdata.model.HealthRecordEntity;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

public class HealthRecordResponse implements Serializable {

    private String id;
    private UUID patientId;
    private Instant recordedAt;
    private Integer ageYears;
    private String sex;
    private Double heightCm;
    private Double weightKg;
    private Double bmi;
    private Integer systolicBp;
    private Integer diastolicBp;
    private Double fastingGlucoseMgDl;
    private Double hba1cPercent;
    private Double totalCholesterolMgDl;
    private Double hdlMgDl;
    private Double ldlMgDl;
    private String smokingStatus;
    private String activityLevel;
    private Boolean hasHypertension;
    private Boolean hasHeartDisease;
    private Boolean familyHistoryDiabetes;

    public HealthRecordResponse() {
    }

    public static HealthRecordResponse fromEntity(HealthRecordEntity entity) {
        HealthRecordResponse res = new HealthRecordResponse();
        res.id = entity.getId();
        res.patientId = entity.getPatientId() != null ? UUID.fromString(entity.getPatientId()) : null;
        res.recordedAt = entity.getRecordedAt();
        res.ageYears = entity.getAgeYears();
        res.sex = entity.getSex();
        res.heightCm = entity.getHeightCm();
        res.weightKg = entity.getWeightKg();
        res.bmi = entity.getBmi();
        res.systolicBp = entity.getSystolicBp();
        res.diastolicBp = entity.getDiastolicBp();
        res.fastingGlucoseMgDl = entity.getFastingGlucoseMgDl();
        res.hba1cPercent = entity.getHba1cPercent();
        res.totalCholesterolMgDl = entity.getTotalCholesterolMgDl();
        res.hdlMgDl = entity.getHdlMgDl();
        res.ldlMgDl = entity.getLdlMgDl();
        res.smokingStatus = entity.getSmokingStatus();
        res.activityLevel = entity.getActivityLevel();
        res.hasHypertension = entity.getHasHypertension();
        res.hasHeartDisease = entity.getHasHeartDisease();
        res.familyHistoryDiabetes = entity.getFamilyHistoryDiabetes();
        return res;
    }

    public String getId() { return id; }
    public UUID getPatientId() { return patientId; }
    public Instant getRecordedAt() { return recordedAt; }
    public Integer getAgeYears() { return ageYears; }
    public String getSex() { return sex; }
    public Double getHeightCm() { return heightCm; }
    public Double getWeightKg() { return weightKg; }
    public Double getBmi() { return bmi; }
    public Integer getSystolicBp() { return systolicBp; }
    public Integer getDiastolicBp() { return diastolicBp; }
    public Double getFastingGlucoseMgDl() { return fastingGlucoseMgDl; }
    public Double getHba1cPercent() { return hba1cPercent; }
    public Double getTotalCholesterolMgDl() { return totalCholesterolMgDl; }
    public Double getHdlMgDl() { return hdlMgDl; }
    public Double getLdlMgDl() { return ldlMgDl; }
    public String getSmokingStatus() { return smokingStatus; }
    public String getActivityLevel() { return activityLevel; }
    public Boolean getHasHypertension() { return hasHypertension; }
    public Boolean getHasHeartDisease() { return hasHeartDisease; }
    public Boolean getFamilyHistoryDiabetes() { return familyHistoryDiabetes; }
}
