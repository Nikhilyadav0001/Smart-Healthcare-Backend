import uuid
from typing import List, Dict
from datetime import datetime
from fastapi import APIRouter, HTTPException, Query

from ...schemas import (
    SinglePredictionRequest,
    MultiPredictionRequest,
    DiseaseRiskReport,
    PredictionResponse,
    HistoricalPredictionItem,
    DiseaseEnum
)
from ...models_loader import registry
from ...database import persist_prediction, fetch_patient_history

router = APIRouter(prefix="/predictions", tags=["ML Clinical Predictions"])

@router.post("/predict", response_model=DiseaseRiskReport, summary="Evaluate risk for a single disease")
def predict_single(request: SinglePredictionRequest):
    """
    Evaluates clinical risk for one specified disease:
    - `diabetes_risk`: Type 2 Diabetes Mellitus
    - `heart_disease_risk`: Coronary Artery Disease / Cardiovascular Risk
    - `hypertension_risk`: Primary / Secondary Hypertension
    """
    try:
        report = registry.predict(request.disease.value, request.features)

        # Persist prediction
        pred_id = uuid.uuid4()
        persist_prediction(
            prediction_id=pred_id,
            patient_id=request.patient_id or uuid.uuid4(),
            patient_name=request.patient_name,
            patient_email=request.patient_email,
            disease=report.disease,
            probability=report.probability,
            threshold=report.threshold,
            is_at_risk=report.is_at_risk,
            risk_level=report.risk_level.value,
            top_factors=report.top_factors,
            recommendations=report.recommendations,
            features=request.features.model_dump()
        )

        return report
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Inference error: {str(e)}")

@router.post("/predict/all", response_model=PredictionResponse, summary="Comprehensive assessment across all ML models")
def predict_all(request: MultiPredictionRequest):
    """
    Runs patient data through all three ML models simultaneously (Diabetes, Heart Disease, Hypertension),
    computes an overall cardiovascular-metabolic health score (0-100), and records the assessment.
    """
    try:
        prediction_id = uuid.uuid4()
        patient_id = request.patient_id or uuid.uuid4()

        results: Dict[str, DiseaseRiskReport] = {}
        diseases = [
            DiseaseEnum.DIABETES.value,
            DiseaseEnum.HEART_DISEASE.value,
            DiseaseEnum.HYPERTENSION.value
        ]

        total_penalty = 0.0

        for dis in diseases:
            report = registry.predict(dis, request.features)
            results[dis] = report

            # Calculate health score penalties based on disease probabilities
            if report.is_at_risk:
                total_penalty += 25.0 + (report.probability * 10.0)
            elif report.risk_level.value == "MEDIUM":
                total_penalty += 10.0 + (report.probability * 5.0)

            # Persist to prediction_db
            persist_prediction(
                prediction_id=uuid.uuid4(),
                patient_id=patient_id,
                patient_name=request.patient_name,
                patient_email=request.patient_email,
                disease=report.disease,
                probability=report.probability,
                threshold=report.threshold,
                is_at_risk=report.is_at_risk,
                risk_level=report.risk_level.value,
                top_factors=report.top_factors,
                recommendations=report.recommendations,
                features=request.features.model_dump()
            )

        overall_score = max(5.0, round(100.0 - total_penalty, 1))

        # Generate clinical summary
        at_risk_list = [r.disease.replace("_risk", "").replace("_", " ").title() for r in results.values() if r.is_at_risk]
        if at_risk_list:
            summary = f"Elevated clinical risk detected for: {', '.join(at_risk_list)}. Recommended proactive intervention."
        else:
            summary = "All evaluated parameters within optimal/acceptable predictive margins. Continue preventive lifestyle management."

        return PredictionResponse(
            prediction_id=prediction_id,
            patient_id=patient_id,
            patient_name=request.patient_name,
            patient_email=request.patient_email,
            evaluated_at=datetime.now(),
            predictions=results,
            overall_health_score=overall_score,
            summary=summary
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Multi-model inference error: {str(e)}")

@router.get("/history/{patient_id}", response_model=List[HistoricalPredictionItem], summary="Get past predictions for patient")
def get_history(patient_id: uuid.UUID, limit: int = Query(default=20, ge=1, le=100)):
    """
    Returns the historical audit trail of ML predictions for a patient.
    """
    return fetch_patient_history(patient_id=patient_id, limit=limit)

@router.get("/models", summary="Inspect ML models metadata and optimized thresholds")
def get_models_info():
    """
    Returns live information on loaded scikit-learn models and tuned thresholds.
    """
    return registry.get_metadata()
