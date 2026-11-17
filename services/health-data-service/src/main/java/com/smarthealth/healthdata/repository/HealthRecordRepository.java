package com.smarthealth.healthdata.repository;

import com.smarthealth.healthdata.model.HealthRecordEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface HealthRecordRepository extends MongoRepository<HealthRecordEntity, String> {

    List<HealthRecordEntity> findByPatientIdOrderByRecordedAtDesc(String patientId);

    Optional<HealthRecordEntity> findFirstByPatientIdOrderByRecordedAtDesc(String patientId);

    void deleteByPatientId(String patientId);
}
