package com.smarthealth.healthdata.repository;

import com.smarthealth.healthdata.model.MedicalCondition;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MedicalConditionRepository extends MongoRepository<MedicalCondition, String> {

    List<MedicalCondition> findByPatientId(String patientId);

    void deleteByPatientId(String patientId);
}
