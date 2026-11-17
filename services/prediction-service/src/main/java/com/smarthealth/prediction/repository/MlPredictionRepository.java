package com.smarthealth.prediction.repository;

import com.smarthealth.prediction.model.MlPredictionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MlPredictionRepository extends JpaRepository<MlPredictionEntity, UUID> {
    List<MlPredictionEntity> findByPatientIdOrderByCreatedAtDesc(UUID patientId);
}
