import json
import logging
import uuid
from typing import List, Optional, Dict, Any
from datetime import datetime
import psycopg
from psycopg.rows import dict_row

from .config import DB_URL
from .schemas import HistoricalPredictionItem

logger = logging.getLogger("prediction-service.database")

def get_connection():
    return psycopg.connect(DB_URL, row_factory=dict_row)

def init_db():
    try:
        with get_connection() as conn:
            with conn.cursor() as cur:
                cur.execute("""
                    CREATE TABLE IF NOT EXISTS ml_predictions (
                        id UUID PRIMARY KEY,
                        patient_id UUID NOT NULL,
                        patient_name VARCHAR(255),
                        patient_email VARCHAR(255),
                        disease VARCHAR(50) NOT NULL,
                        probability DOUBLE PRECISION NOT NULL,
                        threshold DOUBLE PRECISION NOT NULL,
                        is_at_risk BOOLEAN NOT NULL,
                        risk_level VARCHAR(20) NOT NULL,
                        top_factors JSONB,
                        recommendations JSONB,
                        features JSONB,
                        created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
                    );
                    CREATE INDEX IF NOT EXISTS idx_ml_predictions_patient ON ml_predictions(patient_id);
                    CREATE INDEX IF NOT EXISTS idx_ml_predictions_created_at ON ml_predictions(created_at);
                """)
                conn.commit()
        logger.info("Database schema verified for prediction_db.")
    except Exception as e:
        logger.warning(f"Could not initialize prediction_db (will operate in stateless fallback if DB unavailable): {e}")

def persist_prediction(
    prediction_id: uuid.UUID,
    patient_id: uuid.UUID,
    patient_name: Optional[str],
    patient_email: Optional[str],
    disease: str,
    probability: float,
    threshold: float,
    is_at_risk: bool,
    risk_level: str,
    top_factors: List[str],
    recommendations: List[str],
    features: Dict[str, Any]
):
    try:
        with get_connection() as conn:
            with conn.cursor() as cur:
                cur.execute("""
                    INSERT INTO ml_predictions (
                        id, patient_id, patient_name, patient_email, disease,
                        probability, threshold, is_at_risk, risk_level,
                        top_factors, recommendations, features, created_at
                    ) VALUES (
                        %s, %s, %s, %s, %s,
                        %s, %s, %s, %s,
                        %s, %s, %s, %s
                    )
                """, (
                    str(prediction_id),
                    str(patient_id),
                    patient_name,
                    patient_email,
                    disease,
                    probability,
                    threshold,
                    is_at_risk,
                    risk_level,
                    json.dumps(top_factors),
                    json.dumps(recommendations),
                    json.dumps(features),
                    datetime.now()
                ))
                conn.commit()
    except Exception as e:
        logger.error(f"Failed to persist prediction record to database: {e}")

def fetch_patient_history(patient_id: uuid.UUID, limit: int = 50) -> List[HistoricalPredictionItem]:
    try:
        with get_connection() as conn:
            with conn.cursor() as cur:
                cur.execute("""
                    SELECT id, patient_id, patient_name, disease, probability,
                           threshold, is_at_risk, risk_level, top_factors,
                           recommendations, features, created_at
                    FROM ml_predictions
                    WHERE patient_id = %s
                    ORDER BY created_at DESC
                    LIMIT %s
                """, (str(patient_id), limit))
                rows = cur.fetchall()

                items = []
                for row in rows:
                    items.append(HistoricalPredictionItem(
                        id=row["id"],
                        patient_id=row["patient_id"],
                        patient_name=row["patient_name"],
                        disease=row["disease"],
                        probability=row["probability"],
                        threshold=row["threshold"],
                        is_at_risk=row["is_at_risk"],
                        risk_level=row["risk_level"],
                        top_factors=row["top_factors"] if isinstance(row["top_factors"], list) else json.loads(row["top_factors"]),
                        recommendations=row["recommendations"] if isinstance(row["recommendations"], list) else json.loads(row["recommendations"]),
                        features=row["features"] if isinstance(row["features"], dict) else json.loads(row["features"]),
                        created_at=row["created_at"]
                    ))
                return items
    except Exception as e:
        logger.error(f"Failed to query patient prediction history: {e}")
        return []
