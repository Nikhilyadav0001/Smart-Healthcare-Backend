import os
from pathlib import Path

# Paths
BASE_DIR = Path(__file__).resolve().parent.parent
# ML models are located in smart-healthcare/ml/models
PROJECT_ROOT = BASE_DIR.parent.parent
DEFAULT_MODELS_DIR = PROJECT_ROOT / "ml" / "models"

MODELS_DIR = Path(os.getenv("MODELS_DIR", str(DEFAULT_MODELS_DIR)))

# Service Configuration
SERVICE_NAME = "prediction-service"
PORT = int(os.getenv("PORT", 8085))
HOST = os.getenv("HOST", "0.0.0.0")

# Database Configuration (PostgreSQL: prediction_db)
DB_HOST = os.getenv("PREDICTION_DB_HOST", "localhost")
DB_PORT = int(os.getenv("PREDICTION_DB_PORT", 5433))
DB_NAME = os.getenv("PREDICTION_DB_NAME", "prediction_db")
DB_USER = os.getenv("PREDICTION_DB_USER", "prediction_user")
DB_PASSWORD = os.getenv("PREDICTION_DB_PASSWORD", "nikhil")

# Connection string
DB_URL = f"host={DB_HOST} port={DB_PORT} dbname={DB_NAME} user={DB_USER} password={DB_PASSWORD}"
