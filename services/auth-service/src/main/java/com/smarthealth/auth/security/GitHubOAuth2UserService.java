package com.smarthealth.auth.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GitHubOAuth2UserService
        implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final DefaultOAuth2UserService delegate;
    private final RestClient restClient;

    public GitHubOAuth2UserService() {
        this.delegate = new DefaultOAuth2UserService();
        this.restClient = RestClient.builder().build();
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest)
            throws OAuth2AuthenticationException {

        OAuth2User user = delegate.loadUser(userRequest);

        String email = user.getAttribute("email");

        if (email == null || email.isBlank()) {
            email = loadPrimaryVerifiedEmail(userRequest);
        }

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(
                    new org.springframework.security.oauth2.core.OAuth2Error(
                            "email_not_available"
                    ),
                    "No verified GitHub email address is available"
            );
        }

        Map<String, Object> attributes =
                new HashMap<>(user.getAttributes());

        attributes.put("email", email);
        attributes.put("email_verified", true);

        String nameAttributeKey = determineNameAttributeKey(user);

        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                nameAttributeKey
        );
    }

    private String loadPrimaryVerifiedEmail(
            OAuth2UserRequest userRequest) {

        String accessToken =
                userRequest.getAccessToken().getTokenValue();

        List<Map<String, Object>> emails =
                restClient.get()
                        .uri("https://api.github.com/user/emails")
                        .headers(headers ->
                                headers.setBearerAuth(accessToken))
                        .retrieve()
                        .body(List.class);

        if (emails == null) {
            return null;
        }

        return emails.stream()
                .filter(email -> Boolean.TRUE.equals(
                        email.get("verified")))
                .filter(email -> Boolean.TRUE.equals(
                        email.get("primary")))
                .map(email -> email.get("email"))
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .findFirst()
                .orElseGet(() ->
                        emails.stream()
                                .filter(email -> Boolean.TRUE.equals(
                                        email.get("verified")))
                                .map(email -> email.get("email"))
                                .filter(String.class::isInstance)
                                .map(String.class::cast)
                                .findFirst()
                                .orElse(null)
                );
    }

    private String determineNameAttributeKey(OAuth2User user) {

        if (user.getAttributes().containsKey("id")) {
            return "id";
        }

        if (user.getAttributes().containsKey("login")) {
            return "login";
        }

        throw new IllegalStateException(
                "GitHub user response does not contain a usable name attribute"
        );
    }
}