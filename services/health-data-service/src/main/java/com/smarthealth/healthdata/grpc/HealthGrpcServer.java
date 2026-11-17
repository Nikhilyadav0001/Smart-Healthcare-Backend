package com.smarthealth.healthdata.grpc;

import com.google.protobuf.Timestamp;
import com.smarthealth.health.v1.*;
import com.smarthealth.healthdata.model.HealthRecordEntity;
import com.smarthealth.healthdata.repository.HealthRecordRepository;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

@Component
public class HealthGrpcServer extends HealthServiceGrpc.HealthServiceImplBase implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(HealthGrpcServer.class);

    private final int port;
    private final HealthRecordRepository repository;
    private Server server;
    private boolean isRunning = false;

    public HealthGrpcServer(
            @Value("${health-data.grpc.port:9083}") int port,
            HealthRecordRepository repository) {
        this.port = port;
        this.repository = repository;
    }

    @Override
    public void start() {
        try {
            server = ServerBuilder.forPort(port)
                    .addService(this)
                    .build()
                    .start();
            isRunning = true;
            log.info("HealthService gRPC server started, listening on port {}", port);
        } catch (IOException e) {
            log.error("Failed to start HealthService gRPC server on port {}: {}", port, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        if (server != null) {
            log.info("Stopping HealthService gRPC server...");
            server.shutdown();
            isRunning = false;
        }
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public void getLatestRecord(GetLatestRecordRequest request, StreamObserver<GetLatestRecordResponse> responseObserver) {
        try {
            String patientId = request.getPatientId();
            Optional<HealthRecordEntity> entityOpt = repository.findFirstByPatientIdOrderByRecordedAtDesc(patientId);

            if (entityOpt.isEmpty()) {
                responseObserver.onNext(GetLatestRecordResponse.getDefaultInstance());
                responseObserver.onCompleted();
                return;
            }

            HealthRecord protoRecord = toProto(entityOpt.get());
            GetLatestRecordResponse resp = GetLatestRecordResponse.newBuilder()
                    .setRecord(protoRecord)
                    .build();

            responseObserver.onNext(resp);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error in getLatestRecord gRPC: {}", e.getMessage(), e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getRecord(GetRecordRequest request, StreamObserver<GetRecordResponse> responseObserver) {
        try {
            Optional<HealthRecordEntity> entityOpt = repository.findById(request.getRecordId());
            if (entityOpt.isEmpty()) {
                responseObserver.onNext(GetRecordResponse.getDefaultInstance());
                responseObserver.onCompleted();
                return;
            }

            HealthRecord protoRecord = toProto(entityOpt.get());
            GetRecordResponse resp = GetRecordResponse.newBuilder()
                    .setRecord(protoRecord)
                    .build();

            responseObserver.onNext(resp);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error in getRecord gRPC: {}", e.getMessage(), e);
            responseObserver.onError(e);
        }
    }

    private HealthRecord toProto(HealthRecordEntity entity) {
        HealthRecord.Builder b = HealthRecord.newBuilder();
        b.setId(entity.getId() != null ? entity.getId() : "");
        b.setPatientId(entity.getPatientId() != null ? entity.getPatientId() : "");

        Instant recAt = entity.getRecordedAt() != null ? entity.getRecordedAt() : Instant.now();
        b.setRecordedAt(Timestamp.newBuilder().setSeconds(recAt.getEpochSecond()).setNanos(recAt.getNano()).build());

        if (entity.getAgeYears() != null) b.setAgeYears(entity.getAgeYears());

        String s = entity.getSex() != null ? entity.getSex().toUpperCase() : "";
        if (s.contains("MALE") && !s.contains("FE")) {
            b.setSex(Sex.SEX_MALE);
        } else if (s.contains("FEMALE")) {
            b.setSex(Sex.SEX_FEMALE);
        } else {
            b.setSex(Sex.SEX_OTHER);
        }

        if (entity.getHeightCm() != null) b.setHeightCm(entity.getHeightCm());
        if (entity.getWeightKg() != null) b.setWeightKg(entity.getWeightKg());
        if (entity.getBmi() != null) b.setBmi(entity.getBmi());
        if (entity.getSystolicBp() != null) b.setSystolicBp(entity.getSystolicBp());
        if (entity.getDiastolicBp() != null) b.setDiastolicBp(entity.getDiastolicBp());

        if (entity.getFastingGlucoseMgDl() != null) b.setFastingGlucoseMgDl(entity.getFastingGlucoseMgDl());
        if (entity.getHba1cPercent() != null) b.setHba1CPercent(entity.getHba1cPercent());
        if (entity.getTotalCholesterolMgDl() != null) b.setTotalCholesterolMgDl(entity.getTotalCholesterolMgDl());
        if (entity.getHdlMgDl() != null) b.setHdlMgDl(entity.getHdlMgDl());
        if (entity.getLdlMgDl() != null) b.setLdlMgDl(entity.getLdlMgDl());

        String smk = entity.getSmokingStatus() != null ? entity.getSmokingStatus().toUpperCase() : "";
        if (smk.contains("CURRENT")) {
            b.setSmokingStatus(SmokingStatus.SMOKING_STATUS_CURRENT);
        } else if (smk.contains("FORMER")) {
            b.setSmokingStatus(SmokingStatus.SMOKING_STATUS_FORMER);
        } else {
            b.setSmokingStatus(SmokingStatus.SMOKING_STATUS_NEVER);
        }

        String act = entity.getActivityLevel() != null ? entity.getActivityLevel().toUpperCase() : "";
        if (act.contains("HIGH")) {
            b.setActivityLevel(ActivityLevel.ACTIVITY_LEVEL_HIGH);
        } else if (act.contains("MOD")) {
            b.setActivityLevel(ActivityLevel.ACTIVITY_LEVEL_MODERATE);
        } else {
            b.setActivityLevel(ActivityLevel.ACTIVITY_LEVEL_LOW);
        }

        b.setHasHypertension(Boolean.TRUE.equals(entity.getHasHypertension()));
        b.setHasHeartDisease(Boolean.TRUE.equals(entity.getHasHeartDisease()));
        b.setFamilyHistoryDiabetes(Boolean.TRUE.equals(entity.getFamilyHistoryDiabetes()));

        return b.build();
    }
}
