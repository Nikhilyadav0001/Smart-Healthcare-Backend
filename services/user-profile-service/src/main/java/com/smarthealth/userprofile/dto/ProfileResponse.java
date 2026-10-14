package com.smarthealth.userprofile.dto;

import java.io.Serializable;
import java.time.LocalDate;

public record ProfileResponse(
        String userId,
        String email,
        String fullName,
        LocalDate dateOfBirth,
        String gender,
        String phone,
        String assignedDoctorId) implements Serializable {}