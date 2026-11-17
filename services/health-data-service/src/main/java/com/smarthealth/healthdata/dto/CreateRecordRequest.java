package com.smarthealth.healthdata.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class CreateRecordRequest {

    private UUID patientId;

    @NotNull
    @Min(1)
    @Max(125)
    private Integer ageYears;

    @NotNull
    private String sex; // MALE, FEMALE, OTHER

    @Min(50)
    @Max(260)
    private Double heightCm;

    @Min(20)
    @Max(350)
    private Double weightKg;

    private Double bmi;

    @NotNull
    @Min(60)
    @Max(260)
    private Integer systolicBp;

    @NotNull
    @Min(40)
    @Max(160)
    private Integer diastolicBp;

    private Double fastingGlucoseMgDl;
    private Double hba1cPercent;
    private Double totalCholesterolMgDl;
    private Double hdlMgDl;
    private Double ldlMgDl;

    private String smokingStatus = "NEVER";
    private String activityLevel = "MODERATE";

    private Boolean hasHypertension = false;
    private Boolean hasHeartDisease = false;
    private Boolean familyHistoryDiabetes = false;

    public CreateRecordRequest() {
    }

    public UUID getPatientId() { return patientId; }
    public void setPatientId(UUID patientId) { this.patientId = patientId; }

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
