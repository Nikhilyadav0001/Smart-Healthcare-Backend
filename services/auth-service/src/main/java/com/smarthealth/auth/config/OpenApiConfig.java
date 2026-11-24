package com.smarthealth.auth.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI openAPI(@Value("${spring.application.name}") String appName) {
        return new OpenAPI()
                .info(new Info()
                        .title(appName.toUpperCase() + " - Smart Healthcare Authentication & Security API")
                        .version("v1.0.0")
                        .description("### Smart Healthcare Authentication Gateway\n\n" +
                                "This API demonstrates and supports multiple authentication mechanisms:\n" +
                                "1. **HTTP Basic Auth**: For Service-to-Service client authentication and legacy access (`Authorization: Basic <base64>`)\n" +
                                "2. **Bearer Token (JWT)**: Asymmetric RSA-signed JWT tokens issued by Spring Authorization Server (`Authorization: Bearer <jwt>`)\n" +
                                "3. **API Key**: Header-based authentication using `X-API-KEY: smarthealth-dev-secret-api-key-2026`\n" +
                                "4. **OAuth 2.0 & OIDC**: Authorization Code with PKCE, Client Credentials for microservices, and Refresh Token rotation."))
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
                                .description("API Key header authentication (Header: X-API-KEY)"))

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
