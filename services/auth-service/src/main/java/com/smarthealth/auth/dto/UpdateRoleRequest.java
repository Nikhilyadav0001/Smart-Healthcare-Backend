package com.smarthealth.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Schema(description = "Request payload to update the assigned roles of a user")
public record UpdateRoleRequest(
        @Schema(
                description = "List of new roles to assign (e.g. ['ROLE_DOCTOR'], ['ADMIN'], ['PATIENT'], ['USER'])",
                example = "[\"DOCTOR\"]",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotEmpty(message = "At least one role must be provided")
        List<String> roles
) {
}
