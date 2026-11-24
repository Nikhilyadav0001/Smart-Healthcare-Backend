package com.smarthealth.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 320, message = "Email must not exceed 320 characters")
        @Schema(description = "User email address", example = "kunal@gmail.com")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 128,
                message = "Password must be between 8 and 128 characters")
        @Schema(description = "Account password (min 8 characters)", example = "12345678")
        String password,

        @Schema(description = "Full name of the user", example = "Kunal Sharma")
        String fullName,

        @Schema(description = "Role to register (PATIENT or DOCTOR)", example = "PATIENT")
        String role
) {
}
