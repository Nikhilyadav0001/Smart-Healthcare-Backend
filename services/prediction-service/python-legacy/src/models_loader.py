import json
import logging
import warnings
from pathlib import Path
from typing import Dict, Any, Tuple
import joblib
import pandas as pd
from sklearn.exceptions import InconsistentVersionWarning

from .config import MODELS_DIR
from .schemas import PatientFeatures, DiseaseEnum, DiseaseRiskReport
from .rules_engine import analyze_risk_factors_and_recommendations

logger = logging.getLogger("prediction-service.models")

# Suppress sklearn unpickle version warning (models were saved on scikit-learn 1.9.1)
warnings.filterwarnings("ignore", category=InconsistentVersionWarning)

class ModelRegistry:
    _instance = None

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super(ModelRegistry, cls).__new__(cls)
            cls._instance.models = {}
            cls._instance.thresholds = {}
            cls._instance.initialized = False
        return cls._instance

    def load_models(self):
        if self.initialized:
            return

        logger.info(f"Loading ML models from: {MODELS_DIR}")
        thresholds_path = MODELS_DIR / "optimized_thresholds.json"
        if not thresholds_path.exists():
            raise FileNotFoundError(f"Thresholds file not found at {thresholds_path}")

        with open(thresholds_path, "r", encoding="utf-8") as f:
            self.thresholds = json.load(f)

        model_files = {
            DiseaseEnum.DIABETES.value: "diabetes_risk_pipeline.json",
            DiseaseEnum.HEART_DISEASE.value: "heart_disease_risk_pipeline.json",
            DiseaseEnum.HYPERTENSION.value: "hypertension_risk_pipeline.json",
        }

        for disease_key, filename in model_files.items():
            model_path = MODELS_DIR / filename
            if not model_path.exists():
                raise FileNotFoundError(f"Model file not found at {model_path}")
            logger.info(f"Loading pipeline: {filename}...")
            self.models[disease_key] = joblib.load(model_path)

        self.initialized = True
        logger.info("All 3 ML risk models loaded and ready for inference!")

    def predict(self, disease_key: str, features: PatientFeatures) -> DiseaseRiskReport:
        if not self.initialized:
            self.load_models()

        if disease_key not in self.models:
            raise ValueError(f"Unknown disease key: {disease_key}. Available: {list(self.models.keys())}")

        # Convert patient features to single-row DataFrame
        data_dict = features.model_dump()
        # Enforce string for categorical fields
        data_dict["sex"] = str(features.sex.value)
        data_dict["physical_activity"] = str(features.physical_activity.value)
        df = pd.DataFrame([data_dict])

        model = self.models[disease_key]
        threshold = float(self.thresholds.get(disease_key, 0.5))

        # Predict probability for positive class (risk = 1)
        probabilities = model.predict_proba(df)[0]
        p_risk = round(float(probabilities[1]), 4)
        is_at_risk = p_risk >= threshold

        risk_level, factors, recommendations = analyze_risk_factors_and_recommendations(
            disease=disease_key,
            p_risk=p_risk,
            threshold=threshold,
            features=features
        )

        return DiseaseRiskReport(
            disease=disease_key,
            probability=p_risk,
            threshold=round(threshold, 4),
            is_at_risk=is_at_risk,
            risk_level=risk_level,
            top_factors=factors,
            recommendations=recommendations
        )

    def get_metadata(self) -> Dict[str, Any]:
        return {
            "loaded_models": list(self.models.keys()),
            "optimized_thresholds": self.thresholds,
            "models_directory": str(MODELS_DIR),
            "status": "ready" if self.initialized else "uninitialized"
        }

registry = ModelRegistry()
