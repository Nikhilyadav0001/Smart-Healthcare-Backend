package com.smarthealth.auth.security;

import com.smarthealth.auth.model.AuthenticatedUser;
import com.smarthealth.auth.model.User;
import com.smarthealth.auth.service.LoginAttemptService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final LoginAttemptService loginAttemptService;

    public LoginSuccessHandler(LoginAttemptService loginAttemptService) {
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
            throws IOException, ServletException {

        if (authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser) {
            User user = authenticatedUser.getUser();
            loginAttemptService.recordSuccess(user);
        }

        super.onAuthenticationSuccess(request, response, authentication);
    }
}