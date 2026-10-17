package com.smarthealth.auth.repository;

import com.smarthealth.auth.model.UserIdentity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserIdentityRepository extends JpaRepository<UserIdentity, UUID> {

    Optional<UserIdentity> findByProviderAndProviderSubject(
            String provider,
            String providerSubject
    );

    boolean existsByProviderAndProviderSubject(
            String provider,
            String providerSubject
    );

    Optional<UserIdentity> findByUserIdAndProvider(
            UUID userId,
            String provider
    );
}