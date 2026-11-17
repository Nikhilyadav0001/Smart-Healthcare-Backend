from typing import List, Tuple
from .schemas import PatientFeatures, RiskLevelEnum

def analyze_risk_factors_and_recommendations(disease: str, p_risk: float, threshold: float, features: PatientFeatures) -> Tuple[RiskLevelEnum, List[str], List[str]]:
    """
    Analyzes patient features according to clinical guidelines and generates
    top contributing risk factors and tailored clinical recommendations.
    """
    factors: List[str] = []
    recommendations: List[str] = []

    # Risk level classification based on probability vs optimal threshold
    if p_risk >= threshold:
        risk_level = RiskLevelEnum.HIGH
    elif p_risk >= threshold * 0.65:
        risk_level = RiskLevelEnum.MEDIUM
    else:
        risk_level = RiskLevelEnum.LOW

    # 1. Diabetes specific evaluation
    if "diabetes" in disease.lower():
        if features.glucose_mg_dl >= 126:
            factors.append(f"Fasting Blood Glucose ({features.glucose_mg_dl} mg/dL) exceeds diabetes diagnostic threshold (>= 126 mg/dL)")
        elif features.glucose_mg_dl >= 100:
            factors.append(f"Impaired Fasting Glucose ({features.glucose_mg_dl} mg/dL) indicates pre-diabetes range (100-125 mg/dL)")

        if features.bmi and features.bmi >= 30:
            factors.append(f"Elevated BMI ({features.bmi} kg/m^2) indicates obesity class, a major insulin resistance factor")
        elif features.bmi and features.bmi >= 25:
            factors.append(f"Overweight BMI ({features.bmi} kg/m^2)")

        if features.family_history_diabetes == 1:
            factors.append("First-degree family history of type 2 diabetes mellitus")

        if features.frequent_urination or features.excessive_thirst:
            factors.append("Classical osmotic symptoms present (polyuria / polydipsia)")

        if features.unexplained_weight_loss or features.blurred_vision:
            factors.append("Metabolic decompensation signs (weight loss, visual disturbance)")

        # Recommendations
        if risk_level == RiskLevelEnum.HIGH:
            recommendations.append("Order confirmation HbA1c and Oral Glucose Tolerance Test (OGTT).")
            recommendations.append("Referral to an Endocrinologist / Certified Diabetes Care and Education Specialist.")
            recommendations.append("Initiate medical nutrition therapy: Low glycemic index, Mediterranean dietary pattern.")
        elif risk_level == RiskLevelEnum.MEDIUM:
            recommendations.append("Perform annual fasting plasma glucose or HbA1c screening.")
            recommendations.append("Target 150 minutes/week of moderate-intensity aerobic exercise.")
            recommendations.append("Target a 5-7% reduction in body weight if BMI >= 25.")
        else:
            recommendations.append("Maintain balanced nutrition with high fiber and low refined carbohydrates.")
            recommendations.append("Routine preventive wellness check within 12 months.")

    # 2. Heart disease specific evaluation
    elif "heart" in disease.lower():
        if features.chest_pain:
            factors.append("Active clinical symptom: Chest discomfort / angina reported")
        if features.shortness_of_breath or features.palpitations:
            factors.append("Cardiorespiratory symptoms (dyspnea, palpitations) on record")
        if features.cholesterol_mg_dl >= 240:
            factors.append(f"High Total Cholesterol ({features.cholesterol_mg_dl} mg/dL) (>= 240 mg/dL)")
        elif features.cholesterol_mg_dl >= 200:
            factors.append(f"Borderline elevated Total Cholesterol ({features.cholesterol_mg_dl} mg/dL)")
        if features.smoking == 1:
            factors.append("Current tobacco use doubles coronary artery disease risk")
        if features.family_history_heart_disease == 1:
            factors.append("Premature coronary heart disease in primary biological relatives")
        if features.systolic_bp >= 140 or features.diastolic_bp >= 90:
            factors.append(f"Concurrent hypertension ({features.systolic_bp:.0f}/{features.diastolic_bp:.0f} mmHg) exacerbating vascular shear stress")

        # Recommendations
        if risk_level == RiskLevelEnum.HIGH:
            recommendations.append("Immediate 12-lead ECG, high-sensitivity cardiac troponin, and cardiology evaluation.")
            recommendations.append("Complete lipid subfraction panel (LDL-C, HDL-C, Triglycerides, ApoB).")
            recommendations.append("Echocardiogram and treadmill stress test if symptomatically indicated.")
        elif risk_level == RiskLevelEnum.MEDIUM:
            recommendations.append("Atherosclerotic Cardiovascular Disease (ASCVD) 10-year risk estimation.")
            recommendations.append("Smoking cessation counseling and nicotine replacement if applicable.")
            recommendations.append("Omega-3 fatty acids, dietary saturated fat reduction (< 7% of total calories).")
        else:
            recommendations.append("Maintain cardiovascular fitness: 30 minutes daily brisk walking or cycling.")
            recommendations.append("Monitor blood pressure and lipid levels every 2 years.")

    # 3. Hypertension specific evaluation
    elif "hypertension" in disease.lower():
        if features.systolic_bp >= 140 or features.diastolic_bp >= 90:
            factors.append(f"Blood pressure ({features.systolic_bp:.0f}/{features.diastolic_bp:.0f} mmHg) meets Stage 2 Hypertension criteria")
        elif features.systolic_bp >= 130 or features.diastolic_bp >= 80:
            factors.append(f"Blood pressure ({features.systolic_bp:.0f}/{features.diastolic_bp:.0f} mmHg) meets Stage 1 Hypertension criteria")

        if features.resting_heart_rate >= 85:
            factors.append(f"Elevated resting heart rate ({features.resting_heart_rate:.0f} bpm) suggests sympathetic tone elevation")
        if features.headache or features.dizziness:
            factors.append("Neurological symptom report: Frequent headaches or dizziness")
        if features.physical_activity.value == "Low":
            factors.append("Sedentary lifestyle / minimal weekly physical exertion")

        # Recommendations
        if risk_level == RiskLevelEnum.HIGH:
            recommendations.append("Ambulatory Blood Pressure Monitoring (ABPM) or twice-daily home BP diary for 14 days.")
            recommendations.append("Initiate DASH (Dietary Approaches to Stop Hypertension) diet: Sodium < 1500 mg/day.")
            recommendations.append("Physician consultation for first-line pharmacotherapy (ACEi/ARB/CCB) evaluation.")
        elif risk_level == RiskLevelEnum.MEDIUM:
            recommendations.append("Lifestyle modification: Stress reduction techniques (mindfulness, paced respiration).")
            recommendations.append("Potassium-rich foods (bananas, leafy greens) unless contraindicated by renal function.")
            recommendations.append("Recheck blood pressure in 4 to 8 weeks.")
        else:
            recommendations.append("Continue maintaining optimal blood pressure (< 120/80 mmHg).")
            recommendations.append("Keep dietary sodium under 2300 mg daily.")

    # Fallback factors if patient is exceptionally healthy
    if not factors:
        factors.append("Vital signs, metabolic labs, and symptom scores are within normal reference ranges.")

    return risk_level, factors, recommendations
