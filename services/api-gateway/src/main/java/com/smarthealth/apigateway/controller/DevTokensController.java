package com.smarthealth.apigateway.controller;

import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class DevTokensController {

    @GetMapping("/dev/tokens")
    public Map<String, Object> getTokens(
            @RegisteredOAuth2AuthorizedClient("healthapp") OAuth2AuthorizedClient authorizedClient) {

        if (authorizedClient == null) {
            return Map.of("error", "No authorized OAuth2 client found in session");
        }

        String accessToken = authorizedClient.getAccessToken() != null
                ? authorizedClient.getAccessToken().getTokenValue()
                : null;

        String refreshToken = authorizedClient.getRefreshToken() != null
                ? authorizedClient.getRefreshToken().getTokenValue()
                : null;

        return Map.of(
                "client_id", authorizedClient.getClientRegistration().getClientId(),
                "access_token", accessToken != null ? accessToken : "none",
                "refresh_token", refreshToken != null ? refreshToken : "none",
                "expires_at", authorizedClient.getAccessToken() != null && authorizedClient.getAccessToken().getExpiresAt() != null
                        ? authorizedClient.getAccessToken().getExpiresAt().toString() : "none"
        );
    }
}
