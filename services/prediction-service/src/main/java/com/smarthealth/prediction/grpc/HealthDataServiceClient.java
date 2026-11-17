package com.smarthealth.prediction.grpc;

import com.smarthealth.health.v1.GetLatestRecordRequest;
import com.smarthealth.health.v1.GetLatestRecordResponse;
import com.smarthealth.health.v1.HealthRecord;
import com.smarthealth.health.v1.HealthServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class HealthDataServiceClient {

    private static final Logger log = LoggerFactory.getLogger(HealthDataServiceClient.class);

    private final String host;
    private final int port;

    public HealthDataServiceClient(
            @Value("${health-data-service.grpc.host:localhost}") String host,
            @Value("${health-data-service.grpc.port:9083}") int port) {
        this.host = host;
        this.port = port;
    }

    public Optional<HealthRecord> getLatestRecord(UUID patientId) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();

        try {
            HealthServiceGrpc.HealthServiceBlockingStub stub = HealthServiceGrpc.newBlockingStub(channel)
                    .withDeadlineAfter(3, TimeUnit.SECONDS);

            GetLatestRecordRequest req = GetLatestRecordRequest.newBuilder()
                    .setPatientId(patientId.toString())
                    .build();

            log.info("Fetching latest health record via gRPC from {}:{} for patientId={}", host, port, patientId);
            GetLatestRecordResponse resp = stub.getLatestRecord(req);
            if (resp.hasRecord()) {
                return Optional.of(resp.getRecord());
            }
            return Optional.empty();

        } catch (Exception e) {
            log.warn("Could not retrieve latest health record from health-data-service via gRPC: {}", e.getMessage());
            return Optional.empty();
        } finally {
            channel.shutdown();
        }
    }
}
