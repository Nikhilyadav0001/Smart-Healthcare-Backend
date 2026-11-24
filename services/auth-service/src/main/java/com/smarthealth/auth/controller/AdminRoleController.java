package com.smarthealth.auth.controller;

import com.smarthealth.auth.dto.AdminUserSummaryResponse;
import com.smarthealth.auth.dto.UpdateRoleRequest;
import com.smarthealth.auth.model.User;
import com.smarthealth.auth.model.UserRole;
import com.smarthealth.auth.repository.UserRepository;
import com.smarthealth.auth.repository.UserRoleRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

@RestController
@RequestMapping("/auth/admin/users")
@Tag(
        name = "Admin User & Role Management",
        description = "Administrative endpoints to inspect registered users and update or reassign user roles (Requires ROLE_ADMIN via Admin API Key 'X-API-KEY: smarthealth-admin-secret-key-2026' or Admin Bearer Token)"
)
@SecurityRequirement(name = "apiKeyAuth")
@SecurityRequirement(name = "bearerAuth")
public class AdminRoleController {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    public AdminRoleController(UserRepository userRepository, UserRoleRepository userRoleRepository) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
    }

    @GetMapping
    @Operation(
            summary = "List all registered users",
            description = "Retrieves all user accounts along with their active security roles and account status."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "List of all registered users",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = AdminUserSummaryResponse.class)))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid credentials"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ROLE_ADMIN authority")
    })
    @Transactional(readOnly = true)
    public ResponseEntity<List<AdminUserSummaryResponse>> listAllUsers() {
        List<User> users = userRepository.findAll();
        List<AdminUserSummaryResponse> summaries = users.stream()
                .map(this::toSummary)
                .toList();
        return ResponseEntity.ok(summaries);
    }

    @GetMapping("/{userId}")
    @Operation(
            summary = "Get user account details and roles by User ID",
            description = "Retrieves detailed information and assigned roles for a specific user ID."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "User details and assigned roles",
                    content = @Content(schema = @Schema(implementation = AdminUserSummaryResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ROLE_ADMIN")
    })
    @Transactional(readOnly = true)
    public ResponseEntity<AdminUserSummaryResponse> getUserById(
            @Parameter(description = "UUID of the user", required = true)
            @PathVariable UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with ID: " + userId));
        return ResponseEntity.ok(toSummary(user));
    }

    @PutMapping("/{userId}/roles")
    @Operation(
            summary = "Update user roles",
            description = "Replaces the user's assigned roles with the specified role list (e.g. ['DOCTOR'], ['ADMIN'], ['PATIENT', 'DOCTOR']). Automatically removes the 'ROLE_' prefix if provided and converts to uppercase."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Roles updated successfully, returns updated user summary",
                    content = @Content(schema = @Schema(implementation = AdminUserSummaryResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid role specified or empty list"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ROLE_ADMIN")
    })
    @Transactional
    public ResponseEntity<AdminUserSummaryResponse> updateUserRoles(
            @Parameter(description = "UUID of the user to update", required = true)
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateRoleRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with ID: " + userId));

        List<String> rawRoles = request.roles();
        if (rawRoles == null || rawRoles.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one role must be provided");
        }

        Set<String> cleanRoles = new LinkedHashSet<>();
        for (String r : rawRoles) {
            if (r == null || r.trim().isEmpty()) {
                continue;
            }
            String role = r.trim().toUpperCase(Locale.ROOT);
            if (role.startsWith("ROLE_")) {
                role = role.substring(5);
            }
            cleanRoles.add(role);
        }

        if (cleanRoles.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No valid role names provided");
        }

        // Delete existing roles
        userRoleRepository.deleteByIdUserId(userId);
        userRoleRepository.flush();

        // Save new roles
        for (String roleName : cleanRoles) {
            UserRole.UserRoleId roleId = new UserRole.UserRoleId(userId, roleName);
            userRoleRepository.save(new UserRole(roleId));
        }

        user.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        User savedUser = userRepository.save(user);

        return ResponseEntity.ok(toSummary(savedUser));
    }

    @PatchMapping("/{userId}/role/{role}")
    @Operation(
            summary = "Assign a single role to user",
            description = "Quick shortcut to set a single primary role (e.g. DOCTOR, ADMIN, PATIENT, USER) for the specified user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role assigned successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ROLE_ADMIN")
    })
    @Transactional
    public ResponseEntity<AdminUserSummaryResponse> setSingleRole(
            @Parameter(description = "UUID of the user", required = true)
            @PathVariable UUID userId,
            @Parameter(description = "Role name (e.g. DOCTOR, ADMIN, PATIENT)", required = true)
            @PathVariable String role) {

        return updateUserRoles(userId, new UpdateRoleRequest(List.of(role)));
    }

    private AdminUserSummaryResponse toSummary(User user) {
        List<String> roles = userRoleRepository.findByIdUserId(user.getId())
                .stream()
                .map(r -> r.getId().getRole())
                .distinct()
                .toList();

        return new AdminUserSummaryResponse(
                user.getId(),
                user.getEmail(),
                user.getStatus(),
                user.isEmailVerified(),
                roles,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
