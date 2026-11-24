package com.smarthealth.auth.service;

import com.smarthealth.auth.dto.VerifyEmailResponse;
import com.smarthealth.auth.model.User;
import com.smarthealth.auth.model.UserStatus;
import com.smarthealth.auth.model.VerificationToken;
import com.smarthealth.auth.repository.UserRepository;
import com.smarthealth.auth.repository.VerificationTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;

@Service
public class EmailVerificationService {

    private static final String VERIFICATION_TOKEN_TYPE = "EMAIL_VERIFICATION";

    private final VerificationTokenRepository verificationTokenRepository;
    private final UserRepository userRepository;

    public EmailVerificationService(
            VerificationTokenRepository verificationTokenRepository,
            UserRepository userRepository) {

        this.verificationTokenRepository = verificationTokenRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public VerifyEmailResponse verify(String rawToken) {
        String token = rawToken == null ? "" : rawToken.trim();

        if (token.isBlank()) {
            throw new IllegalArgumentException("Verification token is required");
        }

        String tokenHash = sha256(token);

        VerificationToken verificationToken =
                verificationTokenRepository.findByTokenHash(tokenHash)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Invalid verification token"
                                ));

        if (!VERIFICATION_TOKEN_TYPE.equals(verificationToken.getType())) {
            throw new IllegalArgumentException(
                    "Invalid verification token"
            );
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        if (!verificationToken.getExpiresAt().isAfter(now)) {
            throw new IllegalArgumentException(
                    "Verification token has expired"
            );
        }

        User user = userRepository.findById(verificationToken.getUserId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User associated with verification token was not found"
                        ));

        if (user.isEmailVerified()) {
            if (verificationToken.getUsedAt() == null) {
                verificationToken.setUsedAt(now);
                verificationTokenRepository.save(verificationToken);
            }
            return new VerifyEmailResponse(
                    user.getId(),
                    user.getEmail(),
                    user.getStatus(),
                    true
            );
        }

        if (verificationToken.getUsedAt() != null) {
            throw new IllegalArgumentException(
                    "Verification token has already been used"
            );
        }

        verificationToken.setUsedAt(now);

        user.setEmailVerified(true);
        user.setStatus(UserStatus.ACTIVE.name());
        user.setUpdatedAt(now);

        userRepository.save(user);
        verificationTokenRepository.save(verificationToken);

        return new VerifyEmailResponse(
                user.getId(),
                user.getEmail(),
                user.getStatus(),
                true
        );
    }

    private String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm not available", ex);
        }
    }
}
