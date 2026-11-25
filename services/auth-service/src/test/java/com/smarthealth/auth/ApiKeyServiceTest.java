package com.smarthealth.auth;

import com.smarthealth.auth.model.ApiKey;
import com.smarthealth.auth.repository.ApiKeyRepository;
import com.smarthealth.auth.service.ApiKeyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ApiKeyServiceTest {

    private ApiKeyRepository repository;
    private ApiKeyService service;
    private Map<String, ApiKey> byHash;
    private Map<UUID, ApiKey> byId;

    @BeforeEach
    void setUp() {
        repository = mock(ApiKeyRepository.class);
        byHash = new HashMap<>();
        byId = new HashMap<>();

        when(repository.save(any(ApiKey.class))).thenAnswer(inv -> {
            ApiKey entity = inv.getArgument(0);
            byHash.put(entity.getKeyHash(), entity);
            byId.put(entity.getId(), entity);
            return entity;
        });

        when(repository.findByKeyHashAndActiveTrue(anyString())).thenAnswer(inv -> {
            ApiKey key = byHash.get(inv.<String>getArgument(0));
            return (key != null && key.isActive()) ? Optional.of(key) : Optional.empty();
        });

        when(repository.findById(any(UUID.class))).thenAnswer(inv ->
                Optional.ofNullable(byId.get(inv.<UUID>getArgument(0))));

        service = new ApiKeyService(repository);
    }

    @Test
    void generateKeyHashesSecretAndValidatesUntilRevoked() {
        ApiKeyService.GeneratedApiKey generated = service.generateKey(
                "mobile-client",
                List.of("USER"),
                "admin@smarthealth.internal"
        );

        assertTrue(generated.plaintextKey().startsWith("sh_live_"));
        assertNotEquals(generated.plaintextKey(), generated.metadata().getKeyHash());
        assertEquals(64, generated.metadata().getKeyHash().length());
        assertTrue(generated.authorities().contains("ROLE_USER"));
        assertTrue(generated.authorities().contains("ROLE_API_CLIENT"));

        Optional<ApiKey> validated = service.validateAndTouch(generated.plaintextKey());
        assertTrue(validated.isPresent());
        assertNotNull(validated.get().getLastUsedAt());

        Optional<ApiKey> revoked = service.revoke(generated.metadata().getId());
        assertTrue(revoked.isPresent());
        assertFalse(revoked.get().isActive());

        assertTrue(service.validateAndTouch(generated.plaintextKey()).isEmpty());
    }
}
