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
 * If the key matches the configured valid key, an authenticated ApiKeyAuthenticationToken is
 * set in the Spring SecurityContext with 'ROLE_API_CLIENT'.
 */
@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    public static final String API_KEY_HEADER = "X-API-KEY";

    @Value("${auth.api-key:smarthealth-dev-secret-api-key-2026}")
    private String validApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String apiKey = request.getHeader(API_KEY_HEADER);

        if (apiKey != null && !apiKey.isBlank()) {
            if (validApiKey.equals(apiKey.trim())) {
                ApiKeyAuthenticationToken authentication = new ApiKeyAuthenticationToken(
                        "api-client",
                        apiKey,
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
