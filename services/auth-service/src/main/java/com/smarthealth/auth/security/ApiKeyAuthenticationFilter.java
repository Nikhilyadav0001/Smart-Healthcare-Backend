package com.smarthealth.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filter that intercepts requests containing the 'X-API-KEY' header.
 * - If matches 'auth.admin-api-key', authenticates as 'admin-user' with 'ROLE_ADMIN' and 'ROLE_API_CLIENT'.
 * - If matches 'auth.api-key', authenticates as 'api-client' with 'ROLE_API_CLIENT'.
 */
@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    public static final String API_KEY_HEADER = "X-API-KEY";

    @Value("${auth.api-key:smarthealth-dev-secret-api-key-2026}")
    private String validApiKey;

    @Value("${auth.admin-api-key:smarthealth-admin-secret-key-2026}")
    private String adminApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String apiKey = request.getHeader(API_KEY_HEADER);

        if (apiKey != null && !apiKey.isBlank()) {
            String trimmedKey = apiKey.trim();
            if (adminApiKey != null && adminApiKey.equals(trimmedKey)) {
                // Predefined Admin API Key
                ApiKeyAuthenticationToken authentication = new ApiKeyAuthenticationToken(
                        "admin-user",
                        trimmedKey,
                        List.of(
                                new SimpleGrantedAuthority("ROLE_ADMIN"),
                                new SimpleGrantedAuthority("ROLE_API_CLIENT"),
                                new SimpleGrantedAuthority("ROLE_USER"),
                                new SimpleGrantedAuthority("ROLE_SERVICE")
                        )
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else if (validApiKey != null && validApiKey.equals(trimmedKey)) {
                // Standard Client API Key
                ApiKeyAuthenticationToken authentication = new ApiKeyAuthenticationToken(
                        "api-client",
                        trimmedKey,
                        List.of(
                                new SimpleGrantedAuthority("ROLE_API_CLIENT"),
                                new SimpleGrantedAuthority("ROLE_SERVICE")
                        )
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
}
