package com.smarthealth.auth.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.Nullable;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * Decorates OAuth2AuthorizationService with refresh token reuse detection.
 * Per RFC 6749 / OAuth 2.0 Security BCP:
 * When a previously rotated refresh token is presented again,
 * the entire token family (the authorization row) is immediately revoked.
 */
public class ReuseDetectionOAuth2AuthorizationService implements OAuth2AuthorizationService {

    private static final Logger log = LoggerFactory.getLogger(ReuseDetectionOAuth2AuthorizationService.class);

    private final OAuth2AuthorizationService delegate;
    private final JdbcTemplate jdbcTemplate;

    public ReuseDetectionOAuth2AuthorizationService(
            OAuth2AuthorizationService delegate,
            JdbcTemplate jdbcTemplate) {
        this.delegate = delegate;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(OAuth2Authorization authorization) {
        // Check if an existing authorization exists to detect refresh token rotation
        OAuth2Authorization existing = delegate.findById(authorization.getId());
        if (existing != null && existing.getRefreshToken() != null) {
            String oldRefreshToken = existing.getRefreshToken().getToken().getTokenValue();
            OAuth2Authorization.Token<OAuth2RefreshToken> newToken = authorization.getRefreshToken();
            String newRefreshToken = newToken != null ? newToken.getToken().getTokenValue() : null;

            // If the refresh token changed, the old one was rotated and is now "used"
            if (newRefreshToken != null && !oldRefreshToken.equals(newRefreshToken)) {
                recordUsedRefreshToken(oldRefreshToken, authorization.getId());
            }
        }

        delegate.save(authorization);
    }

    @Override
    public void remove(OAuth2Authorization authorization) {
        delegate.remove(authorization);
    }

    @Override
    @Nullable
    public OAuth2Authorization findById(String id) {
        return delegate.findById(id);
    }

    @Override
    @Nullable
    public OAuth2Authorization findByToken(String token, @Nullable OAuth2TokenType tokenType) {
        OAuth2Authorization authorization = delegate.findByToken(token, tokenType);

        if (authorization == null && (tokenType == null || OAuth2TokenType.REFRESH_TOKEN.equals(tokenType))) {
            // Check if this token was a previously used refresh token
            String tokenHash = hashToken(token);
            List<String> authorizationIds = jdbcTemplate.query(
                    "SELECT authorization_id FROM used_refresh_tokens WHERE token_value = ? OR token_value = ?",
                    (rs, rowNum) -> rs.getString("authorization_id"),
                    tokenHash, token
            );

            if (!authorizationIds.isEmpty()) {
                String targetAuthId = authorizationIds.getFirst();
                log.warn("SECURITY ALERT: Refresh token replay/reuse detected! Revoking authorization family: {}", targetAuthId);

                // Revoke the whole family (all tokens associated with this authorization)
                OAuth2Authorization family = delegate.findById(targetAuthId);
                if (family != null) {
                    delegate.remove(family);
                } else {
                    jdbcTemplate.update("DELETE FROM oauth2_authorization WHERE id = ?", targetAuthId);
                }
            }
        }

        return authorization;
    }

    private void recordUsedRefreshToken(String tokenValue, String authorizationId) {
        try {
            String tokenHash = hashToken(tokenValue);
            jdbcTemplate.update(
                    "INSERT INTO used_refresh_tokens (id, token_value, authorization_id, used_at) VALUES (?, ?, ?, ?) " +
                    "ON CONFLICT (token_value) DO NOTHING",
                    UUID.randomUUID(),
                    tokenHash,
                    authorizationId,
                    OffsetDateTime.now(ZoneOffset.UTC)
            );
        } catch (Exception e) {
            log.warn("Failed to record used refresh token: {}", e.getMessage());
        }
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            return token;
        }
    }
}
