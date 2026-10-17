package com.smarthealth.auth.controller;

import com.smarthealth.auth.dto.RegisterRequest;
import com.smarthealth.auth.dto.RegisterResponse;
import com.smarthealth.auth.dto.VerifyEmailResponse;
import com.smarthealth.auth.service.EmailVerificationService;
import com.smarthealth.auth.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
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
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        RegisterResponse response =
                registrationService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/verify-email")
    public ResponseEntity<VerifyEmailResponse> verifyEmail(
            @RequestParam String token) {

        VerifyEmailResponse response =
                emailVerificationService.verify(token);

        return ResponseEntity.ok(response);
    }
}