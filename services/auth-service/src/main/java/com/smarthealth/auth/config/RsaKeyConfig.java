package com.smarthealth.auth.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

@Configuration
public class RsaKeyConfig {

    private static final Logger log = LoggerFactory.getLogger(RsaKeyConfig.class);
    private static final String REDIS_JWK_KEY = "smarthealth:auth:rsa-jwk:v1";

    @Bean
    public JWKSource<SecurityContext> jwkSource(
            @Value("${AUTH_PRIVATE_KEY_PATH:}") String privateKeyPath,
            @Value("${AUTH_PUBLIC_KEY_PATH:}") String publicKeyPath,
            ObjectProvider<StringRedisTemplate> redisProvider) throws Exception {

        if (privateKeyPath == null || privateKeyPath.isBlank() || !Files.exists(Path.of(privateKeyPath))
                || publicKeyPath == null || publicKeyPath.isBlank() || !Files.exists(Path.of(publicKeyPath))) {

            StringRedisTemplate redis = redisProvider.getIfAvailable();
            if (redis != null) {
                try {
                    String existing = redis.opsForValue().get(REDIS_JWK_KEY);
                    if (existing != null && !existing.isBlank()) {
                        RSAKey sharedKey = RSAKey.parse(existing);
                        log.info("Loaded shared RSA JWK (kid={}) from Redis so all replicas verify tokens consistently.", sharedKey.getKeyID());
                        return new ImmutableJWKSet<>(new JWKSet(sharedKey));
                    }

                    RSAKey generated = generateEphemeralRsaKey();
                    Boolean won = redis.opsForValue().setIfAbsent(REDIS_JWK_KEY, generated.toJSONString());
                    String stored = Boolean.TRUE.equals(won) ? generated.toJSONString() : redis.opsForValue().get(REDIS_JWK_KEY);
                    RSAKey activeKey = (stored != null && !stored.isBlank()) ? RSAKey.parse(stored) : generated;
                    log.info("Initialized shared RSA JWK (kid={}) in Redis across replicas.", activeKey.getKeyID());
                    return new ImmutableJWKSet<>(new JWKSet(activeKey));
                } catch (Exception ex) {
                    log.warn("Could not coordinate RSA JWK via Redis ({}). Falling back to local ephemeral key.", ex.getMessage());
                }
            }

            log.warn("RSA key paths not configured or files not found. Generating ephemeral 2048-bit RSA key pair for development/testing.");
            return new ImmutableJWKSet<>(new JWKSet(generateEphemeralRsaKey()));
        }

        String privatePem = Files.readString(Path.of(privateKeyPath));
        String publicPem = Files.readString(Path.of(publicKeyPath));

        byte[] privateBytes = Base64.getMimeDecoder().decode(
                privatePem
                        .replace("-----BEGIN PRIVATE KEY-----", "")
                        .replace("-----END PRIVATE KEY-----", ""));

        byte[] publicBytes = Base64.getMimeDecoder().decode(
                publicPem
                        .replace("-----BEGIN PUBLIC KEY-----", "")
                        .replace("-----END PUBLIC KEY-----", ""));

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(
                new PKCS8EncodedKeySpec(privateBytes));

        RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(
                new X509EncodedKeySpec(publicBytes));

        String keyId = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(MessageDigest.getInstance("SHA-256")
                        .digest(publicKey.getEncoded()));

        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(keyId)
                .build();

        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    private RSAKey generateEphemeralRsaKey() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
    }
}
