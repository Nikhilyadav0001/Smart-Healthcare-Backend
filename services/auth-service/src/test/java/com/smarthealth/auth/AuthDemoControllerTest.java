package com.smarthealth.auth;

import com.smarthealth.auth.controller.AuthDemoController;
import com.smarthealth.auth.security.ApiKeyAuthenticationToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AuthDemoControllerTest {

    private AuthDemoController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthDemoController();
    }

    @Test
    void testPublicEndpoint() {
        ResponseEntity<Map<String, Object>> resp = controller.publicEndpoint();
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("SUCCESS", resp.getBody().get("status"));
        assertEquals("NONE (Public)", resp.getBody().get("authenticationType"));
    }

    @Test
    void testBasicAuth_Authenticated() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "user@smarthealth.io",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_PATIENT"))
        );

        ResponseEntity<Map<String, Object>> resp = controller.basicAuthDemo(auth);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("HTTP Basic", resp.getBody().get("authenticationType"));
        assertEquals("user@smarthealth.io", resp.getBody().get("principal"));
    }

    @Test
    void testApiKey_Standard_Authenticated() {
        ApiKeyAuthenticationToken auth = new ApiKeyAuthenticationToken(
                "api-client",
                "test-api-key",
                List.of(new SimpleGrantedAuthority("ROLE_API_CLIENT"))
        );

        ResponseEntity<Map<String, Object>> resp = controller.apiKeyDemo("test-api-key", auth);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("Standard API Key", resp.getBody().get("authenticationType"));
        assertEquals("api-client", resp.getBody().get("principal"));
        assertEquals(false, resp.getBody().get("isAdmin"));
    }

    @Test
    void testApiKey_Admin_Authenticated() {
        ApiKeyAuthenticationToken auth = new ApiKeyAuthenticationToken(
                "admin-user",
                "test-admin-key",
                List.of(
                        new SimpleGrantedAuthority("ROLE_ADMIN"),
                        new SimpleGrantedAuthority("ROLE_API_CLIENT")
                )
        );

        ResponseEntity<Map<String, Object>> resp = controller.apiKeyDemo("test-admin-key", auth);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("Admin API Key", resp.getBody().get("authenticationType"));
        assertEquals("admin-user", resp.getBody().get("principal"));
        assertEquals(true, resp.getBody().get("isAdmin"));
    }

    @Test
    void testAdminApiKeyEndpoint_Success() {
        ApiKeyAuthenticationToken auth = new ApiKeyAuthenticationToken(
                "admin-user",
                "test-admin-key",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        ResponseEntity<Map<String, Object>> resp = controller.adminApiKeyDemo("test-admin-key", auth);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("Admin API Key", resp.getBody().get("authenticationType"));
    }

    @Test
    void testAdminApiKeyEndpoint_ForbiddenForStandardUser() {
        ApiKeyAuthenticationToken auth = new ApiKeyAuthenticationToken(
                "api-client",
                "test-api-key",
                List.of(new SimpleGrantedAuthority("ROLE_API_CLIENT"))
        );

        ResponseEntity<Map<String, Object>> resp = controller.adminApiKeyDemo("test-api-key", auth);
        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }

    @Test
    void testBearerJwt_Authenticated() {
        Jwt jwt = Jwt.withTokenValue("mock-token")
                .header("alg", "RS256")
                .claim("sub", "user@smarthealth.io")
                .claim("roles", List.of("PATIENT"))
                .issuer("http://localhost:8081")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken jwtAuth =
                new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(
                        jwt,
                        List.of(new SimpleGrantedAuthority("ROLE_PATIENT"))
                );

        ResponseEntity<Map<String, Object>> resp = controller.bearerJwtDemo(jwtAuth);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("Bearer Token (JWT)", resp.getBody().get("authenticationType"));
        assertEquals("user@smarthealth.io", resp.getBody().get("subject"));
    }

    @Test
    void testOauthInfo() {
        ResponseEntity<Map<String, Object>> resp = controller.oauthInfo();
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody().get("tokenEndpoint"));
        assertNotNull(resp.getBody().get("registeredClients"));
    }
}
