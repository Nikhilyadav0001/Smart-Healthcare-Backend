# Smart Healthcare - ML Prediction Service

A clinical risk assessment microservice powered by FastAPI and pre-trained scikit-learn pipelines.

## Features
- **3 Pre-Trained ML Pipelines**:
  - `diabetes_risk`: Type 2 Diabetes Mellitus risk evaluation (Logistic Regression)
  - `heart_disease_risk`: Coronary Artery Disease risk evaluation (Random Forest)
  - `hypertension_risk`: Hypertension risk evaluation (Logistic Regression)
- **Calibrated Clinical Thresholds**: Sourced from `optimized_thresholds.json`.
- **Explainability & Action Plans**: Computes prominent risk factors and evidence-based clinical recommendations.
- **Audit Persistence**: Automatically records predictions in PostgreSQL database `prediction_db` (`ml_predictions` table).
- **Interactive Documentation**: Swagger UI at `/docs` and ReDoc at `/redoc`.

## Running Locally

```bash
cd smart-healthcare/services/prediction-service
python run.py
```

Runs on port `8085`:
- API: `http://localhost:8085`
- Swagger UI: `http://localhost:8085/docs`
- Healthcheck: `http://localhost:8085/health`
