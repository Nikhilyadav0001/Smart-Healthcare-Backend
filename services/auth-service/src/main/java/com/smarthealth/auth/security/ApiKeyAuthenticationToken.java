package com.smarthealth.auth.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Collections;

/**
 * Authentication token representing an authenticated API Key client.
 */
public class ApiKeyAuthenticationToken extends AbstractAuthenticationToken {

    private final String apiKey;
    private final Object principal;

    public ApiKeyAuthenticationToken(String apiKey) {
        super(Collections.emptyList());
        this.apiKey = apiKey;
        this.principal = "api-client";
        setAuthenticated(false);
    }

    public ApiKeyAuthenticationToken(Object principal, String apiKey, Collection<? extends GrantedAuthority> authorities) {
        super(authorities != null ? authorities : Collections.emptyList());
        this.apiKey = apiKey;
        this.principal = principal;
        super.setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return apiKey;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }
}
