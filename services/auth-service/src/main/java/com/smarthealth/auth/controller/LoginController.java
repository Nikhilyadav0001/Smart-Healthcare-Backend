package com.smarthealth.auth.controller;

import com.smarthealth.auth.model.AuthenticatedUser;
import com.smarthealth.auth.model.User;
import com.smarthealth.auth.repository.UserRepository;
import com.smarthealth.auth.repository.UserRoleRepository;
import com.smarthealth.auth.service.LoginAttemptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.OffsetDateTime;
import java.util.*;

@Controller
@Tag(name = "Authentication & User Session", description = "Endpoints for user session inspection, REST login and HTML form login")
public class LoginController {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;

    public LoginController(
            UserRepository userRepository,
            UserRoleRepository userRoleRepository,
            PasswordEncoder passwordEncoder,
            LoginAttemptService loginAttemptService) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.loginAttemptService = loginAttemptService;
    }

    @GetMapping("/login")
    @Operation(summary = "HTML Login Page", description = "Renders the Thymeleaf form login interface supporting local credentials and OAuth2 Social Login (Google & GitHub).")
    public String login(HttpServletRequest request, CsrfToken csrfToken) {
        request.getSession(true);
        if (csrfToken != null) {
            csrfToken.getToken();
        }
        return "login";
    }

    @PostMapping("/auth/login")
    @ResponseBody
    @Operation(
            summary = "REST User Login (JSON)",
            description = "Authenticates a registered user with email and password. Checks BCrypt hash and verifies account is active. Establishes session and returns profile with assigned roles."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "400", description = "Missing email or password"),
            @ApiResponse(responseCode = "401", description = "Invalid email or password"),
            @ApiResponse(responseCode = "403", description = "Account not verified or temporarily locked")
    })
    public ResponseEntity<?> loginJson(
            @RequestBody(required = false) Map<String, String> body,
            HttpServletRequest request) {

        String email = body != null ? body.get("email") : null;
        String password = body != null ? body.get("password") : null;

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Email and password are required"));
        }

        email = email.trim().toLowerCase(Locale.ROOT);

        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid email or password"));
        }

        User user = userOpt.get();

        // Check if locked
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(OffsetDateTime.now())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Account is temporarily locked due to multiple failed login attempts. Please try again later."));
        }

        // Check password
        if (user.getPasswordHash() == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            loginAttemptService.recordFailure(user);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid email or password"));
        }

        // Check email verification status
        if (!user.isEmailVerified() || !"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "error", "Account email is not verified yet. Please check your verification email in Mailpit at http://localhost:8025 or open the verification link to activate your account.",
                            "emailVerified", false
                    ));
        }

        loginAttemptService.recordSuccess(user);

        // Fetch authorities
        List<String> roles = userRoleRepository.findByIdUserId(user.getId())
                .stream()
                .map(r -> r.getId().getRole())
                .distinct()
                .toList();

        List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .toList();

        AuthenticatedUser authUser = new AuthenticatedUser(user, authorities);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(authUser, null, authorities);

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        request.getSession(true).setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                securityContext
        );

        String primaryRole = roles.contains("ADMIN") ? "ADMIN" : (roles.contains("DOCTOR") ? "DOCTOR" : "PATIENT");
        String name = email.split("@")[0].replace('.', ' ');

        Map<String, Object> userData = new HashMap<>();
        userData.put("id", user.getId().toString());
        userData.put("uid", user.getId().toString());
        userData.put("email", user.getEmail());
        userData.put("name", name);
        userData.put("fullName", name);
        userData.put("role", primaryRole);
        userData.put("roles", roles);
        userData.put("emailVerified", true);

        return ResponseEntity.ok(Map.of("status", "SUCCESS", "user", userData));
    }

    @GetMapping({"/auth/me", "/me"})
    @ResponseBody
    @Operation(
            summary = "Get current authenticated user info",
            description = "Returns current session identity, verified email, assigned roles (PATIENT, DOCTOR, ADMIN), and profile attributes.",
            security = {
                    @SecurityRequirement(name = "bearerAuth"),
                    @SecurityRequirement(name = "basicAuth")
            }
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current authenticated user profile returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - No active authenticated session or valid token")
    })
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Object principal = authentication.getPrincipal();
        String userId = null;
        String email = authentication.getName();
        String name = email != null && email.contains("@") ? email.split("@")[0] : "User";
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(r -> r.startsWith("ROLE_") ? r.substring(5) : r)
                .toList();

        if (principal instanceof AuthenticatedUser authUser) {
            email = authUser.getUsername();
            if (authUser.getUserId() != null) {
                userId = authUser.getUserId().toString();
            }
            name = email != null && email.contains("@") ? email.split("@")[0] : "User";
        } else if (principal instanceof OAuth2User oauth2User) {
            if (oauth2User.getAttribute("email") != null) {
                email = oauth2User.getAttribute("email");
            }
            if (oauth2User.getAttribute("name") != null) {
                name = oauth2User.getAttribute("name");
            } else if (oauth2User.getAttribute("login") != null) {
                name = oauth2User.getAttribute("login");
            } else if (email != null && email.contains("@")) {
                name = email.split("@")[0];
            }
        }

        if (userId == null && email != null && !email.isBlank()) {
            Optional<User> dbUser = userRepository.findByEmailIgnoreCase(email.trim());
            if (dbUser.isPresent()) {
                userId = dbUser.get().getId().toString();
            }
        }

        Map<String, Object> resp = new HashMap<>();
        if (userId != null) {
            resp.put("id", userId);
            resp.put("uid", userId);
        }
        resp.put("email", email);
        resp.put("name", name);
        resp.put("fullName", name);
        resp.put("roles", roles);
        resp.put("role", roles.contains("ADMIN") ? "ADMIN" : (roles.contains("DOCTOR") ? "DOCTOR" : "PATIENT"));
        return ResponseEntity.ok(resp);
    }
}
