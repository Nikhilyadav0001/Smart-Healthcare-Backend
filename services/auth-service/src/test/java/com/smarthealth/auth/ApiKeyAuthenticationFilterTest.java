package com.smarthealth.auth;

import com.smarthealth.auth.model.ApiKey;
import com.smarthealth.auth.security.ApiKeyAuthenticationFilter;
import com.smarthealth.auth.service.ApiKeyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiKeyAuthenticationFilterTest {

    private ApiKeyService apiKeyService;
    private ApiKeyAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        apiKeyService = mock(ApiKeyService.class);
        filter = new ApiKeyAuthenticationFilter(apiKeyService);
        ReflectionTestUtils.setField(filter, "validApiKey", "test-secret-key-123");
        ReflectionTestUtils.setField(filter, "adminApiKey", "test-admin-key-999");
        SecurityContextHolder.clearContext();
    }

    @Test
    void whenValidApiKeyProvided_thenSetsAuthenticated() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-API-KEY", "test-secret-key-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertTrue(SecurityContextHolder.getContext().getAuthentication().isAuthenticated());
        assertEquals("api-client", SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> "ROLE_API_CLIENT".equals(a.getAuthority())));
    }

    @Test
    void whenAdminApiKeyProvided_thenSetsAdminAuthenticated() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-API-KEY", "test-admin-key-999");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertTrue(SecurityContextHolder.getContext().getAuthentication().isAuthenticated());
        assertEquals("admin-user", SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())));
        assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> "ROLE_API_CLIENT".equals(a.getAuthority())));
    }

    @Test
    void whenDynamicDatabaseApiKeyProvided_thenSetsAuthenticated() throws Exception {
        ApiKey dynamicKey = new ApiKey();
        dynamicKey.setName("postman-test");
        when(apiKeyService.validateAndTouch("sh_live_dynamic123")).thenReturn(Optional.of(dynamicKey));
        when(apiKeyService.toGrantedAuthorities(dynamicKey))
                .thenReturn(List.of(new SimpleGrantedAuthority("ROLE_API_CLIENT")));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-API-KEY", "sh_live_dynamic123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertTrue(SecurityContextHolder.getContext().getAuthentication().isAuthenticated());
        assertEquals("api-key:postman-test", SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> "ROLE_API_CLIENT".equals(a.getAuthority())));
    }

    @Test
    void whenInvalidApiKeyProvided_thenDoesNotAuthenticate() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-API-KEY", "wrong-key");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void whenNoApiKeyHeader_thenDoesNotAuthenticate() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
