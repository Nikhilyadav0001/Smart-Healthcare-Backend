package com.smarthealth.prediction.controller;

import com.smarthealth.prediction.dto.MultiPredictionRequest;
import com.smarthealth.prediction.dto.PredictionResponse;
import com.smarthealth.prediction.dto.SinglePredictionRequest;
import com.smarthealth.prediction.model.MlPredictionEntity;
import com.smarthealth.prediction.service.PredictionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@Tag(name = "ML Predictions", description = "Cardiometabolic Risk Inference & Clinical Recommendations")
@CrossOrigin(origins = "*")
public class PredictionController {

    private final PredictionService predictionService;

    public PredictionController(PredictionService predictionService) {
        this.predictionService = predictionService;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "prediction-service",
                "language", "Java",
                "models_loaded", predictionService.getAvailableModels().size()
        ));
    }

    @GetMapping("/api/v1/predictions/models")
    @Operation(summary = "Get list of available ML models and optimized clinical decision thresholds")
    public ResponseEntity<Map<String, Object>> getModels() {
        return ResponseEntity.ok(Map.of(
                "loaded_models", predictionService.getAvailableModels(),
                "optimized_thresholds", predictionService.getThresholds(),
                "runtime", "Java 25 Spring Boot with OpenTelemetry & Prometheus",
                "status", "ready"
        ));
    }

    @PostMapping("/api/v1/predictions/predict/all")
    @Operation(summary = "Evaluate multi-disease risk (Diabetes, Heart Disease, Hypertension) and generate overall score")
    public ResponseEntity<PredictionResponse> predictAll(@RequestBody MultiPredictionRequest request) {
        return ResponseEntity.ok(predictionService.predictAll(request));
    }

    @PostMapping("/api/v1/predictions/predict")
    @Operation(summary = "Evaluate risk for a single specific disease model")
    public ResponseEntity<PredictionResponse> predictSingle(@RequestBody SinglePredictionRequest request) {
        return ResponseEntity.ok(predictionService.predictSingle(request));
    }

    @GetMapping("/api/v1/predictions/{id}")
    @Operation(summary = "Retrieve a specific prediction assessment by its ID")
    public ResponseEntity<MlPredictionEntity> getPrediction(@PathVariable UUID id) {
        return predictionService.getPrediction(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/api/v1/predictions/{id}/report")
    @Operation(summary = "Download official clinical PDF report generated via go-service gRPC")
    public ResponseEntity<byte[]> downloadReport(@PathVariable UUID id) {
        byte[] pdfBytes = predictionService.generateReportPdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Clinical_Report_" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @GetMapping("/api/v1/predictions/history/{patientId}")
    @Operation(summary = "Retrieve historical predictions and recommendations for a patient")
    public ResponseEntity<List<MlPredictionEntity>> getHistory(@PathVariable UUID patientId) {
        return ResponseEntity.ok(predictionService.getHistory(patientId));
    }
}
