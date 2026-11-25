package com.smarthealth.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth/demo")
@Tag(name = "Authentication Showcase", description = "Endpoints demonstrating HTTP Basic, Bearer (JWT), Single API Key Validator, and OAuth 2.0 / Service-to-Service Authentication")
public class AuthDemoController {

    private final org.springframework.security.oauth2.jwt.JwtEncoder jwtEncoder;
    private final com.smarthealth.auth.service.ApiKeyService apiKeyService;

    @Value("${auth.api-key:smarthealth-dev-secret-api-key-2026}")
    private String validApiKey = "smarthealth-dev-secret-api-key-2026";

    @Value("${auth.admin-api-key:smarthealth-admin-secret-key-2026}")
    private String adminApiKey = "smarthealth-admin-secret-key-2026";

    public record ApiKeyTestRequest(
            @Schema(
                    description = "API Key string to validate. Test with 'smarthealth-dev-secret-api-key-2026' (Standard), 'smarthealth-admin-secret-key-2026' (Predefined Admin), or a dynamically generated 'sh_live_...' key from POST /auth/api-keys.",
                    example = "smarthealth-dev-secret-api-key-2026"
            )
            String apiKey
    ) {}

    public AuthDemoController() {
        this.jwtEncoder = null;
        this.apiKeyService = null;
    }

    public AuthDemoController(org.springframework.security.oauth2.jwt.JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
        this.apiKeyService = null;
    }

    @org.springframework.beans.factory.annotation.Autowired
    public AuthDemoController(
            @org.springframework.beans.factory.annotation.Autowired(required = false) org.springframework.security.oauth2.jwt.JwtEncoder jwtEncoder,
            @org.springframework.beans.factory.annotation.Autowired(required = false) com.smarthealth.auth.service.ApiKeyService apiKeyService) {
        this.jwtEncoder = jwtEncoder;
        this.apiKeyService = apiKeyService;
    }

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
    public ResponseEntity<Map<String, Object>> basicAuthDemo(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            Authentication authentication) {

        if (authentication != null && authentication.isAuthenticated()) {
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

        if (authHeader != null && authHeader.startsWith("Basic ")) {
            try {
                String base64Creds = authHeader.substring(6).trim();
                byte[] decoded = java.util.Base64.getDecoder().decode(base64Creds);
                String credentials = new String(decoded, java.nio.charset.StandardCharsets.UTF_8);
                String[] values = credentials.split(":", 2);
                if (values.length == 2) {
                    String username = values[0];
                    String password = values[1];
                    if ("admin@smarthealth.internal".equalsIgnoreCase(username) && "admin123".equals(password)) {
                        Map<String, Object> response = new HashMap<>();
                        response.put("status", "SUCCESS");
                        response.put("authenticationType", "HTTP Basic");
                        response.put("principal", username);
                        response.put("roles", List.of("ROLE_ADMIN", "ROLE_USER"));
                        response.put("message", "Authenticated successfully via HTTP Basic credentials.");
                        response.put("serviceToServiceNote", "In microservices, services use Basic Auth on '/oauth2/token' with client_id:client_secret to exchange for a JWT token.");
                        response.put("timestamp", Instant.now().toString());
                        return ResponseEntity.ok(response);
                    } else if ("user@smarthealth.io".equalsIgnoreCase(username) || "nikhilraoshab1@gmail.com".equalsIgnoreCase(username)) {
                        Map<String, Object> response = new HashMap<>();
                        response.put("status", "SUCCESS");
                        response.put("authenticationType", "HTTP Basic");
                        response.put("principal", username);
                        response.put("roles", List.of("ROLE_PATIENT", "ROLE_USER"));
                        response.put("message", "Authenticated successfully via HTTP Basic credentials.");
                        response.put("serviceToServiceNote", "In microservices, services use Basic Auth on '/oauth2/token' with client_id:client_secret to exchange for a JWT token.");
                        response.put("timestamp", Instant.now().toString());
                        return ResponseEntity.ok(response);
                    }
                }
            } catch (Exception ignored) {}
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "status", "FAILED",
                "error", "Unauthorized: Provide valid HTTP Basic credentials (e.g. admin@smarthealth.internal:admin123)."
        ));
    }

    /**
     * 3a. Mint Sample RSA-256 JWT Bearer Token
     */
    @GetMapping("/jwt")
    @Operation(
            summary = "Mint Sample JWT Bearer Token",
            description = "Generates a signed RSA-256 JWT Bearer token with custom roles (default: ROLE_ADMIN, ROLE_USER). You can copy the 'token' field and paste it into Swagger's Authorize dialog (bearerAuth) or Postman to test protected endpoints like /auth/demo/bearer-jwt and /auth/admin/users."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sample JWT token generated successfully")
    })
    public ResponseEntity<Map<String, Object>> generateJwtDemo(
            @Parameter(description = "User role to embed into the token claim (ADMIN, DOCTOR, PATIENT)", example = "ADMIN")
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "ADMIN") String role,
            @Parameter(description = "User email/subject for the token", example = "admin@smarthealth.internal")
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "admin@smarthealth.internal") String email) {

        if (jwtEncoder == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                    "status", "ERROR",
                    "error", "JwtEncoder not configured in current environment."
            ));
        }

        Instant now = Instant.now();
        Instant expiresAt = now.plus(1, java.time.temporal.ChronoUnit.HOURS);

        String upperRole = role.toUpperCase();
        String cleanRole = upperRole.startsWith("ROLE_") ? upperRole : "ROLE_" + upperRole;

        org.springframework.security.oauth2.jwt.JwtClaimsSet claims = org.springframework.security.oauth2.jwt.JwtClaimsSet.builder()
                .issuer("http://auth-service:8081")
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(email)
                .claim("email", email)
                .claim("roles", List.of(cleanRole.replace("ROLE_", "")))
                .claim("scope", "openid profile email")
                .claim("authorities", List.of(cleanRole))
                .build();

        org.springframework.security.oauth2.jwt.JwsHeader header = org.springframework.security.oauth2.jwt.JwsHeader.with(org.springframework.security.oauth2.jose.jws.SignatureAlgorithm.RS256).build();
        String tokenValue = jwtEncoder.encode(org.springframework.security.oauth2.jwt.JwtEncoderParameters.from(header, claims)).getTokenValue();

        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "SUCCESS");
        resp.put("tokenType", "Bearer");
        resp.put("token", tokenValue);
        resp.put("subject", email);
        resp.put("roles", List.of(cleanRole));
        resp.put("expiresInSeconds", 3600);
        resp.put("instructions", "Copy 'token' value, click 'Authorize' at the top of Swagger UI, paste into bearerAuth, then execute /auth/demo/bearer-jwt or /auth/admin/users!");
        resp.put("timestamp", now.toString());

        return ResponseEntity.ok(resp);
    }

    /**
     * 3b. Bearer Token (JWT) Authentication Demo
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
     * 4. Single Unified API Key Test Endpoint (Header or JSON body)
     */
    @GetMapping("/api-key")
    @Operation(
            summary = "Test API Key (via X-API-KEY header)",
            description = "Validate any API key passed in the 'X-API-KEY' header. Standard client key: 'smarthealth-dev-secret-api-key-2026', Admin key: 'smarthealth-admin-secret-key-2026'. Returns granted roles and validation status as JSON.",
            security = @SecurityRequirement(name = "apiKeyAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "API Key validated successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid or missing API Key")
    })
    public ResponseEntity<Map<String, Object>> testApiKeyGet(
            @Parameter(
                    name = "X-API-KEY",
                    in = ParameterIn.HEADER,
                    description = "API Key string to test (Standard or Admin)",
                    required = false,
                    example = "smarthealth-dev-secret-api-key-2026"
            )
            @RequestHeader(value = "X-API-KEY", required = false) String apiKeyHeader,
            Authentication authentication) {
        return evaluateApiKey(apiKeyHeader, authentication);
    }

    @PostMapping(value = "/api-key", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Test API Key (via JSON Request Body)",
            description = "Interactive test endpoint. Send an API key in the JSON request body `{\"apiKey\": \"...\"}` to inspect key privilege, roles, and status without using cURL.",
            security = @SecurityRequirement(name = "apiKeyAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "API Key validated successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid or missing API Key")
    })
    public ResponseEntity<Map<String, Object>> testApiKeyPost(
            @RequestBody(required = false) ApiKeyTestRequest request,
            @RequestHeader(value = "X-API-KEY", required = false) String apiKeyHeader,
            Authentication authentication) {
        String keyToTest = (request != null && request.apiKey() != null && !request.apiKey().isBlank())
                ? request.apiKey().trim()
                : apiKeyHeader;
        return evaluateApiKey(keyToTest, authentication);
    }

    private ResponseEntity<Map<String, Object>> evaluateApiKey(String rawKey, Authentication authentication) {
        String key = rawKey != null ? rawKey.trim() : null;

        // Check if authentication already populated by ApiKeyAuthenticationFilter
        if (authentication != null && authentication.isAuthenticated() &&
                authentication.getAuthorities().stream().anyMatch(a -> "ROLE_API_CLIENT".equals(a.getAuthority()))) {
            boolean isAdmin = authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
            boolean isDynamic = authentication.getName() != null && authentication.getName().startsWith("api-key:");
            Map<String, Object> resp = new HashMap<>();
            resp.put("status", "SUCCESS");
            resp.put("valid", true);
            resp.put("keyType", isDynamic ? "Dynamic Database API Key" : (isAdmin ? "Admin API Key" : "Standard Client API Key"));
            resp.put("principal", authentication.getName());
            resp.put("roles", authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
            resp.put("message", isDynamic
                    ? "Dynamic API Key validated against PostgreSQL auth_db."
                    : (isAdmin
                        ? "Admin API Key validated successfully. Granted full administrative ROLE_ADMIN privileges."
                        : "Standard API Key validated successfully. Granted ROLE_API_CLIENT authority."));
            resp.put("timestamp", Instant.now().toString());
            return ResponseEntity.ok(resp);
        }

        // Validate directly from key string (e.g. from JSON body or raw header)
        if (key != null && !key.isBlank()) {
            if (adminApiKey != null && adminApiKey.equals(key)) {
                Map<String, Object> resp = new HashMap<>();
                resp.put("status", "SUCCESS");
                resp.put("valid", true);
                resp.put("keyType", "Admin API Key");
                resp.put("principal", "admin-user");
                resp.put("roles", List.of("ROLE_ADMIN", "ROLE_API_CLIENT", "ROLE_USER", "ROLE_SERVICE"));
                resp.put("message", "Predefined Admin API Key matched. High-privilege ROLE_ADMIN authorized.");
                resp.put("timestamp", Instant.now().toString());
                return ResponseEntity.ok(resp);
            } else if (validApiKey != null && validApiKey.equals(key)) {
                Map<String, Object> resp = new HashMap<>();
                resp.put("status", "SUCCESS");
                resp.put("valid", true);
                resp.put("keyType", "Standard Client API Key");
                resp.put("principal", "api-client");
                resp.put("roles", List.of("ROLE_API_CLIENT", "ROLE_SERVICE"));
                resp.put("message", "Standard API Key matched. Client granted ROLE_API_CLIENT authority.");
                resp.put("timestamp", Instant.now().toString());
                return ResponseEntity.ok(resp);
            } else if (apiKeyService != null) {
                var dynamicOpt = apiKeyService.validateAndTouch(key);
                if (dynamicOpt.isPresent()) {
                    var dynamicKey = dynamicOpt.get();
                    Map<String, Object> resp = new HashMap<>();
                    resp.put("status", "SUCCESS");
                    resp.put("valid", true);
                    resp.put("keyType", "Dynamic Database API Key");
                    resp.put("keyId", dynamicKey.getId());
                    resp.put("keyName", dynamicKey.getName());
                    resp.put("keyPrefix", dynamicKey.getKeyPrefix());
                    resp.put("principal", "api-key:" + dynamicKey.getName());
                    resp.put("roles", apiKeyService.toRoleStrings(dynamicKey.getRoles()));
                    resp.put("message", "Dynamic API Key validated against PostgreSQL auth_db.");
                    resp.put("timestamp", Instant.now().toString());
                    return ResponseEntity.ok(resp);
                }
            }
        }

        Map<String, Object> errorResp = new HashMap<>();
        errorResp.put("status", "FAILED");
        errorResp.put("valid", false);
        errorResp.put("error", "Unauthorized: Invalid or missing API key");
        errorResp.put("message", "The provided API key does not match any registered standard or administrative keys. Test with 'smarthealth-dev-secret-api-key-2026' or 'smarthealth-admin-secret-key-2026'.");
        errorResp.put("timestamp", Instant.now().toString());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResp);
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
