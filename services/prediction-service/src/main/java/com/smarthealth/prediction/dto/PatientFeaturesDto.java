package com.smarthealth.prediction.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;

public class PatientFeaturesDto {

    private Integer age = 45;
    private String sex = "Female";

    @JsonProperty("height_cm")
    private Double heightCm = 168.0;

    @JsonProperty("weight_kg")
    private Double weightKg = 70.0;

    private Double bmi;

    @JsonProperty("systolic_bp")
    private Double systolicBp = 120.0;

    @JsonProperty("diastolic_bp")
    private Double diastolicBp = 80.0;

    @JsonProperty("glucose_mg_dl")
    private Double glucoseMgDl = 95.0;

    @JsonProperty("cholesterol_mg_dl")
    private Double cholesterolMgDl = 190.0;

    @JsonProperty("resting_heart_rate")
    private Double restingHeartRate = 72.0;

    @JsonProperty("physical_activity")
    private String physicalActivity = "Moderate";

    private Integer smoking = 0;

    @JsonProperty("family_history_diabetes")
    private Integer familyHistoryDiabetes = 0;

    @JsonProperty("family_history_heart_disease")
    private Integer familyHistoryHeartDisease = 0;

    @JsonProperty("frequent_urination")
    private Integer frequentUrination = 0;

    @JsonProperty("excessive_thirst")
    private Integer excessiveThirst = 0;

    @JsonProperty("unexplained_weight_loss")
    private Integer unexplainedWeightLoss = 0;

    private Integer fatigue = 0;

    @JsonProperty("blurred_vision")
    private Integer blurredVision = 0;

    @JsonProperty("chest_pain")
    private Integer chestPain = 0;

    @JsonProperty("shortness_of_breath")
    private Integer shortnessOfBreath = 0;

    private Integer palpitations = 0;
    private Integer headache = 0;
    private Integer dizziness = 0;
    private Integer cough = 0;
    private Integer wheezing = 0;
    private Integer jaundice = 0;

    @JsonProperty("abdominal_discomfort")
    private Integer abdominalDiscomfort = 0;

    private Integer nausea = 0;

    public Double getComputedBmi() {
        if (bmi != null && bmi > 0) {
            return bmi;
        }
        if (heightCm != null && heightCm > 0 && weightKg != null && weightKg > 0) {
            double hM = heightCm / 100.0;
            return Math.round((weightKg / (hM * hM)) * 100.0) / 100.0;
        }
        return 24.8;
    }

    public Map<String, Object> toFeatureMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("age", age != null ? age.doubleValue() : 45.0);
        map.put("sex", sex != null ? sex : "Female");
        map.put("height_cm", heightCm != null ? heightCm : 168.0);
        map.put("weight_kg", weightKg != null ? weightKg : 70.0);
        map.put("bmi", getComputedBmi());
        map.put("systolic_bp", systolicBp != null ? systolicBp : 120.0);
        map.put("diastolic_bp", diastolicBp != null ? diastolicBp : 80.0);
        map.put("glucose_mg_dl", glucoseMgDl != null ? glucoseMgDl : 95.0);
        map.put("cholesterol_mg_dl", cholesterolMgDl != null ? cholesterolMgDl : 190.0);
        map.put("resting_heart_rate", restingHeartRate != null ? restingHeartRate : 72.0);
        map.put("physical_activity", physicalActivity != null ? physicalActivity : "Moderate");
        map.put("smoking", smoking != null ? smoking.doubleValue() : 0.0);
        map.put("family_history_diabetes", familyHistoryDiabetes != null ? familyHistoryDiabetes.doubleValue() : 0.0);
        map.put("family_history_heart_disease", familyHistoryHeartDisease != null ? familyHistoryHeartDisease.doubleValue() : 0.0);
        map.put("frequent_urination", frequentUrination != null ? frequentUrination.doubleValue() : 0.0);
        map.put("excessive_thirst", excessiveThirst != null ? excessiveThirst.doubleValue() : 0.0);
        map.put("unexplained_weight_loss", unexplainedWeightLoss != null ? unexplainedWeightLoss.doubleValue() : 0.0);
        map.put("fatigue", fatigue != null ? fatigue.doubleValue() : 0.0);
        map.put("blurred_vision", blurredVision != null ? blurredVision.doubleValue() : 0.0);
        map.put("chest_pain", chestPain != null ? chestPain.doubleValue() : 0.0);
        map.put("shortness_of_breath", shortnessOfBreath != null ? shortnessOfBreath.doubleValue() : 0.0);
        map.put("palpitations", palpitations != null ? palpitations.doubleValue() : 0.0);
        map.put("headache", headache != null ? headache.doubleValue() : 0.0);
        map.put("dizziness", dizziness != null ? dizziness.doubleValue() : 0.0);
        map.put("cough", cough != null ? cough.doubleValue() : 0.0);
        map.put("wheezing", wheezing != null ? wheezing.doubleValue() : 0.0);
        map.put("jaundice", jaundice != null ? jaundice.doubleValue() : 0.0);
        map.put("abdominal_discomfort", abdominalDiscomfort != null ? abdominalDiscomfort.doubleValue() : 0.0);
        map.put("nausea", nausea != null ? nausea.doubleValue() : 0.0);
        return map;
    }

    // Getters and Setters
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public String getSex() { return sex; }
    public void setSex(String sex) { this.sex = sex; }
    public Double getHeightCm() { return heightCm; }
    public void setHeightCm(Double heightCm) { this.heightCm = heightCm; }
    public Double getWeightKg() { return weightKg; }
    public void setWeightKg(Double weightKg) { this.weightKg = weightKg; }
    public Double getBmi() { return bmi; }
    public void setBmi(Double bmi) { this.bmi = bmi; }
    public Double getSystolicBp() { return systolicBp; }
    public void setSystolicBp(Double systolicBp) { this.systolicBp = systolicBp; }
    public Double getDiastolicBp() { return diastolicBp; }
    public void setDiastolicBp(Double diastolicBp) { this.diastolicBp = diastolicBp; }
    public Double getGlucoseMgDl() { return glucoseMgDl; }
    public void setGlucoseMgDl(Double glucoseMgDl) { this.glucoseMgDl = glucoseMgDl; }
    public Double getCholesterolMgDl() { return cholesterolMgDl; }
    public void setCholesterolMgDl(Double cholesterolMgDl) { this.cholesterolMgDl = cholesterolMgDl; }
    public Double getRestingHeartRate() { return restingHeartRate; }
    public void setRestingHeartRate(Double restingHeartRate) { this.restingHeartRate = restingHeartRate; }
    public String getPhysicalActivity() { return physicalActivity; }
    public void setPhysicalActivity(String physicalActivity) { this.physicalActivity = physicalActivity; }
    public Integer getSmoking() { return smoking; }
    public void setSmoking(Integer smoking) { this.smoking = smoking; }
    public Integer getFamilyHistoryDiabetes() { return familyHistoryDiabetes; }
    public void setFamilyHistoryDiabetes(Integer familyHistoryDiabetes) { this.familyHistoryDiabetes = familyHistoryDiabetes; }
    public Integer getFamilyHistoryHeartDisease() { return familyHistoryHeartDisease; }
    public void setFamilyHistoryHeartDisease(Integer familyHistoryHeartDisease) { this.familyHistoryHeartDisease = familyHistoryHeartDisease; }
    public Integer getFrequentUrination() { return frequentUrination; }
    public void setFrequentUrination(Integer frequentUrination) { this.frequentUrination = frequentUrination; }
    public Integer getExcessiveThirst() { return excessiveThirst; }
    public void setExcessiveThirst(Integer excessiveThirst) { this.excessiveThirst = excessiveThirst; }
    public Integer getUnexplainedWeightLoss() { return unexplainedWeightLoss; }
    public void setUnexplainedWeightLoss(Integer unexplainedWeightLoss) { this.unexplainedWeightLoss = unexplainedWeightLoss; }
    public Integer getFatigue() { return fatigue; }
    public void setFatigue(Integer fatigue) { this.fatigue = fatigue; }
    public Integer getBlurredVision() { return blurredVision; }
    public void setBlurredVision(Integer blurredVision) { this.blurredVision = blurredVision; }
    public Integer getChestPain() { return chestPain; }
    public void setChestPain(Integer chestPain) { this.chestPain = chestPain; }
    public Integer getShortnessOfBreath() { return shortnessOfBreath; }
    public void setShortnessOfBreath(Integer shortnessOfBreath) { this.shortnessOfBreath = shortnessOfBreath; }
    public Integer getPalpitations() { return palpitations; }
    public void setPalpitations(Integer palpitations) { this.palpitations = palpitations; }
    public Integer getHeadache() { return headache; }
    public void setHeadache(Integer headache) { this.headache = headache; }
    public Integer getDizziness() { return dizziness; }
    public void setDizziness(Integer dizziness) { this.dizziness = dizziness; }
    public Integer getCough() { return cough; }
    public void setCough(Integer cough) { this.cough = cough; }
    public Integer getWheezing() { return wheezing; }
    public void setWheezing(Integer wheezing) { this.wheezing = wheezing; }
    public Integer getJaundice() { return jaundice; }
    public void setJaundice(Integer jaundice) { this.jaundice = jaundice; }
    public Integer getAbdominalDiscomfort() { return abdominalDiscomfort; }
    public void setAbdominalDiscomfort(Integer abdominalDiscomfort) { this.abdominalDiscomfort = abdominalDiscomfort; }
    public Integer getNausea() { return nausea; }
    public void setNausea(Integer nausea) { this.nausea = nausea; }
}
