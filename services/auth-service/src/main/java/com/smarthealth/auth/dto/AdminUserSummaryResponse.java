package com.smarthealth.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "User profile summary with assigned roles for administration")
public record AdminUserSummaryResponse(
        @Schema(description = "Unique user identifier", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        UUID id,

        @Schema(description = "User email address", example = "doctor.smith@smarthealth.com")
        String email,

        @Schema(description = "Lifecycle status", example = "ACTIVE")
        String status,

        @Schema(description = "Email verification status", example = "true")
        boolean emailVerified,

        @Schema(description = "Assigned security roles", example = "[\"DOCTOR\", \"USER\"]")
        List<String> roles,

        @Schema(description = "Timestamp when the user registered")
        OffsetDateTime createdAt,

        @Schema(description = "Timestamp when the user account or roles were last modified")
        OffsetDateTime updatedAt
) {
}
