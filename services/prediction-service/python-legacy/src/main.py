import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from .config import SERVICE_NAME
from .models_loader import registry
from .database import init_db
from .api.v1.predictions import router as predictions_router

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] [%(name)s]: %(message)s"
)
logger = logging.getLogger(SERVICE_NAME)

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup: Load ML models & connect DB
    logger.info("Initializing ML Prediction Service...")
    init_db()
    registry.load_models()
    logger.info("Prediction Service ready to serve inferences!")
    yield
    # Shutdown
    logger.info("Shutting down Prediction Service...")

app = FastAPI(
    title="Smart Healthcare Prediction Service",
    description="Machine Learning Risk Prediction microservice utilizing pre-trained scikit-learn pipelines for Diabetes, Heart Disease, and Hypertension.",
    version="1.0.0",
    lifespan=lifespan,
    docs_url="/docs",
    redoc_url="/redoc",
    openapi_url="/v3/api-docs"
)

# CORS configuration
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:5173", "http://localhost:8080", "*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include API routes
app.include_router(predictions_router, prefix="/api/v1")

@app.get("/health", tags=["Health"])
@app.get("/actuator/health", tags=["Health"])
def health_check():
    return {
        "status": "UP",
        "service": SERVICE_NAME,
        "models_loaded": len(registry.models)
    }

@app.get("/", tags=["Root"])
def root():
    return {
        "service": "Smart Healthcare ML Prediction Service",
        "version": "1.0.0",
        "docs": "/docs",
        "health": "/health"
    }
