package com.smarthealth.auth.dto;

import java.util.UUID;

public record UserRegisteredEvent(
        UUID userId,
        String email,
        String verificationToken
) {
}