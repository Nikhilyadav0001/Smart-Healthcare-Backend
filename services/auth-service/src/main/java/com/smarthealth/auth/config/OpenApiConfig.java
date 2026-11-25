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
import io.swagger.v3.oas.models.servers.Server;
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
                .servers(List.of(
                        new Server().url("/swagger/auth").description("Through Frontend / Gateway Proxy (/swagger/auth)"),
                        new Server().url("/").description("Current Origin / Relative (/)"),
                        new Server().url("https://smarthealthcare.dpdns.org/swagger/auth").description("Public HTTPS (Domain)"),
                        new Server().url("http://localhost:8081").description("Local Direct Service (localhost:8081)")
                ))
                .info(new Info()
                        .title("Smart Healthcare Platform - Auth & Identity Service API")
                        .version("v1.1.0")
                        .description("### Smart Healthcare Authentication & Security Microservice\n\n" +
                                "Comprehensive interactive API documentation covering all identity, authentication, dynamic API key, session, and role management capabilities:\n\n" +
                                "- **1. User Registration & Verification**: Registration (`/auth/register`) with Mailpit email token verification (`/auth/verify-email`) synced via Outbox -> Kafka -> MariaDB.\n" +
                                "- **2. User Session & Login**: REST JSON credentials validation (`/auth/login`) and active session inspection (`/auth/me`).\n" +
                                "- **3. Admin Role Management**: Inspect registered users and reassign security roles (`/auth/admin/users/**`) using Admin Key or Admin JWT.\n" +
                                "- **4. HTTP Basic Auth**: Service-to-Service or credential authentication (`/auth/demo/basic`).\n" +
                                "- **5. Bearer JWT**: Mint an RSA-256 JWT (`/auth/demo/jwt`) first, then verify it on `/auth/demo/bearer-jwt` or `/auth/admin/users`.\n" +
                                "- **6. Dynamic & Predefined API Keys**: Dynamically generate/list/revoke DB-backed API keys (`POST/GET/DELETE /auth/api-keys`), or use Standard (`smarthealth-dev-secret-api-key-2026`) / Admin (`smarthealth-admin-secret-key-2026`) keys via `X-API-KEY` header or single JSON validator (`/auth/demo/api-key`).\n" +
                                "- **7. OAuth 2.0 & OIDC (incl. Google PKCE)**: Authorization Code with PKCE (`postman-oauth2` / `healthapp-bff`), Client Credentials token minting (`/oauth2/token`), and JWKS public keys (`/oauth2/jwks`).")
                        .contact(new Contact().name("Smart Healthcare Security Team").email("security@smarthealth.io"))
                        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0")))
                .tags(List.of(
                        new Tag().name("Authentication & User Session").description("REST Login, session inspection and current user identity"),
                        new Tag().name("User Registration & Email Verification").description("Patient registration, token hashing and email verification"),
                        new Tag().name("Admin User & Role Management").description("Inspect registered users and update security roles (Requires ROLE_ADMIN)"),
                        new Tag().name("API Key Management").description("Dynamically generate, list, and revoke DB-backed API keys alongside the predefined Admin key"),
                        new Tag().name("Authentication Showcase").description("Interactive endpoints demonstrating No Auth, Basic, Bearer JWT mint/verify, unified API Key validation, and OAuth 2.0"),
                        new Tag().name("Service Diagnostics").description("Service liveness and readiness ping endpoints")
                ))
                .components(new Components()
                        // 1. HTTP Basic Authentication
                        .addSecuritySchemes("basicAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("basic")
                                .description("HTTP Basic authentication (e.g. admin@smarthealth.internal / admin123)"))

                        // 2. JWT Bearer Token
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT Bearer token issued by GET /auth/demo/jwt or POST /oauth2/token"))

                        // 3. API Key Header
                        .addSecuritySchemes("apiKeyAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-API-KEY")
                                .description("API Key header authentication. Paste a dynamically generated 'sh_live_...' key from POST /auth/api-keys, or use 'smarthealth-dev-secret-api-key-2026' (client) / 'smarthealth-admin-secret-key-2026' (admin)."))

                        // 4. OAuth 2.0 Flows
                        .addSecuritySchemes("oauth2Auth", new SecurityScheme()
                                .type(SecurityScheme.Type.OAUTH2)
                                .description("OAuth 2.0 Authorization Server flows (Google Login PKCE via client_id 'postman-oauth2', or M2M Client Credentials)")
                                .flows(new OAuthFlows()
                                        .clientCredentials(new OAuthFlow()
                                                .tokenUrl("http://localhost:8081/oauth2/token")
                                                .scopes(new Scopes()
                                                        .addString("prediction.read", "Read prediction models and results")
                                                        .addString("prediction.write", "Execute risk assessments")
                                                        .addString("health.read", "Read medical health records")
                                                        .addString("health.write", "Create/Update health records")
                                                )
                                        )
                                        .authorizationCode(new OAuthFlow()
                                                .authorizationUrl("http://localhost:8081/oauth2/authorize")
                                                .tokenUrl("http://localhost:8081/oauth2/token")
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
