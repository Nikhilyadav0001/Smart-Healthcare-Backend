package com.smarthealth.healthdata.repository;

import com.smarthealth.healthdata.model.AccessAudit;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AccessAuditRepository extends MongoRepository<AccessAudit, String> {

    List<AccessAudit> findByPatientIdOrderByAccessedAtDesc(String patientId);
}
