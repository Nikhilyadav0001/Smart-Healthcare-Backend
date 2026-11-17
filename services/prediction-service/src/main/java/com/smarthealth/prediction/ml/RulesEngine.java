package com.smarthealth.prediction.ml;

import com.smarthealth.prediction.dto.PatientFeaturesDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RulesEngine {

    public List<String> extractTopFactors(String disease, PatientFeaturesDto features) {
        List<String> factors = new ArrayList<>();
        double bmi = features.getComputedBmi();

        if ("diabetes_risk".equalsIgnoreCase(disease)) {
            if (features.getGlucoseMgDl() != null && features.getGlucoseMgDl() >= 126.0) {
                factors.add(String.format("Fasting plasma glucose severely elevated (%.1f mg/dL >= 126 mg/dL)", features.getGlucoseMgDl()));
            } else if (features.getGlucoseMgDl() != null && features.getGlucoseMgDl() >= 100.0) {
                factors.add(String.format("Impaired fasting glucose in pre-diabetic margin (%.1f mg/dL)", features.getGlucoseMgDl()));
            }
            if (bmi >= 30.0) {
                factors.add(String.format("Clinical obesity (BMI: %.1f kg/m² >= 30.0)", bmi));
            } else if (bmi >= 25.0) {
                factors.add(String.format("Overweight index (BMI: %.1f kg/m²)", bmi));
            }
            if (features.getFamilyHistoryDiabetes() != null && features.getFamilyHistoryDiabetes() == 1) {
                factors.add("Positive first-degree family history of Type 2 Diabetes");
            }
            if (features.getAge() != null && features.getAge() >= 45) {
                factors.add(String.format("Advanced metabolic risk age window (%d years)", features.getAge()));
            }
            if (features.getFrequentUrination() != null && features.getFrequentUrination() == 1) {
                factors.add("Presenting symptom: Frequent urination (polyuria)");
            }
            if (features.getExcessiveThirst() != null && features.getExcessiveThirst() == 1) {
                factors.add("Presenting symptom: Excessive thirst (polydipsia)");
            }
        } else if ("hypertension_risk".equalsIgnoreCase(disease)) {
            if (features.getSystolicBp() != null && features.getSystolicBp() >= 140.0) {
                factors.add(String.format("Stage 2 systolic hypertension (%.0f mmHg >= 140 mmHg)", features.getSystolicBp()));
            } else if (features.getSystolicBp() != null && features.getSystolicBp() >= 130.0) {
                factors.add(String.format("Stage 1 elevated systolic pressure (%.0f mmHg)", features.getSystolicBp()));
            }
            if (features.getDiastolicBp() != null && features.getDiastolicBp() >= 90.0) {
                factors.add(String.format("Stage 2 diastolic hypertension (%.0f mmHg >= 90 mmHg)", features.getDiastolicBp()));
            }
            if (bmi >= 28.0) {
                factors.add(String.format("Elevated body mass index contributing to peripheral vascular resistance (BMI: %.1f)", bmi));
            }
            if ("Low".equalsIgnoreCase(features.getPhysicalActivity())) {
                factors.add("Sedentary lifestyle elevating resting sympathetic tone");
            }
            if (features.getHeadache() != null && features.getHeadache() == 1) {
                factors.add("Presenting symptom: Recurrent hypertensive headache");
            }
        } else if ("heart_disease_risk".equalsIgnoreCase(disease)) {
            if (features.getCholesterolMgDl() != null && features.getCholesterolMgDl() >= 240.0) {
                factors.add(String.format("Hypercholesterolemia (Total Cholesterol: %.1f mg/dL >= 240)", features.getCholesterolMgDl()));
            } else if (features.getCholesterolMgDl() != null && features.getCholesterolMgDl() >= 200.0) {
                factors.add(String.format("Borderline high serum cholesterol (%.1f mg/dL)", features.getCholesterolMgDl()));
            }
            if (features.getSmoking() != null && features.getSmoking() == 1) {
                factors.add("Active tobacco smoking causing endothelial oxidative stress");
            }
            if (features.getFamilyHistoryHeartDisease() != null && features.getFamilyHistoryHeartDisease() == 1) {
                factors.add("Premature cardiovascular disease in first-degree relatives");
            }
            if (features.getChestPain() != null && features.getChestPain() == 1) {
                factors.add("Warning symptom: Angina / chest pain reported");
            }
            if (features.getShortnessOfBreath() != null && features.getShortnessOfBreath() == 1) {
                factors.add("Warning symptom: Dyspnea / shortness of breath on exertion");
            }
        }

        if (factors.isEmpty()) {
            factors.add("Biomarkers and clinical indices within standard physiological baseline margins.");
        }
        return factors;
    }

    public List<String> generateRecommendations(String disease, String riskLevel, PatientFeaturesDto features) {
        List<String> recs = new ArrayList<>();

        if ("diabetes_risk".equalsIgnoreCase(disease)) {
            if ("HIGH".equals(riskLevel) || "CRITICAL".equals(riskLevel)) {
                recs.add("Order confirmatory diagnostic HbA1c test and 2-hour Oral Glucose Tolerance Test (OGTT).");
                recs.add("Schedule endocrine consultation for individualized medical nutrition therapy.");
                recs.add("Begin continuous glucose monitoring (CGM) or pre/post-prandial blood sugar tracking.");
            } else if ("MEDIUM".equals(riskLevel)) {
                recs.add("Repeat annual fasting plasma glucose monitoring.");
                recs.add("Adopt low-glycemic dietary protocols with increased dietary soluble fiber.");
                recs.add("Target 5-7% total body weight reduction via structured moderate caloric restriction.");
            } else {
                recs.add("Maintain balanced low-refined-sugar diet and repeat routine screening at annual check-up.");
            }
        } else if ("hypertension_risk".equalsIgnoreCase(disease)) {
            if ("HIGH".equals(riskLevel) || "CRITICAL".equals(riskLevel)) {
                recs.add("Perform 24-hour ambulatory blood pressure monitoring (ABPM) to rule out white-coat artifact.");
                recs.add("Evaluate baseline serum creatinine, electrolytes, and urinalysis for microalbuminuria.");
                recs.add("Initiate strict dietary sodium restriction (< 1,500 mg/day) and DASH dietary protocol.");
            } else if ("MEDIUM".equals(riskLevel)) {
                recs.add("Log home blood pressure twice daily for 14 days and share logs with primary care physician.");
                recs.add("Target 150 minutes weekly of moderate aerobic exercise (brisk walking, swimming, cycling).");
            } else {
                recs.add("Maintain normative sodium intake and schedule routine annual blood pressure check.");
            }
        } else if ("heart_disease_risk".equalsIgnoreCase(disease)) {
            if ("HIGH".equals(riskLevel) || "CRITICAL".equals(riskLevel)) {
                recs.add("Order resting 12-lead ECG and consider coronary artery calcium (CAC) scoring or stress imaging.");
                recs.add("Complete full fasting lipid panel (LDL-C, HDL-C, Triglycerides, ApoB).");
                recs.add("Immediate smoking cessation therapy with clinical counseling support if applicable.");
            } else if ("MEDIUM".equals(riskLevel)) {
                recs.add("Schedule comprehensive ASCVD risk reassessment within 6 months.");
                recs.add("Prioritize Mediterranean dietary pattern rich in omega-3 fatty acids and polyphenols.");
            } else {
                recs.add("Continue regular cardiovascular wellness exercise routine and annual preventive screening.");
            }
        }

        return recs;
    }

    public String computeRiskLevel(double probability, double threshold) {
        if (probability >= Math.max(0.85, threshold * 1.25)) {
            return "CRITICAL";
        } else if (probability >= threshold) {
            return "HIGH";
        } else if (probability >= threshold * 0.65) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }
}
