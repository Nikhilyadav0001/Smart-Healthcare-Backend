package com.smarthealth.healthdata.controller;

import com.smarthealth.healthdata.dto.CreateRecordRequest;
import com.smarthealth.healthdata.dto.HealthRecordResponse;
import com.smarthealth.healthdata.service.HealthDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health Data Records", description = "Patient Longitudinal Vitals, Lab Tests & Clinical Metrics")
@CrossOrigin(origins = "*")
public class HealthDataController {

    private final HealthDataService service;

    public HealthDataController(HealthDataService service) {
        this.service = service;
    }

    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "health-data-service",
                "datastore", "MongoDB rs0",
                "grpc_port", 9083
        ));
    }

    @PostMapping("/records")
    @Operation(summary = "Record new patient clinical vitals and test results")
    public ResponseEntity<HealthRecordResponse> createRecord(
            @RequestHeader(value = "X-Dev-User-Id", required = false) String headerUserId,
            @Valid @RequestBody CreateRecordRequest req) {

        UUID patientId = req.getPatientId();
        if (patientId == null && headerUserId != null && !headerUserId.isBlank()) {
            patientId = UUID.fromString(headerUserId);
        }
        if (patientId == null) {
            patientId = UUID.randomUUID();
        }

        HealthRecordResponse created = service.createRecord(patientId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/records/latest")
    @Operation(summary = "Get the most recent health record for the authenticated patient")
    public ResponseEntity<HealthRecordResponse> getLatestRecord(
            @RequestHeader(value = "X-Dev-User-Id", required = false) String headerUserId,
            @RequestParam(value = "patientId", required = false) UUID paramPatientId) {

        UUID patientId = paramPatientId;
        if (patientId == null && headerUserId != null && !headerUserId.isBlank()) {
            patientId = UUID.fromString(headerUserId);
        }

        if (patientId == null) {
            return ResponseEntity.badRequest().build();
        }

        return service.getLatestRecord(patientId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/records/{id}")
    @Operation(summary = "Get a specific clinical health record by its unique identifier")
    public ResponseEntity<HealthRecordResponse> getRecordById(@PathVariable String id) {
        return service.getRecordById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/records")
    @Operation(summary = "Retrieve all historical health records for the patient")
    public ResponseEntity<List<HealthRecordResponse>> getMyRecords(
            @RequestHeader(value = "X-Dev-User-Id", required = false) String headerUserId,
            @RequestParam(value = "patientId", required = false) UUID paramPatientId) {

        UUID patientId = paramPatientId;
        if (patientId == null && headerUserId != null && !headerUserId.isBlank()) {
            patientId = UUID.fromString(headerUserId);
        }

        if (patientId == null) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(service.getRecordsForPatient(patientId));
    }

    @GetMapping("/patients/{patientId}/records")
    @Operation(summary = "Doctor access to patient records with HIPAA access audit trail")
    public ResponseEntity<List<HealthRecordResponse>> getPatientRecordsForDoctor(
            @PathVariable UUID patientId,
            @RequestHeader(value = "X-Doctor-Id", defaultValue = "doctor-system") String doctorId,
            HttpServletRequest request) {

        String ip = request.getRemoteAddr();
        List<HealthRecordResponse> records = service.getDoctorAuditedRecords(patientId, doctorId, ip);
        return ResponseEntity.ok(records);
    }
}
