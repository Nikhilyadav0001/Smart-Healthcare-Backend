package com.smarthealth.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.logout.RedirectServerLogoutSuccessHandler;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;

import java.net.URI;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        // Upon successful login with auth-server, redirect the browser to the Vite frontend
        RedirectServerAuthenticationSuccessHandler successHandler =
                new RedirectServerAuthenticationSuccessHandler("http://localhost:5173/dashboard");

        RedirectServerLogoutSuccessHandler logoutSuccessHandler =
                new RedirectServerLogoutSuccessHandler();
        // Redirect to auth-service logout endpoint so the auth-service session is also invalidated
        logoutSuccessHandler.setLogoutSuccessUrl(URI.create("http://localhost:8081/logout?redirect_uri=http://localhost:5173/"));

        return http
                .authorizeExchange(exchange -> exchange
                        .pathMatchers(
                                "/api/auth/**",
                                "/auth/**",
                                "/api/predictions/**",
                                "/api/health/**",
                                "/api/users/**",
                                "/profiles/**",
                                "/dev/tokens",
                                "/actuator/**"
                        ).permitAll()
                        .anyExchange().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .authenticationSuccessHandler(successHandler)
                )
                .logout(logout -> logout
                        .requiresLogout(new PathPatternParserServerWebExchangeMatcher("/logout"))
                        .logoutSuccessHandler(logoutSuccessHandler)
                )
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .build();
    }
}
