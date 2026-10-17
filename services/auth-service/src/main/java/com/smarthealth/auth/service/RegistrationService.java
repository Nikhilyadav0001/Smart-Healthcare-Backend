package com.smarthealth.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthealth.auth.dto.RegisterRequest;
import com.smarthealth.auth.dto.RegisterResponse;
import com.smarthealth.auth.dto.UserRegisteredEvent;
import com.smarthealth.auth.exception.ConflictException;
import com.smarthealth.auth.model.*;
import com.smarthealth.auth.repository.OutboxEventRepository;
import com.smarthealth.auth.repository.UserRepository;
import com.smarthealth.auth.repository.UserRoleRepository;
import com.smarthealth.auth.repository.VerificationTokenRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

@Service
public class RegistrationService {

    private static final String VERIFICATION_TOKEN_TYPE = "EMAIL_VERIFICATION";
    private static final String OUTBOX_STATUS_PENDING = "PENDING";
    private static final String OUTBOX_EVENT_TYPE = "user.registered";
    private static final String OUTBOX_AGGREGATE_TYPE = "USER";

    private static final long VERIFICATION_TOKEN_VALIDITY_HOURS = 24;

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom;

    public RegistrationService(
            UserRepository userRepository,
            UserRoleRepository userRoleRepository,
            VerificationTokenRepository verificationTokenRepository,
            OutboxEventRepository outboxEventRepository,
            PasswordEncoder passwordEncoder,
            ObjectMapper objectMapper) {

        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
        this.secureRandom = new SecureRandom();
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException(
                    "An account with this email already exists"
            );
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        // ------------------------------------------------------------
        // 1. Create the user
        // ------------------------------------------------------------

        User user = new User();

        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus(UserStatus.PENDING.name());
        user.setEmailVerified(false);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        User savedUser = userRepository.save(user);

        // ------------------------------------------------------------
        // 2. Assign the default USER role
        // ------------------------------------------------------------

        UserRole.UserRoleId roleId =
                new UserRole.UserRoleId(
                        savedUser.getId(),
                        "USER"
                );

        userRoleRepository.save(new UserRole(roleId));
        // ------------------------------------------------------------
        // 3. Generate a cryptographically secure verification token
        // ------------------------------------------------------------

        String rawVerificationToken = generateVerificationToken();
        String tokenHash = sha256(rawVerificationToken);

        VerificationToken verificationToken = new VerificationToken();

        verificationToken.setId(UUID.randomUUID());
        verificationToken.setUserId(savedUser.getId());
        verificationToken.setTokenHash(tokenHash);
        verificationToken.setType(VERIFICATION_TOKEN_TYPE);
        verificationToken.setExpiresAt(
                now.plusHours(VERIFICATION_TOKEN_VALIDITY_HOURS)
        );
        verificationToken.setUsedAt(null);

        verificationTokenRepository.save(verificationToken);

        // ------------------------------------------------------------
        // 4. Build the user.registered event
        // ------------------------------------------------------------

        UserRegisteredEvent event = new UserRegisteredEvent(
                savedUser.getId(),
                savedUser.getEmail(),
                rawVerificationToken
        );

        String payload = serializeEvent(event);

        // ------------------------------------------------------------
        // 5. Persist the outbox event in the SAME transaction
        // ------------------------------------------------------------

        OutboxEvent outboxEvent = new OutboxEvent();

        outboxEvent.setId(UUID.randomUUID());
        outboxEvent.setAggregateType(OUTBOX_AGGREGATE_TYPE);
        outboxEvent.setAggregateId(savedUser.getId());
        outboxEvent.setEventType(OUTBOX_EVENT_TYPE);
        outboxEvent.setPayload(payload);
        outboxEvent.setStatus(OUTBOX_STATUS_PENDING);
        outboxEvent.setAttempts(0);
        outboxEvent.setAvailableAt(now);
        outboxEvent.setCreatedAt(now);
        outboxEvent.setPublishedAt(null);

        outboxEventRepository.save(outboxEvent);

        return new RegisterResponse(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getStatus()
        );
    }

    private String generateVerificationToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    value.getBytes(StandardCharsets.UTF_8)
            );

            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(hash);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 algorithm is not available",
                    exception
            );
        }
    }

    private String serializeEvent(UserRegisteredEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Failed to serialize user.registered event",
                    exception
            );
        }
    }
}