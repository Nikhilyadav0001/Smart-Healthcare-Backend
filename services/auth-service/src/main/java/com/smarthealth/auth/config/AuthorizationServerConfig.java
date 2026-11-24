package com.smarthealth.auth.config;

import com.smarthealth.auth.model.User;
import com.smarthealth.auth.model.AuthenticatedUser;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import org.springframework.security.jackson.SecurityJacksonModules;

import com.smarthealth.auth.security.ApiKeyAuthenticationFilter;
import com.smarthealth.auth.security.GitHubOAuth2UserService;
import com.smarthealth.auth.security.LoginFailureHandler;
import com.smarthealth.auth.security.LoginSuccessHandler;
import com.smarthealth.auth.security.SocialLoginAuthenticationSuccessHandler;
import com.smarthealth.auth.service.AuthUserDetailsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpStatus;

import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.jwk.source.JWKSource;

import java.time.Duration;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.ArrayList;

@Configuration
public class AuthorizationServerConfig {

    @Bean
    @org.springframework.core.annotation.Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(
            HttpSecurity http) throws Exception {

        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                new OAuth2AuthorizationServerConfigurer();

        http
                .securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
                .with(authorizationServerConfigurer, authorizationServer ->
                        authorizationServer
                                .oidc(Customizer.withDefaults())
                )
                .authorizeHttpRequests(authorize ->
                        authorize.anyRequest().authenticated()
                )
                .exceptionHandling(exceptions ->
                        exceptions.authenticationEntryPoint(
                                new LoginUrlAuthenticationEntryPoint("/login")
                        )
                )
                .oauth2ResourceServer(resourceServer ->
                        resourceServer.jwt(Customizer.withDefaults())
                );

        return http.build();
    }

    @Bean
    @org.springframework.core.annotation.Order(2)
    public SecurityFilterChain applicationSecurityFilterChain(
            HttpSecurity http,
            AuthenticationProvider authenticationProvider,
            LoginSuccessHandler loginSuccessHandler,
            LoginFailureHandler loginFailureHandler,
            GitHubOAuth2UserService gitHubOAuth2UserService,
            SocialLoginAuthenticationSuccessHandler socialLoginAuthenticationSuccessHandler,
            ApiKeyAuthenticationFilter apiKeyAuthenticationFilter)
            throws Exception {

        http
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(apiKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/login",
                                "/logout",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/favicon.ico",
                                "/oauth2/authorization/**",
                                "/login/oauth2/code/**",
                                "/actuator/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/auth/register",
                                "/auth/verify-email",
                                "/auth/demo/public",
                                "/auth/demo/oauth-info",
                                "/auth/me",
                                "/me"
                        ).permitAll()
                        .requestMatchers("/auth/demo/api-key").authenticated()
                        .requestMatchers("/auth/demo/basic").authenticated()
                        .requestMatchers("/auth/demo/bearer-jwt").authenticated()
                        .requestMatchers("/auth/demo/service-to-service").authenticated()
                        .requestMatchers("/auth/**").permitAll()
                        .anyRequest().authenticated()
                )
                .csrf(csrf ->
                        csrf.ignoringRequestMatchers(
                                "/auth/**",
                                "/logout"
                        )
                )
                .httpBasic(Customizer.withDefaults())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .exceptionHandling(exceptions -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                request -> request.getRequestURI() != null && request.getRequestURI().startsWith("/auth/demo")
                        )
                )
                .formLogin(form ->
                        form
                                .loginPage("/login")
                                .successHandler(loginSuccessHandler)
                                .failureHandler(loginFailureHandler)
                                .permitAll()
                )
                .oauth2Login(oauth2 ->
                        oauth2
                                .loginPage("/login")
                                .userInfoEndpoint(userInfo ->
                                        userInfo
                                                .userService(gitHubOAuth2UserService)
                                                .oidcUserService(new OidcUserService())
                                )
                                .successHandler(
                                        socialLoginAuthenticationSuccessHandler
                                )
                                .failureHandler((request, response, exception) -> {
                                    exception.printStackTrace();
                                    response.sendRedirect("/login?error");
                                })
                )
                .logout(logout -> logout
                        .logoutRequestMatcher(request -> "/logout".equals(request.getRequestURI()))
                        .logoutSuccessHandler((request, response, authentication) -> {
                            String redirectUri = request.getParameter("redirect_uri");
                            if (redirectUri != null && (redirectUri.startsWith("http://localhost:5173") || redirectUri.startsWith("http://localhost:8080"))) {
                                response.sendRedirect(redirectUri);
                            } else {
                                response.sendRedirect("http://localhost:5173/");
                            }
                        })
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("AUTH_SESSION")
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider(
            AuthUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);

        provider.setPasswordEncoder(passwordEncoder);

        return provider;
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository(
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder,
            @Value("${BFF_CLIENT_SECRET:nikhil}") String bffClientSecret) {

        JdbcRegisteredClientRepository repository =
                new JdbcRegisteredClientRepository(jdbcTemplate);

        RegisteredClient existingClient =
                repository.findByClientId("healthapp-bff");

        if (existingClient == null) {

            RegisteredClient bffClient =
                    RegisteredClient.withId(UUID.randomUUID().toString())
                            .clientId("healthapp-bff")
                            .clientSecret(passwordEncoder.encode(bffClientSecret))
                            .clientName("Smart Healthcare BFF")
                            .clientAuthenticationMethod(
                                    ClientAuthenticationMethod.CLIENT_SECRET_BASIC
                            )
                            .authorizationGrantType(
                                    AuthorizationGrantType.AUTHORIZATION_CODE
                            )
                            .authorizationGrantType(
                                    AuthorizationGrantType.REFRESH_TOKEN
                            )
                            .redirectUri(
                                    "http://localhost:8080/login/oauth2/code/healthapp"
                            )
                            .postLogoutRedirectUri(
                                    "http://localhost:5173"
                            )
                            .scope(OidcScopes.OPENID)
                            .scope(OidcScopes.PROFILE)
                            .scope(OidcScopes.EMAIL)
                            .clientSettings(
                                    ClientSettings.builder()
                                             .requireProofKey(true)
                                            .requireAuthorizationConsent(false)
                                            .build()
                            )
                            .tokenSettings(
                                    TokenSettings.builder()
                                             .accessTokenTimeToLive(
                                                    Duration.ofMinutes(15)
                                            )
                                            .refreshTokenTimeToLive(
                                                    Duration.ofDays(7)
                                            )
                                            .reuseRefreshTokens(false)
                                            .build()
                            )
                            .build();
            repository.save(bffClient);

        } else {
            RegisteredClient synchronizedClient =
                    RegisteredClient.from(existingClient)
                            .clientSecret(
                                    passwordEncoder.encode(bffClientSecret)
                            )
                            .build();

            repository.save(synchronizedClient);
        }

        // Seed prediction-service client credentials
        RegisteredClient existingPrediction = repository.findByClientId("prediction-service");
        if (existingPrediction == null) {
            RegisteredClient predictionClient =
                    RegisteredClient.withId(UUID.randomUUID().toString())
                            .clientId("prediction-service")
                            .clientSecret(passwordEncoder.encode("nikhil"))
                            .clientName("Prediction Service")
                            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                            .scope("profile.read")
                            .scope("health.read")
                            .scope("prediction.read")
                            .scope("prediction.write")
                            .tokenSettings(
                                    TokenSettings.builder()
                                            .accessTokenTimeToLive(Duration.ofMinutes(60))
                                            .build()
                            )
                            .build();
            repository.save(predictionClient);
        }

        // Seed health-data-service client credentials
        RegisteredClient existingHealthData = repository.findByClientId("health-data-service");
        if (existingHealthData == null) {
            RegisteredClient healthDataClient =
                    RegisteredClient.withId(UUID.randomUUID().toString())
                            .clientId("health-data-service")
                            .clientSecret(passwordEncoder.encode("nikhil"))
                            .clientName("Health Data Service")
                            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                            .scope("health.read")
                            .scope("health.write")
                            .tokenSettings(
                                    TokenSettings.builder()
                                            .accessTokenTimeToLive(Duration.ofMinutes(60))
                                            .build()
                            )
                            .build();
            repository.save(healthDataClient);
        }

        return repository;
    }

    @Bean
    public OAuth2AuthorizationService authorizationService(
            JdbcTemplate jdbcTemplate,
            RegisteredClientRepository registeredClientRepository) {

        ClassLoader classLoader = getClass().getClassLoader();

        BasicPolymorphicTypeValidator.Builder typeValidator =
                BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType("java.util.")
                        .allowIfSubType(AuthenticatedUser.class)
                        .allowIfSubType(User.class);

        JsonMapper jsonMapper =
                JsonMapper.builder()
                        .addModules(
                                SecurityJacksonModules.getModules(
                                        classLoader,
                                        typeValidator
                                )
                        )
                        .build();

        JdbcOAuth2AuthorizationService authorizationService =
                new JdbcOAuth2AuthorizationService(
                        jdbcTemplate,
                        registeredClientRepository
                );

        authorizationService.setAuthorizationRowMapper(
                new JdbcOAuth2AuthorizationService
                        .JsonMapperOAuth2AuthorizationRowMapper(
                        registeredClientRepository,
                        jsonMapper
                )
        );

        authorizationService.setAuthorizationParametersMapper(
                new JdbcOAuth2AuthorizationService
                        .JsonMapperOAuth2AuthorizationParametersMapper(
                        jsonMapper
                )
        );

        return new com.smarthealth.auth.security.ReuseDetectionOAuth2AuthorizationService(
                authorizationService,
                jdbcTemplate
        );
    }

    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService(
            JdbcTemplate jdbcTemplate,
            RegisteredClientRepository registeredClientRepository) {

        return new JdbcOAuth2AuthorizationConsentService(
                jdbcTemplate,
                registeredClientRepository
        );
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings(
            @Value("${auth.server.issuer:}") String issuer) {
        AuthorizationServerSettings.Builder builder = AuthorizationServerSettings.builder();
        if (org.springframework.util.StringUtils.hasText(issuer)) {
            builder.issuer(issuer);
        }
        return builder.build();
    }

    @Bean
    public JwtDecoder jwtDecoder(
            JWKSource<SecurityContext> jwkSource) {

        return OAuth2AuthorizationServerConfiguration.jwtDecoder(
                jwkSource
        );
    }

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer() {

        return context -> {

            String email = context.getPrincipal().getName();

            if (OAuth2TokenType.ACCESS_TOKEN.equals(
                    context.getTokenType())) {

                context.getClaims().claim(
                        "roles",
                        context.getPrincipal()
                                .getAuthorities()
                                .stream()
                                .map(authority -> authority.getAuthority())
                                .filter(authority ->
                                        authority.startsWith("ROLE_"))
                                .map(authority ->
                                        authority.substring(5))
                                .distinct()
                                .collect(Collectors.toCollection(ArrayList::new))
                );

                context.getClaims().claim("email", email);
                context.getClaims().claim("jti", UUID.randomUUID().toString());

                Object principal = context.getPrincipal().getPrincipal();
                if (principal instanceof AuthenticatedUser authUser) {
                    context.getClaims().claim("uid", authUser.getUserId().toString());
                }
            }

            if (OidcParameterNames.ID_TOKEN.equals(
                    context.getTokenType().getValue())) {

                context.getClaims().claim("email", email);

                Object principal = context.getPrincipal().getPrincipal();
                if (principal instanceof AuthenticatedUser authUser) {
                    context.getClaims().claim("uid", authUser.getUserId().toString());
                }
            }
        };
    }
}
