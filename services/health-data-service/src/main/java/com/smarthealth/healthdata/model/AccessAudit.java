package com.smarthealth.healthdata.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "access_audit")
public class AccessAudit {

    @Id
    private String id;

    @Indexed
    private String patientId;

    private String accessedByDoctorId;
    private Instant accessedAt = Instant.now();
    private String action; // READ_RECORD, EXPORT_REPORT
    private String ipAddress;

    public AccessAudit() {
    }

    public static AccessAudit of(String patientId, String doctorId, String action, String ipAddress) {
        AccessAudit a = new AccessAudit();
        a.patientId = patientId;
        a.accessedByDoctorId = doctorId;
        a.action = action;
        a.ipAddress = ipAddress;
        a.accessedAt = Instant.now();
        return a;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getAccessedByDoctorId() { return accessedByDoctorId; }
    public void setAccessedByDoctorId(String accessedByDoctorId) { this.accessedByDoctorId = accessedByDoctorId; }

    public Instant getAccessedAt() { return accessedAt; }
    public void setAccessedAt(Instant accessedAt) { this.accessedAt = accessedAt; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
}
