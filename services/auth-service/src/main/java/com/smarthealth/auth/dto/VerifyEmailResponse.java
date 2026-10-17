package com.smarthealth.auth.dto;

import java.util.UUID;

public record VerifyEmailResponse(
        UUID userId,
        String email,
        String status,
        boolean emailVerified
) {
}