package com.smarthealth.userprofile.dto;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateProfileRequest(
        @Size(max = 150) String fullName,
        @Past LocalDate dateOfBirth,
        @Size(max = 20) String gender,
        @Size(max = 30) String phone) {}