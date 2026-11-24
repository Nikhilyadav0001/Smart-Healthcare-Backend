package com.smarthealth.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth/demo")
@Tag(name = "Authentication Showcase", description = "Endpoints demonstrating HTTP Basic, Bearer (JWT), API Key, Admin Key, and OAuth 2.0 / Service-to-Service Authentication")
public class AuthDemoController {

    /**
     * 1. Public Endpoint - No Authentication
     */
    @GetMapping("/public")
    @Operation(
            summary = "Public Endpoint (No Auth)",
            description = "Unsecured endpoint accessible by anyone without any credentials."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully accessed public endpoint")
    })
    public ResponseEntity<Map<String, Object>> publicEndpoint() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("authenticationType", "NONE (Public)");
        response.put("message", "This endpoint does not require any authentication.");
        response.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(response);
    }

    /**
     * 2. HTTP Basic Authentication Demo (User or Service)
     */
    @GetMapping("/basic")
    @Operation(
            summary = "HTTP Basic Authentication Demo",
            description = "Requires HTTP Basic credentials in the 'Authorization: Basic <base64(username:password)>' header. Commonly used for Service-to-Service client authentication or direct user credentials.",
            security = @SecurityRequirement(name = "basicAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully authenticated via HTTP Basic"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid Basic credentials")
    })
    public ResponseEntity<Map<String, Object>> basicAuthDemo(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", "FAILED",
                    "error", "Unauthorized: Provide valid HTTP Basic credentials."
            ));
        }

        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("authenticationType", "HTTP Basic");
        response.put("principal", authentication.getName());
        response.put("roles", roles);
        response.put("message", "Authenticated successfully using HTTP Basic Authorization header.");
        response.put("serviceToServiceNote", "In microservices, services use Basic Auth on '/oauth2/token' with client_id:client_secret to exchange for a JWT token.");
        response.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(response);
    }

    /**
     * 3. Bearer Token (JWT) Authentication Demo
     */
    @GetMapping("/bearer-jwt")
    @Operation(
            summary = "Bearer Token (JWT) Authentication Demo",
            description = "Requires an RSA-signed JWT Bearer token in the 'Authorization: Bearer <token>' header. The token is decoded and validated by the Resource Server filter.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully validated JWT Bearer token"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid/expired JWT token")
    })
    public ResponseEntity<Map<String, Object>> bearerJwtDemo(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", "FAILED",
                    "error", "Unauthorized: Provide a valid JWT Bearer token."
            ));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("authenticationType", "Bearer Token (JWT)");
        response.put("principal", authentication.getName());

        if (authentication.getPrincipal() instanceof Jwt jwt) {
            response.put("subject", jwt.getSubject());
            response.put("issuer", jwt.getIssuer() != null ? jwt.getIssuer().toString() : "unknown");
            response.put("expiresAt", jwt.getExpiresAt() != null ? jwt.getExpiresAt().toString() : "none");
            response.put("claims", jwt.getClaims());
        } else {
            response.put("authorities", authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
        }

        response.put("message", "JWT signature and claims successfully verified by the Resource Server.");
        response.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(response);
    }

    /**
     * 4. API Key Authentication Demo (Standard or Admin)
     */
    @GetMapping("/api-key")
    @Operation(
            summary = "API Key Authentication Demo",
            description = "Requires an API key in 'X-API-KEY'. Standard: 'smarthealth-dev-secret-api-key-2026', Admin: 'smarthealth-admin-secret-key-2026'.",
            security = @SecurityRequirement(name = "apiKeyAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully authenticated via API Key"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid API Key")
    })
    public ResponseEntity<Map<String, Object>> apiKeyDemo(
            @Parameter(
                    name = "X-API-KEY",
                    in = ParameterIn.HEADER,
                    description = "API Key for client access (Standard or Admin)",
                    required = true,
                    example = "smarthealth-dev-secret-api-key-2026"
            )
            @RequestHeader(value = "X-API-KEY", required = false) String apiKeyHeader,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated() ||
                authentication.getAuthorities().stream().noneMatch(a -> "ROLE_API_CLIENT".equals(a.getAuthority()))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", "FAILED",
                    "error", "Unauthorized: Please provide a valid 'X-API-KEY' header."
            ));
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("authenticationType", isAdmin ? "Admin API Key" : "Standard API Key");
        response.put("headerUsed", "X-API-KEY");
        response.put("principal", authentication.getName());
        response.put("isAdmin", isAdmin);
        response.put("roles", authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
        response.put("message", isAdmin
                ? "Predefined Admin API Key validated. Client granted high-privilege ROLE_ADMIN."
                : "Standard API Key validated. Client granted ROLE_API_CLIENT authority.");
        response.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(response);
    }

    /**
     * 4b. Admin Predefined API Key Demo (Requires Admin Key)
     */
    @GetMapping("/admin-api-key")
    @Operation(
            summary = "Admin Predefined API Key Demo",
            description = "Protected administrative endpoint requiring the predefined Admin API Key: 'smarthealth-admin-secret-key-2026'.",
            security = @SecurityRequirement(name = "apiKeyAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Admin successfully authorized"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid API Key"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Standard API key lacks ROLE_ADMIN privilege")
    })
    public ResponseEntity<Map<String, Object>> adminApiKeyDemo(
            @Parameter(
                    name = "X-API-KEY",
                    in = ParameterIn.HEADER,
                    description = "Predefined Admin API Key",
                    required = true,
                    example = "smarthealth-admin-secret-key-2026"
            )
            @RequestHeader(value = "X-API-KEY", required = false) String apiKeyHeader,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", "FAILED",
                    "error", "Unauthorized: Please provide the Admin 'X-API-KEY' header."
            ));
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        if (!isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "status", "FORBIDDEN",
                    "error", "Forbidden: This administrative action requires the predefined Admin API Key."
            ));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("authenticationType", "Admin API Key");
        response.put("principal", authentication.getName());
        response.put("roles", authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
        response.put("message", "Access granted! Administrative operations are unlocked for this caller.");
        response.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(response);
    }

    /**
     * 5. OAuth 2.0 & Service-to-Service Information
     */
    @GetMapping("/oauth-info")
    @Operation(
            summary = "OAuth 2.0 & OIDC Endpoints and Instructions",
            description = "Lists all OAuth 2.0 / OIDC endpoints, registered client IDs, and instructions for Postman or Swagger.",
            security = @SecurityRequirement(name = "oauth2Auth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Information returned successfully")
    })
    public ResponseEntity<Map<String, Object>> oauthInfo() {
        Map<String, Object> response = new HashMap<>();
        response.put("authorizationServer", "Spring Authorization Server (OAuth 2.1 / OIDC 1.0)");
        response.put("tokenEndpoint", "http://localhost:8081/oauth2/token");
        response.put("authorizeEndpoint", "http://localhost:8081/oauth2/authorize");
        response.put("jwksEndpoint", "http://localhost:8081/oauth2/jwks");

        Map<String, Object> clients = new HashMap<>();
        clients.put("serviceToServiceClients", List.of(
                Map.of(
                        "clientId", "prediction-service",
                        "clientSecret", "nikhil",
                        "grantType", "client_credentials",
                        "scopes", List.of("prediction.read", "prediction.write", "health.read", "profile.read"),
                        "authMethod", "client_secret_basic (HTTP Basic in token request)"
                ),
                Map.of(
                        "clientId", "health-data-service",
                        "clientSecret", "nikhil",
                        "grantType", "client_credentials",
                        "scopes", List.of("health.read", "health.write"),
                        "authMethod", "client_secret_basic (HTTP Basic in token request)"
                )
        ));
        clients.put("browserBffClient", Map.of(
                "clientId", "healthapp-bff",
                "clientSecret", "nikhil",
                "grantTypes", List.of("authorization_code", "refresh_token"),
                "pkceRequired", true,
                "redirectUri", "http://localhost:8080/login/oauth2/code/healthapp"
        ));

        response.put("registeredClients", clients);
        response.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(response);
    }

    /**
     * 6. Service-to-Service Demonstration Endpoint
     */
    @PostMapping("/service-to-service")
    @Operation(
            summary = "Service-to-Service Communication Demonstration",
            description = "Demonstrates how backend microservices authenticate with each other. Accepts either HTTP Basic credentials of a registered service OR a JWT Bearer token acquired via Client Credentials.",
            security = {
                    @SecurityRequirement(name = "basicAuth"),
                    @SecurityRequirement(name = "bearerAuth")
            }
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Service successfully authenticated"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid service credentials")
    })
    public ResponseEntity<Map<String, Object>> serviceToServiceDemo(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", "FAILED",
                    "error", "Unauthorized: Service-to-service calls require either Basic Auth or Bearer JWT token."
            ));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("flow", "Service-to-Service (M2M)");
        response.put("authenticatedCaller", authentication.getName());
        response.put("authorities", authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());

        if (authHeader != null && authHeader.toLowerCase().startsWith("basic ")) {
            response.put("mechanism", "HTTP Basic (client_secret_basic / service credentials)");
            response.put("serviceToServiceExplanation",
                    "Service A calls Service B passing service credentials directly via 'Authorization: Basic <base64(client_id:client_secret)>'.");
        } else if (authHeader != null && authHeader.toLowerCase().startsWith("bearer ")) {
            response.put("mechanism", "OAuth2 Client Credentials Bearer JWT");
            response.put("serviceToServiceExplanation",
                    "Service A calls 'POST /oauth2/token' with HTTP Basic client credentials to obtain a signed JWT, then invokes Service B passing 'Authorization: Bearer <token>'.");
        } else {
            response.put("mechanism", authentication.getClass().getSimpleName());
        }

        response.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(response);
    }
}
