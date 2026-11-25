package com.smarthealth.auth.security;

import com.smarthealth.auth.service.ApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filter that intercepts requests containing the 'X-API-KEY' header.
 * - Predefined Admin Key ('auth.admin-api-key') -> ROLE_ADMIN, ROLE_API_CLIENT, ROLE_USER, ROLE_SERVICE
 * - Predefined Standard Key ('auth.api-key') -> ROLE_API_CLIENT, ROLE_SERVICE
 * - Dynamic DB-backed API Key ('sh_live_...') -> stored roles from auth_db.api_keys
 */
@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    public static final String API_KEY_HEADER = "X-API-KEY";

    private final ApiKeyService apiKeyService;

    @Value("${auth.api-key:smarthealth-dev-secret-api-key-2026}")
    private String validApiKey;

    @Value("${auth.admin-api-key:smarthealth-admin-secret-key-2026}")
    private String adminApiKey;

    public ApiKeyAuthenticationFilter() {
        this.apiKeyService = null;
    }

    @Autowired
    public ApiKeyAuthenticationFilter(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String apiKey = request.getHeader(API_KEY_HEADER);

        if (apiKey != null && !apiKey.isBlank()) {
            String trimmedKey = apiKey.trim();
            if (adminApiKey != null && adminApiKey.equals(trimmedKey)) {
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
                ApiKeyAuthenticationToken authentication = new ApiKeyAuthenticationToken(
                        "api-client",
                        trimmedKey,
                        List.of(
                                new SimpleGrantedAuthority("ROLE_API_CLIENT"),
                                new SimpleGrantedAuthority("ROLE_SERVICE")
                        )
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else if (apiKeyService != null) {
                apiKeyService.validateAndTouch(trimmedKey).ifPresent(dynamicKey -> {
                    ApiKeyAuthenticationToken authentication = new ApiKeyAuthenticationToken(
                            "api-key:" + dynamicKey.getName(),
                            trimmedKey,
                            apiKeyService.toGrantedAuthorities(dynamicKey)
                    );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                });
            }
        }

        filterChain.doFilter(request, response);
    }
}
