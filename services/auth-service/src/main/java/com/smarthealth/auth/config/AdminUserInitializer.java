package com.smarthealth.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthealth.auth.dto.UserRegisteredEvent;
import com.smarthealth.auth.model.OutboxEvent;
import com.smarthealth.auth.model.User;
import com.smarthealth.auth.model.UserRole;
import com.smarthealth.auth.model.UserStatus;
import com.smarthealth.auth.repository.OutboxEventRepository;
import com.smarthealth.auth.repository.UserRepository;
import com.smarthealth.auth.repository.UserRoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "spring.flyway.enabled", havingValue = "true", matchIfMissing = true)
public class AdminUserInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminUserInitializer.class);
    private static final UUID DEFAULT_ADMIN_ID = UUID.fromString("a0000000-0000-0000-0000-000000000001");

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final String adminEmail;
    private final String adminPassword;

    public AdminUserInitializer(
            UserRepository userRepository,
            UserRoleRepository userRoleRepository,
            OutboxEventRepository outboxEventRepository,
            PasswordEncoder passwordEncoder,
            ObjectMapper objectMapper,
            PlatformTransactionManager transactionManager,
            @Value("${app.auth.default-admin.email:admin@smarthealth.internal}") String adminEmail,
            @Value("${app.auth.default-admin.password:admin123}") String adminPassword) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            transactionTemplate.executeWithoutResult(status -> seedAdminIfMissing());
        } catch (DataIntegrityViolationException ex) {
            log.info("Default admin user already seeded by another replica: {}", adminEmail);
        }
    }

    private void seedAdminIfMissing() {
        if (userRepository.findByEmailIgnoreCase(adminEmail).isPresent()) {
            return;
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        User admin = new User();
        admin.setId(DEFAULT_ADMIN_ID);
        admin.setEmail(adminEmail.toLowerCase());
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setStatus(UserStatus.ACTIVE.name());
        admin.setEmailVerified(true);
        admin.setFailedLoginAttempts(0);
        admin.setLockedUntil(null);
        admin.setCreatedAt(now);
        admin.setUpdatedAt(now);

        User saved = userRepository.save(admin);

        userRoleRepository.save(new UserRole(new UserRole.UserRoleId(saved.getId(), "USER")));
        userRoleRepository.save(new UserRole(new UserRole.UserRoleId(saved.getId(), "ADMIN")));

        try {
            UserRegisteredEvent event = new UserRegisteredEvent(
                    UUID.randomUUID().toString(),
                    1,
                    now.toString(),
                    saved.getId().toString(),
                    saved.getEmail(),
                    "System Administrator",
                    "ADMIN",
                    ""
            );
            OutboxEvent outboxEvent = new OutboxEvent();
            outboxEvent.setId(UUID.randomUUID());
            outboxEvent.setAggregateType("USER");
            outboxEvent.setAggregateId(saved.getId());
            outboxEvent.setEventType("user.registered");
            outboxEvent.setPayload(objectMapper.writeValueAsString(event));
            outboxEvent.setStatus("PENDING");
            outboxEvent.setAttempts(0);
            outboxEvent.setAvailableAt(now);
            outboxEvent.setCreatedAt(now);
            outboxEventRepository.save(outboxEvent);
        } catch (Exception ex) {
            log.warn("Failed to enqueue user.registered outbox event for default admin: {}", ex.getMessage());
        }

        log.info("Seeded default admin account: {} (id={})", saved.getEmail(), saved.getId());
    }
}
