package com.smarthealth.userprofile.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "profiles")
public class ProfileEntity {
    @Id
    @Column(name = "user_id", length = 36)
    private String userId;

    @Column(nullable = false)
    private String email;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    private String gender;

    private String phone;

    @Column(name = "assigned_doctor_id")
    private String assignedDoctorId;

    public ProfileEntity() {

    }
    public ProfileEntity(String userId, String email) {
        this.userId = userId;
        this.email = email;
    }

    public String getUserId() {
        return userId;
    }
    public String getEmail() {
        return email;
    }
    public String getFullName() {
        return fullName;
    }
    public void setFullName(String v) {
        this.fullName = v;
    }
    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }
    public void setDateOfBirth(LocalDate v) {
        this.dateOfBirth = v;
    }
    public String getGender() {
        return gender;
    }
    public void setGender(String v) {
        this.gender = v;
    }
    public String getPhone() {
        return phone;
    }
    public void setPhone(String v) {
        this.phone = v;
    }
    public String getAssignedDoctorId() {
        return assignedDoctorId;
    }
}