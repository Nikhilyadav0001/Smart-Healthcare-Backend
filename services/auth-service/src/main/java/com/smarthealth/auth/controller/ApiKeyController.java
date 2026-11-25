package com.smarthealth.auth.controller;

import com.smarthealth.auth.model.ApiKey;
import com.smarthealth.auth.service.ApiKeyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth/api-keys")
@Tag(
        name = "API Key Management",
        description = "Generate, list, and revoke dynamic database-backed API keys alongside the predefined Admin and Standard API keys (Requires ROLE_ADMIN)"
)
@SecurityRequirement(name = "apiKeyAuth")
@SecurityRequirement(name = "bearerAuth")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    public record CreateApiKeyRequest(
            @Schema(description = "Human-readable label for the dynamic API key", example = "mobile-client-key")
            String name,
            @Schema(description = "Roles granted to this API key (default: ['API_CLIENT', 'SERVICE'])", example = "[\"API_CLIENT\", \"USER\"]")
            List<String> roles
    ) {}

    public record ApiKeySummaryResponse(
            UUID id,
            String name,
            String keyPrefix,
            List<String> roles,
            boolean active,
            String createdBy,
            OffsetDateTime createdAt,
            OffsetDateTime lastUsedAt
    ) {}

    @PostMapping
    @Operation(
            summary = "Generate a new dynamic API key",
            description = "Creates a cryptographically random API key ('sh_live_...'), stores its SHA-256 hash in PostgreSQL auth_db, and returns the raw key once. Test the generated key immediately on '/auth/demo/api-key'!"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Dynamic API key generated successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing credentials"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ROLE_ADMIN (e.g. Predefined Admin Key or Admin JWT)")
    })
    public ResponseEntity<Map<String, Object>> generateApiKey(
            @RequestBody(required = false) CreateApiKeyRequest request,
            Authentication authentication) {

        String name = request != null ? request.name() : null;
        List<String> roles = request != null ? request.roles() : null;
        String createdBy = authentication != null ? authentication.getName() : "admin";

        ApiKeyService.GeneratedApiKey created = apiKeyService.generateKey(name, roles, createdBy);
        ApiKey meta = created.metadata();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "CREATED");
        body.put("id", meta.getId());
        body.put("name", meta.getName());
        body.put("apiKey", created.plaintextKey());
        body.put("keyPrefix", meta.getKeyPrefix());
        body.put("roles", created.authorities());
        body.put("active", meta.isActive());
        body.put("createdBy", meta.getCreatedBy());
        body.put("createdAt", meta.getCreatedAt());
        body.put("note", "Copy 'apiKey' now — only its SHA-256 hash is stored in auth_db. Test it in 'X-API-KEY' header or POST /auth/demo/api-key.");
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping
    @Operation(
            summary = "List all dynamically generated API keys",
            description = "Returns metadata (prefix, roles, active status, lastUsedAt) for all dynamic API keys stored in auth_db. Raw secrets are never exposed after creation."
    )
    public ResponseEntity<List<ApiKeySummaryResponse>> listApiKeys() {
        List<ApiKeySummaryResponse> keys = apiKeyService.listAll().stream()
                .map(k -> new ApiKeySummaryResponse(
                        k.getId(),
                        k.getName(),
                        k.getKeyPrefix(),
                        apiKeyService.toRoleStrings(k.getRoles()),
                        k.isActive(),
                        k.getCreatedBy(),
                        k.getCreatedAt(),
                        k.getLastUsedAt()
                ))
                .toList();
        return ResponseEntity.ok(keys);
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Revoke a dynamic API key",
            description = "Deactivates a dynamic API key in auth_db so it is immediately rejected on subsequent requests."
    )
    public ResponseEntity<Map<String, Object>> revokeApiKey(@PathVariable UUID id) {
        ApiKey revoked = apiKeyService.revoke(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "API key not found: " + id));
        return ResponseEntity.ok(Map.of(
                "status", "REVOKED",
                "id", revoked.getId(),
                "name", revoked.getName(),
                "keyPrefix", revoked.getKeyPrefix(),
                "active", revoked.isActive()
        ));
    }
}
