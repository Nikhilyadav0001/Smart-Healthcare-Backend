package com.smarthealth.apigateway.dto;

public record CurrentUserResponse(
        String subject,
        String email,
        String name
) {
}