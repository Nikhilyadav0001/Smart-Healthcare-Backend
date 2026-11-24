package com.smarthealth.auth.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI openAPI(@Value("${spring.application.name:auth-service}") String appName) {
        return new OpenAPI()
                .info(new Info()
                        .title("Smart Healthcare Platform - Auth & Identity Service API")
                        .version("v1.0.0")
                        .description("### Smart Healthcare Authentication & Security Microservice\n\n" +
                                "Comprehensive interactive API documentation covering all identity, authentication, session, and role management capabilities:\n\n" +
                                "- **1. User Registration & Verification**: Registration (`/auth/register`) with Mailpit email token verification (`/auth/verify-email`).\n" +
                                "- **2. User Session & Login**: REST JSON credentials validation (`/auth/login`) and active session inspection (`/auth/me`).\n" +
                                "- **3. Admin Role Management**: Inspect users and reassign security roles (`/auth/admin/users/**`) using Admin Key or Admin JWT.\n" +
                                "- **4. HTTP Basic Auth**: Service-to-Service communication (`Authorization: Basic <base64>`).\n" +
                                "- **5. Bearer JWT**: Asymmetric RSA-256 signed JSON Web Tokens (`Authorization: Bearer <jwt>`).\n" +
                                "- **6. API Key Auth**: Standard Key (`smarthealth-dev-secret-api-key-2026`) and Admin Key (`smarthealth-admin-secret-key-2026`) via `X-API-KEY` header.\n" +
                                "- **7. OAuth 2.0 & OIDC**: Authorization Code with PKCE, Client Credentials token minting (`/oauth2/token`), and JWKS public keys (`/oauth2/jwks`).")
                        .contact(new Contact().name("Smart Healthcare Security Team").email("security@smarthealth.io"))
                        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0")))
                .tags(List.of(
                        new Tag().name("Authentication & User Session").description("REST Login, session inspection and current user identity"),
                        new Tag().name("User Registration & Email Verification").description("Patient registration, token hashing and email verification"),
                        new Tag().name("Admin User & Role Management").description("Inspect registered users and update security roles (Requires ROLE_ADMIN)"),
                        new Tag().name("Authentication Mechanisms Demo").description("Interactive showcases for Basic, Bearer JWT, API Key and Admin Key"),
                        new Tag().name("Service Diagnostics").description("Service liveness and readiness ping endpoints")
                ))
                .components(new Components()
                        // 1. HTTP Basic Authentication
                        .addSecuritySchemes("basicAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("basic")
                                .description("HTTP Basic authentication (Base64 username:password)"))

                        // 2. JWT Bearer Token
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT Bearer token issued by Authorization Server"))

                        // 3. API Key Header
                        .addSecuritySchemes("apiKeyAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-API-KEY")
                                .description("API Key header authentication. Use 'smarthealth-dev-secret-api-key-2026' for client or 'smarthealth-admin-secret-key-2026' for admin."))

                        // 4. OAuth 2.0 Flows
                        .addSecuritySchemes("oauth2Auth", new SecurityScheme()
                                .type(SecurityScheme.Type.OAUTH2)
                                .description("OAuth 2.0 Authorization Server flows")
                                .flows(new OAuthFlows()
                                        .clientCredentials(new OAuthFlow()
                                                .tokenUrl("/oauth2/token")
                                                .scopes(new Scopes()
                                                        .addString("prediction.read", "Read prediction models and results")
                                                        .addString("prediction.write", "Execute risk assessments")
                                                        .addString("health.read", "Read medical health records")
                                                        .addString("health.write", "Create/Update health records")
                                                )
                                        )
                                        .authorizationCode(new OAuthFlow()
                                                .authorizationUrl("/oauth2/authorize")
                                                .tokenUrl("/oauth2/token")
                                                .scopes(new Scopes()
                                                        .addString("openid", "OpenID Connect user identity")
                                                        .addString("profile", "User profile and role claims")
                                                        .addString("email", "User verified email address")
                                                )
                                        )
                                )
                        )
                );
    }
}
