package com.smarthealth.auth.dto;

public record UserRegisteredEvent(
        String eventId,
        int schemaVersion,
        String occurredAt,
        String userId,
        String email,
        String fullName,
        String role,
        String verificationUrl
) {
}
