package com.smarthealth.auth.service;

import com.smarthealth.auth.model.ApiKey;
import com.smarthealth.auth.repository.ApiKeyRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ApiKeyService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String KEY_PREFIX = "sh_live_";

    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyService(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    public record GeneratedApiKey(ApiKey metadata, String plaintextKey, List<String> authorities) {}

    @Transactional
    public GeneratedApiKey generateKey(String rawName, List<String> requestedRoles, String createdBy) {
        String name = (rawName == null || rawName.isBlank()) ? "dynamic-client-key" : rawName.trim();
        if (name.length() > 120) {
            name = name.substring(0, 120);
        }

        Set<String> cleanRoles = new LinkedHashSet<>();
        if (requestedRoles != null) {
            for (String r : requestedRoles) {
                if (r == null || r.isBlank()) continue;
                String normalized = r.trim().toUpperCase(Locale.ROOT);
                if (normalized.startsWith("ROLE_")) {
                    normalized = normalized.substring(5);
                }
                if (!normalized.isEmpty()) {
                    cleanRoles.add(normalized);
                }
            }
        }
        if (cleanRoles.isEmpty()) {
            cleanRoles.add("API_CLIENT");
            cleanRoles.add("SERVICE");
        } else {
            cleanRoles.add("API_CLIENT");
        }

        byte[] randomBytes = new byte[24];
        SECURE_RANDOM.nextBytes(randomBytes);
        String plaintextKey = KEY_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        String keyHash = sha256Hex(plaintextKey);
        String displayPrefix = plaintextKey.substring(0, Math.min(16, plaintextKey.length()));

        ApiKey entity = new ApiKey();
        entity.setId(UUID.randomUUID());
        entity.setName(name);
        entity.setKeyPrefix(displayPrefix);
        entity.setKeyHash(keyHash);
        entity.setRoles(String.join(",", cleanRoles));
        entity.setCreatedBy(createdBy);
        entity.setActive(true);
        entity.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));

        ApiKey saved = apiKeyRepository.save(entity);
        List<String> authorities = toRoleStrings(saved.getRoles());
        return new GeneratedApiKey(saved, plaintextKey, authorities);
    }

    @Transactional
    public Optional<ApiKey> validateAndTouch(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            return Optional.empty();
        }
        String hash = sha256Hex(rawKey.trim());
        Optional<ApiKey> found = apiKeyRepository.findByKeyHashAndActiveTrue(hash);
        found.ifPresent(key -> {
            key.setLastUsedAt(OffsetDateTime.now(ZoneOffset.UTC));
            apiKeyRepository.save(key);
        });
        return found;
    }

    @Transactional(readOnly = true)
    public List<ApiKey> listAll() {
        return apiKeyRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public Optional<ApiKey> revoke(UUID id) {
        return apiKeyRepository.findById(id).map(key -> {
            key.setActive(false);
            return apiKeyRepository.save(key);
        });
    }

    public List<GrantedAuthority> toGrantedAuthorities(ApiKey key) {
        return toRoleStrings(key.getRoles()).stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    public List<String> toRoleStrings(String rolesCsv) {
        if (rolesCsv == null || rolesCsv.isBlank()) {
            return List.of("ROLE_API_CLIENT");
        }
        return Arrays.stream(rolesCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.startsWith("ROLE_") ? s : "ROLE_" + s)
                .distinct()
                .toList();
    }

    public static String sha256Hex(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
