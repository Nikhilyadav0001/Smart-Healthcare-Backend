package com.smarthealth.healthdata.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "health_records")
@CompoundIndex(name = "patient_recorded_idx", def = "{'patientId': 1, 'recordedAt': -1}")
public class HealthRecordEntity {

    @Id
    private String id;

    @Indexed
    private String patientId;

    private Instant recordedAt = Instant.now();

    private Integer ageYears;
    private String sex; // MALE, FEMALE, OTHER
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
    private String smokingStatus; // NEVER, FORMER, CURRENT
    private String activityLevel; // LOW, MODERATE, HIGH
    private Boolean hasHypertension = false;
    private Boolean hasHeartDisease = false;
    private Boolean familyHistoryDiabetes = false;

    public HealthRecordEntity() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public Instant getRecordedAt() { return recordedAt; }
    public void setRecordedAt(Instant recordedAt) { this.recordedAt = recordedAt; }

    public Integer getAgeYears() { return ageYears; }
    public void setAgeYears(Integer ageYears) { this.ageYears = ageYears; }

    public String getSex() { return sex; }
    public void setSex(String sex) { this.sex = sex; }

    public Double getHeightCm() { return heightCm; }
    public void setHeightCm(Double heightCm) { this.heightCm = heightCm; }

    public Double getWeightKg() { return weightKg; }
    public void setWeightKg(Double weightKg) { this.weightKg = weightKg; }

    public Double getBmi() { return bmi; }
    public void setBmi(Double bmi) { this.bmi = bmi; }

    public Integer getSystolicBp() { return systolicBp; }
    public void setSystolicBp(Integer systolicBp) { this.systolicBp = systolicBp; }

    public Integer getDiastolicBp() { return diastolicBp; }
    public void setDiastolicBp(Integer diastolicBp) { this.diastolicBp = diastolicBp; }

    public Double getFastingGlucoseMgDl() { return fastingGlucoseMgDl; }
    public void setFastingGlucoseMgDl(Double fastingGlucoseMgDl) { this.fastingGlucoseMgDl = fastingGlucoseMgDl; }

    public Double getHba1cPercent() { return hba1cPercent; }
    public void setHba1cPercent(Double hba1cPercent) { this.hba1cPercent = hba1cPercent; }

    public Double getTotalCholesterolMgDl() { return totalCholesterolMgDl; }
    public void setTotalCholesterolMgDl(Double totalCholesterolMgDl) { this.totalCholesterolMgDl = totalCholesterolMgDl; }

    public Double getHdlMgDl() { return hdlMgDl; }
    public void setHdlMgDl(Double hdlMgDl) { this.hdlMgDl = hdlMgDl; }

    public Double getLdlMgDl() { return ldlMgDl; }
    public void setLdlMgDl(Double ldlMgDl) { this.ldlMgDl = ldlMgDl; }

    public String getSmokingStatus() { return smokingStatus; }
    public void setSmokingStatus(String smokingStatus) { this.smokingStatus = smokingStatus; }

    public String getActivityLevel() { return activityLevel; }
    public void setActivityLevel(String activityLevel) { this.activityLevel = activityLevel; }

    public Boolean getHasHypertension() { return hasHypertension; }
    public void setHasHypertension(Boolean hasHypertension) { this.hasHypertension = hasHypertension; }

    public Boolean getHasHeartDisease() { return hasHeartDisease; }
    public void setHasHeartDisease(Boolean hasHeartDisease) { this.hasHeartDisease = hasHeartDisease; }

    public Boolean getFamilyHistoryDiabetes() { return familyHistoryDiabetes; }
    public void setFamilyHistoryDiabetes(Boolean familyHistoryDiabetes) { this.familyHistoryDiabetes = familyHistoryDiabetes; }
}
