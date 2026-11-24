package com.smarthealth.auth.controller;

import com.smarthealth.auth.dto.RegisterRequest;
import com.smarthealth.auth.dto.RegisterResponse;
import com.smarthealth.auth.dto.VerifyEmailResponse;
import com.smarthealth.auth.service.EmailVerificationService;
import com.smarthealth.auth.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "User Registration & Email Verification", description = "Endpoints for new patient registration and email token verification")
public class RegistrationController {

    private final RegistrationService registrationService;
    private final EmailVerificationService emailVerificationService;

    public RegistrationController(
            RegistrationService registrationService,
            EmailVerificationService emailVerificationService) {

        this.registrationService = registrationService;
        this.emailVerificationService = emailVerificationService;
    }

    @PostMapping("/register")
    @Operation(
            summary = "Register a new patient account",
            description = "Creates a new user record in PostgreSQL auth_db and initiates the registration saga with an email verification token."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created successfully, verification email sent"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload or validation error"),
            @ApiResponse(responseCode = "409", description = "Email already registered in the system")
    })
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        RegisterResponse response =
                registrationService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/verify-email")
    @Operation(
            summary = "Verify email address via token",
            description = "Validates the one-time verification token received by the patient in their email (or Mailpit in dev) and activates the account."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Email successfully verified"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired verification token")
    })
    public ResponseEntity<VerifyEmailResponse> verifyEmail(
            @Parameter(description = "Verification token sent to user's email", required = true)
            @RequestParam String token) {

        VerifyEmailResponse response =
                emailVerificationService.verify(token);

        return ResponseEntity.ok(response);
    }
}
