package com.smarthealth.auth.security;

import com.smarthealth.auth.model.AuthenticatedUser;
import com.smarthealth.auth.model.User;
import com.smarthealth.auth.service.AuthUserDetailsService;
import com.smarthealth.auth.service.SocialLoginService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Locale;

@Component
public class SocialLoginAuthenticationSuccessHandler
        extends SavedRequestAwareAuthenticationSuccessHandler {
    private final SocialLoginService socialLoginService;
    private final AuthUserDetailsService authUserDetailsService;
    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    public SocialLoginAuthenticationSuccessHandler(
            SocialLoginService socialLoginService,
            AuthUserDetailsService authUserDetailsService) {
        this.socialLoginService = socialLoginService;
        this.authUserDetailsService = authUserDetailsService;
        setDefaultTargetUrl("/");
        setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
            throws IOException, ServletException {

        if (!(authentication instanceof OAuth2AuthenticationToken oauth2Authentication)) {
            throw new IllegalStateException(
                    "Expected OAuth2AuthenticationToken but received: "
                            + authentication.getClass().getName());
        }

        OAuth2User oauth2User = oauth2Authentication.getPrincipal();

        String provider = oauth2Authentication.getAuthorizedClientRegistrationId()
                .toUpperCase(Locale.ROOT);

        String providerSubject = extractProviderSubject(provider, oauth2User);
        String email = extractEmail(oauth2User);
        boolean emailVerified = extractEmailVerified(oauth2User);

        User user = socialLoginService.loginOrCreate(
                provider,
                providerSubject,
                email,
                emailVerified
        );

        AuthenticatedUser authenticatedUser =
                (AuthenticatedUser) authUserDetailsService.loadUserByUsername(user.getEmail());

        Authentication localAuthentication =
                new UsernamePasswordAuthenticationToken(
                        authenticatedUser,
                        null,
                        authenticatedUser.getAuthorities()
                );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(localAuthentication);
        SecurityContextHolder.setContext(context);

        securityContextRepository.saveContext(context, request, response);

        super.onAuthenticationSuccess(request, response, localAuthentication);
    }

    private String extractProviderSubject(String provider, OAuth2User user) {
        if (user instanceof OidcUser oidcUser) {
            String subject = oidcUser.getSubject();

            if (subject == null || subject.isBlank()) {
                throw new IllegalStateException(
                        "OIDC provider did not return a subject");
            }

            return subject;
        }

        Object id = user.getAttribute("id");

        if (id == null) {
            throw new IllegalStateException(
                    "OAuth2 provider did not return a provider subject");
        }

        return String.valueOf(id);
    }

    private String extractEmail(OAuth2User user) {
        String email = user.getAttribute("email");

        if (email == null || email.isBlank()) {
            throw new IllegalStateException(
                    "OAuth2 provider did not return an email address");
        }

        return email;
    }

    private boolean extractEmailVerified(OAuth2User user) {
        Object verified = user.getAttribute("email_verified");

        if (verified instanceof Boolean booleanValue) {
            return booleanValue;
        }

        if (verified instanceof String stringValue) {
            return Boolean.parseBoolean(stringValue);
        }

        /*
         * GitHubOAuth2UserService explicitly adds email_verified=true
         * after obtaining a verified GitHub email address.
         */
        return false;
    }
}
