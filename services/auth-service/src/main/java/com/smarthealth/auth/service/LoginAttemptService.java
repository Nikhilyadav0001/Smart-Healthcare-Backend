package com.smarthealth.auth.service;

import com.smarthealth.auth.model.User;
import com.smarthealth.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class LoginAttemptService {

    private final UserRepository userRepository;
    private final int maxFailedAttempts;
    private final long lockDurationMinutes;

    public LoginAttemptService(
            UserRepository userRepository,
            @Value("${auth.security.max-failed-attempts:5}") int maxFailedAttempts,
            @Value("${auth.security.lock-duration-minutes:15}") long lockDurationMinutes) {

        this.userRepository = userRepository;
        this.maxFailedAttempts = maxFailedAttempts;
        this.lockDurationMinutes = lockDurationMinutes;
    }

    @Transactional
    public void recordFailure(User user) {

        int attempts = user.getFailedLoginAttempts() + 1;

        user.setFailedLoginAttempts(attempts);

        if (attempts >= maxFailedAttempts) {
            user.setLockedUntil(
                    OffsetDateTime.now().plusMinutes(lockDurationMinutes)
            );
        }

        userRepository.save(user);
    }

    @Transactional
    public void recordSuccess(User user) {

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);

        userRepository.save(user);
    }
}