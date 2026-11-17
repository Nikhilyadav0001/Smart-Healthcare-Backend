from enum import Enum
from typing import List, Optional, Dict, Any
from pydantic import BaseModel, Field, model_validator
from datetime import datetime
import uuid

class SexEnum(str, Enum):
    FEMALE = "Female"
    MALE = "Male"
    OTHER = "Other"

class PhysicalActivityEnum(str, Enum):
    HIGH = "High"
    MODERATE = "Moderate"
    LOW = "Low"

class DiseaseEnum(str, Enum):
    DIABETES = "diabetes_risk"
    HEART_DISEASE = "heart_disease_risk"
    HYPERTENSION = "hypertension_risk"

class RiskLevelEnum(str, Enum):
    LOW = "LOW"
    MEDIUM = "MEDIUM"
    HIGH = "HIGH"

class PatientFeatures(BaseModel):
    # Demographics & Anthropometrics
    age: float = Field(..., ge=1, le=120, description="Patient age in years")
    sex: SexEnum = Field(default=SexEnum.MALE, description="Biological sex (Female, Male, Other)")
    height_cm: float = Field(default=170.0, ge=50, le=250, description="Height in centimeters")
    weight_kg: float = Field(default=70.0, ge=20, le=300, description="Weight in kilograms")
    bmi: Optional[float] = Field(default=None, description="Body Mass Index (kg/m^2). Auto-calculated if omitted.")

    # Vitals & Labs
    systolic_bp: float = Field(default=120.0, ge=60, le=260, description="Systolic Blood Pressure (mmHg)")
    diastolic_bp: float = Field(default=80.0, ge=40, le=160, description="Diastolic Blood Pressure (mmHg)")
    glucose_mg_dl: float = Field(default=100.0, ge=40, le=500, description="Fasting Blood Glucose (mg/dL)")
    cholesterol_mg_dl: float = Field(default=190.0, ge=80, le=600, description="Total Serum Cholesterol (mg/dL)")
    resting_heart_rate: float = Field(default=72.0, ge=30, le=220, description="Resting Heart Rate (bpm)")
    physical_activity: PhysicalActivityEnum = Field(default=PhysicalActivityEnum.MODERATE, description="Physical activity level")
    smoking: int = Field(default=0, ge=0, le=1, description="Current or significant smoker (0: No, 1: Yes)")

    # Family History
    family_history_diabetes: int = Field(default=0, ge=0, le=1, description="Family history of diabetes (0: No, 1: Yes)")
    family_history_heart_disease: int = Field(default=0, ge=0, le=1, description="Family history of heart disease (0: No, 1: Yes)")

    # Clinical Symptoms (0: absent, 1: present)
    frequent_urination: int = Field(default=0, ge=0, le=1)
    excessive_thirst: int = Field(default=0, ge=0, le=1)
    unexplained_weight_loss: int = Field(default=0, ge=0, le=1)
    fatigue: int = Field(default=0, ge=0, le=1)
    blurred_vision: int = Field(default=0, ge=0, le=1)
    chest_pain: int = Field(default=0, ge=0, le=1)
    shortness_of_breath: int = Field(default=0, ge=0, le=1)
    palpitations: int = Field(default=0, ge=0, le=1)
    headache: int = Field(default=0, ge=0, le=1)
    dizziness: int = Field(default=0, ge=0, le=1)
    cough: int = Field(default=0, ge=0, le=1)
    wheezing: int = Field(default=0, ge=0, le=1)
    jaundice: int = Field(default=0, ge=0, le=1)
    abdominal_discomfort: int = Field(default=0, ge=0, le=1)
    nausea: int = Field(default=0, ge=0, le=1)

    @model_validator(mode="after")
    def compute_bmi_if_missing(self) -> "PatientFeatures":
        if self.bmi is None and self.height_cm > 0:
            h_m = self.height_cm / 100.0
            self.bmi = round(self.weight_kg / (h_m * h_m), 2)
        return self

class SinglePredictionRequest(BaseModel):
    patient_id: Optional[uuid.UUID] = Field(default_factory=uuid.uuid4)
    patient_name: Optional[str] = "Anonymous Patient"
    patient_email: Optional[str] = None
    disease: DiseaseEnum
    features: PatientFeatures

class MultiPredictionRequest(BaseModel):
    patient_id: Optional[uuid.UUID] = Field(default_factory=uuid.uuid4)
    patient_name: Optional[str] = "Anonymous Patient"
    patient_email: Optional[str] = None
    features: PatientFeatures

class DiseaseRiskReport(BaseModel):
    disease: str
    probability: float
    threshold: float
    is_at_risk: bool
    risk_level: RiskLevelEnum
    top_factors: List[str]
    recommendations: List[str]

class PredictionResponse(BaseModel):
    prediction_id: uuid.UUID
    patient_id: uuid.UUID
    patient_name: Optional[str]
    patient_email: Optional[str]
    evaluated_at: datetime
    predictions: Dict[str, DiseaseRiskReport]
    overall_health_score: float = Field(description="Score from 0 (critical) to 100 (optimal)")
    summary: str

class HistoricalPredictionItem(BaseModel):
    id: uuid.UUID
    patient_id: uuid.UUID
    patient_name: Optional[str]
    disease: str
    probability: float
    threshold: float
    is_at_risk: bool
    risk_level: str
    top_factors: List[str]
    recommendations: List[str]
    features: Dict[str, Any]
    created_at: datetime
