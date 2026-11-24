package com.smarthealth.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthealth.auth.dto.UserRegisteredEvent;
import com.smarthealth.auth.model.OutboxEvent;
import com.smarthealth.auth.model.User;
import com.smarthealth.auth.model.UserIdentity;
import com.smarthealth.auth.model.UserRole;
import com.smarthealth.auth.model.UserStatus;
import com.smarthealth.auth.repository.OutboxEventRepository;
import com.smarthealth.auth.repository.UserIdentityRepository;
import com.smarthealth.auth.repository.UserRepository;
import com.smarthealth.auth.repository.UserRoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.UUID;

@Service
public class SocialLoginService {

    private static final String OUTBOX_STATUS_PENDING = "PENDING";
    private static final String OUTBOX_EVENT_TYPE = "user.registered";
    private static final String OUTBOX_AGGREGATE_TYPE = "USER";

    private final UserRepository userRepository;
    private final UserIdentityRepository userIdentityRepository;
    private final UserRoleRepository userRoleRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public SocialLoginService(
            UserRepository userRepository,
            UserIdentityRepository userIdentityRepository,
            UserRoleRepository userRoleRepository,
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper) {

        this.userRepository = userRepository;
        this.userIdentityRepository = userIdentityRepository;
        this.userRoleRepository = userRoleRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public User loginOrCreate(
            String provider,
            String providerSubject,
            String email,
            boolean emailVerified) {

        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException(
                    "Social provider is required"
            );
        }

        if (providerSubject == null || providerSubject.isBlank()) {
            throw new IllegalArgumentException(
                    "Provider subject is required"
            );
        }

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "A verified email address is required for social login"
            );
        }

        if (!emailVerified) {
            throw new IllegalStateException(
                    "Social provider email must be verified"
            );
        }

        String normalizedProvider =
                provider.trim().toUpperCase(Locale.ROOT);

        String normalizedProviderSubject =
                providerSubject.trim();

        String normalizedEmail =
                email.trim().toLowerCase(Locale.ROOT);

        // ------------------------------------------------------------
        // 1. Existing provider identity
        // ------------------------------------------------------------

        var existingIdentity =
                userIdentityRepository.findByProviderAndProviderSubject(
                        normalizedProvider,
                        normalizedProviderSubject
                );

        if (existingIdentity.isPresent()) {

            User user = userRepository.findById(
                    existingIdentity.get().getUserId()
            ).orElseThrow(() ->
                    new IllegalStateException(
                            "User linked to social identity no longer exists"
                    )
            );

            ensureUserCanLogin(user);

            return user;
        }

        // ------------------------------------------------------------
        // 2. Existing local account with the same verified email
        // ------------------------------------------------------------

        var existingUser =
                userRepository.findByEmailIgnoreCase(normalizedEmail);

        if (existingUser.isPresent()) {

            User user = existingUser.get();

            ensureUserCanLogin(user);

            createIdentity(
                    user.getId(),
                    normalizedProvider,
                    normalizedProviderSubject,
                    normalizedEmail
            );

            return user;
        }

        // ------------------------------------------------------------
        // 3. Create a brand-new social user
        // ------------------------------------------------------------

        OffsetDateTime now =
                OffsetDateTime.now(ZoneOffset.UTC);

        User user = new User();

        user.setId(UUID.randomUUID());
        user.setEmail(normalizedEmail);

        // Social users authenticate through the external provider.
        user.setPasswordHash(null);

        // Provider has already verified the user's identity/email.
        user.setStatus(UserStatus.ACTIVE.name());
        user.setEmailVerified(true);

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        User savedUser = userRepository.save(user);

        // ------------------------------------------------------------
        // 4. Assign default USER role
        // ------------------------------------------------------------

        UserRole.UserRoleId roleId =
                new UserRole.UserRoleId(
                        savedUser.getId(),
                        "USER"
                );

        userRoleRepository.save(
                new UserRole(roleId)
        );

        // ------------------------------------------------------------
        // 5. Store federated identity
        // ------------------------------------------------------------

        createIdentity(
                savedUser.getId(),
                normalizedProvider,
                normalizedProviderSubject,
                normalizedEmail
        );

        // ------------------------------------------------------------
        // 6. Publish user.registered through transactional outbox
        //
        // Social users do not need email verification, therefore
        // verificationToken is null.
        // ------------------------------------------------------------

        String userEmail = savedUser.getEmail() != null ? savedUser.getEmail() : "";
        String fullName = userEmail.contains("@") ? userEmail.split("@")[0] : "Social User";

        UserRegisteredEvent event =
                new UserRegisteredEvent(
                        UUID.randomUUID().toString(),
                        1,
                        now.toString(),
                        savedUser.getId().toString(),
                        userEmail,
                        fullName,
                        "PATIENT",
                        "https://smarthealthcare.dpdns.org"
                );

        String payload = serializeEvent(event);

        OutboxEvent outboxEvent = new OutboxEvent();

        outboxEvent.setId(UUID.randomUUID());
        outboxEvent.setAggregateType(
                OUTBOX_AGGREGATE_TYPE
        );
        outboxEvent.setAggregateId(
                savedUser.getId()
        );
        outboxEvent.setEventType(
                OUTBOX_EVENT_TYPE
        );
        outboxEvent.setPayload(payload);
        outboxEvent.setStatus(
                OUTBOX_STATUS_PENDING
        );
        outboxEvent.setAttempts(0);
        outboxEvent.setAvailableAt(now);
        outboxEvent.setCreatedAt(now);
        outboxEvent.setPublishedAt(null);

        outboxEventRepository.save(outboxEvent);

        return savedUser;
    }

    private void createIdentity(
            UUID userId,
            String provider,
            String providerSubject,
            String email) {

        if (userIdentityRepository
                .existsByProviderAndProviderSubject(
                        provider,
                        providerSubject)) {
            return;
        }

        UserIdentity identity = new UserIdentity();

        identity.setId(UUID.randomUUID());
        identity.setUserId(userId);
        identity.setProvider(provider);
        identity.setProviderSubject(providerSubject);
        identity.setEmail(email);
        identity.setCreatedAt(
                OffsetDateTime.now(ZoneOffset.UTC)
        );

        userIdentityRepository.save(identity);
    }

    private void ensureUserCanLogin(User user) {

        if (UserStatus.DISABLED.name().equals(user.getStatus())) {
            throw new IllegalStateException(
                    "User account is disabled"
            );
        }

        if (UserStatus.REJECTED.name().equals(user.getStatus())) {
            throw new IllegalStateException(
                    "User account has been rejected"
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