package com.smarthealth.auth.service;

import com.smarthealth.auth.model.User;
import com.smarthealth.auth.model.UserIdentity;
import com.smarthealth.auth.model.UserRole;
import com.smarthealth.auth.model.UserStatus;
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

    private final UserRepository userRepository;
    private final UserIdentityRepository userIdentityRepository;
    private final UserRoleRepository userRoleRepository;

    public SocialLoginService(
            UserRepository userRepository,
            UserIdentityRepository userIdentityRepository,
            UserRoleRepository userRoleRepository) {
        this.userRepository = userRepository;
        this.userIdentityRepository = userIdentityRepository;
        this.userRoleRepository = userRoleRepository;
    }

    @Transactional
    public User loginOrCreate(
            String provider,
            String providerSubject,
            String email,
            boolean emailVerified) {

        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException("Social provider is required");
        }

        if (providerSubject == null || providerSubject.isBlank()) {
            throw new IllegalArgumentException("Provider subject is required");
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

        String normalizedProvider = provider.trim().toUpperCase(Locale.ROOT);
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        // ------------------------------------------------------------
        // 1. Existing social identity
        // ------------------------------------------------------------

        var existingIdentity =
                userIdentityRepository.findByProviderAndProviderSubject(
                        normalizedProvider,
                        providerSubject
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

        User user;

        if (existingUser.isPresent()) {
            user = existingUser.get();

            ensureUserCanLogin(user);

            // Link the verified social identity to the existing account.
            createIdentity(
                    user.getId(),
                    normalizedProvider,
                    providerSubject,
                    normalizedEmail
            );

            return user;
        }

        // ------------------------------------------------------------
        // 3. Create a new social user
        // ------------------------------------------------------------

        OffsetDateTime now =
                OffsetDateTime.now(ZoneOffset.UTC);

        user = new User();

        user.setId(UUID.randomUUID());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(null);
        user.setStatus(UserStatus.ACTIVE.name());
        user.setEmailVerified(true);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        User savedUser = userRepository.save(user);

        // ------------------------------------------------------------
        // 4. Assign the default USER role
        // ------------------------------------------------------------

        UserRole.UserRoleId roleId =
                new UserRole.UserRoleId(
                        savedUser.getId(),
                        "USER"
                );

        userRoleRepository.save(new UserRole(roleId));

        // ------------------------------------------------------------
        // 5. Store the social identity
        // ------------------------------------------------------------

        createIdentity(
                savedUser.getId(),
                normalizedProvider,
                providerSubject,
                normalizedEmail
        );

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
}