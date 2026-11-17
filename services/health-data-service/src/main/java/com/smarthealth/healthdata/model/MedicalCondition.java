package com.smarthealth.healthdata.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "medical_conditions")
public class MedicalCondition {

    @Id
    private String id;

    @Indexed
    private String patientId;

    private String conditionName;
    private String diagnosedDate;
    private String status; // ACTIVE, MANAGED, RESOLVED
    private String notes;

    public MedicalCondition() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getConditionName() { return conditionName; }
    public void setConditionName(String conditionName) { this.conditionName = conditionName; }

    public String getDiagnosedDate() { return diagnosedDate; }
    public void setDiagnosedDate(String diagnosedDate) { this.diagnosedDate = diagnosedDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
