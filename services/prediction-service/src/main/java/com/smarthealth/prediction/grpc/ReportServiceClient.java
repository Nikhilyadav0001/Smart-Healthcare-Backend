package com.smarthealth.prediction.grpc;

import com.google.protobuf.Timestamp;
import com.smarthealth.report.v1.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ReportServiceClient {

    private static final Logger log = LoggerFactory.getLogger(ReportServiceClient.class);

    private final String host;
    private final int port;

    public ReportServiceClient(
            @Value("${go-service.grpc.host:localhost}") String host,
            @Value("${go-service.grpc.port:9095}") int port) {
        this.host = host;
        this.port = port;
    }

    public byte[] generateClinicalReportPdf(
            String patientName,
            String disease,
            double probability,
            String riskLevel,
            List<String> topFactors,
            List<String> recommendations) {

        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();

        try {
            ReportServiceGrpc.ReportServiceBlockingStub stub = ReportServiceGrpc.newBlockingStub(channel)
                    .withDeadlineAfter(5, TimeUnit.SECONDS);

            Disease protoDisease;
            String dUpper = disease.toUpperCase();
            if (dUpper.contains("DIABETES")) {
                protoDisease = Disease.DISEASE_DIABETES;
            } else if (dUpper.contains("HEART")) {
                protoDisease = Disease.DISEASE_HEART_DISEASE;
            } else if (dUpper.contains("STROKE")) {
                protoDisease = Disease.DISEASE_STROKE;
            } else {
                protoDisease = Disease.DISEASE_UNSPECIFIED;
            }

            RiskLevel protoRisk;
            String rUpper = riskLevel.toUpperCase();
            if (rUpper.contains("HIGH")) {
                protoRisk = RiskLevel.RISK_LEVEL_HIGH;
            } else if (rUpper.contains("MED")) {
                protoRisk = RiskLevel.RISK_LEVEL_MEDIUM;
            } else {
                protoRisk = RiskLevel.RISK_LEVEL_LOW;
            }

            Instant now = Instant.now();
            Timestamp timestamp = Timestamp.newBuilder()
                    .setSeconds(now.getEpochSecond())
                    .setNanos(now.getNano())
                    .build();

            GenerateReportRequest req = GenerateReportRequest.newBuilder()
                    .setPatientName(patientName != null ? patientName : "Patient")
                    .setDisease(protoDisease)
                    .setProbability(probability)
                    .setRiskLevel(protoRisk)
                    .addAllTopFactors(topFactors != null ? topFactors : List.of())
                    .addAllRecommendations(recommendations != null ? recommendations : List.of())
                    .setGeneratedAt(timestamp)
                    .build();

            log.info("Invoking go-service gRPC GenerateReport at {}:{} for patient={}", host, port, patientName);
            GenerateReportResponse resp = stub.generateReport(req);
            return resp.getPdf().toByteArray();

        } catch (Exception e) {
            log.error("Failed to generate PDF report from go-service gRPC: {}", e.getMessage());
            throw new RuntimeException("Clinical report generation service unavailable: " + e.getMessage(), e);
        } finally {
            channel.shutdown();
        }
    }
}
