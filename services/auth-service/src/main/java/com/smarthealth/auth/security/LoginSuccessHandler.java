package com.smarthealth.auth.security;

import com.smarthealth.auth.model.AuthenticatedUser;
import com.smarthealth.auth.model.User;
import com.smarthealth.auth.repository.UserRepository;
import com.smarthealth.auth.service.LoginAttemptService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class LoginSuccessHandler
        extends SavedRequestAwareAuthenticationSuccessHandler {

    private final LoginAttemptService loginAttemptService;
    private final UserRepository userRepository;

    public LoginSuccessHandler(
            LoginAttemptService loginAttemptService,
            UserRepository userRepository) {

        this.loginAttemptService = loginAttemptService;
        this.userRepository = userRepository;
        setDefaultTargetUrl("http://localhost:5173/");
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
            throws IOException, ServletException {

        if (authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser) {

            User user = userRepository
                    .findByEmailIgnoreCase(authenticatedUser.getEmail())
                    .orElseThrow(() ->
                            new IllegalStateException(
                                     "Authenticated user no longer exists: "
                                            + authenticatedUser.getEmail()));

            loginAttemptService.recordSuccess(user);
        }

        super.onAuthenticationSuccess(request, response, authentication);
    }
}
