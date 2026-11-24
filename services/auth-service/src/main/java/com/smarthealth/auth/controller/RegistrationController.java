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
import org.springframework.http.MediaType;
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
    public ResponseEntity<?> verifyEmail(
            @Parameter(description = "Verification token sent to user's email", required = true)
            @RequestParam String token,
            @RequestHeader(value = "Accept", required = false) String acceptHeader) {

        VerifyEmailResponse response =
                emailVerificationService.verify(token);

        if (acceptHeader != null && acceptHeader.contains("text/html")) {
            String html = """
                    <!DOCTYPE html>
                    <html lang="en">
                    <head>
                        <meta charset="UTF-8">
                        <title>Account Verified - Smart Healthcare</title>
                        <meta http-equiv="refresh" content="3;url=/?verified=true" />
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <style>
                            body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; display: grid; place-items: center; min-height: 100vh; margin: 0; background: #f8fafc; color: #1e293b; }
                            .card { background: white; padding: 40px; border-radius: 16px; box-shadow: 0 10px 25px -5px rgba(0,0,0,0.05); text-align: center; max-width: 440px; border: 1px solid #e2e8f0; }
                            .icon { font-size: 44px; margin-bottom: 16px; color: #10b981; }
                            h2 { margin: 0 0 10px; font-size: 22px; font-weight: 700; }
                            p { color: #64748b; font-size: 14px; margin: 0 0 24px; line-height: 1.5; }
                            a { display: inline-block; background: #0f766e; color: white; padding: 12px 28px; border-radius: 8px; text-decoration: none; font-weight: 600; font-size: 14px; }
                            a:hover { background: #115e59; }
                        </style>
                    </head>
                    <body>
                        <div class="card">
                            <div class="icon">&#10004;</div>
                            <h2>Email Verified Successfully!</h2>
                            <p>Your account for <strong>%s</strong> has been verified. Redirecting you to sign in...</p>
                            <a href="/?verified=true">Proceed to Sign In</a>
                        </div>
                    </body>
                    </html>
                    """.formatted(response.email());
            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
        }

        return ResponseEntity.ok(response);
    }
}
