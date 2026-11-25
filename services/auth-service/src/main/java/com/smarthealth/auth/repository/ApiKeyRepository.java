package com.smarthealth.auth.repository;

import com.smarthealth.auth.model.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {
    Optional<ApiKey> findByKeyHashAndActiveTrue(String keyHash);
    List<ApiKey> findAllByOrderByCreatedAtDesc();
}
