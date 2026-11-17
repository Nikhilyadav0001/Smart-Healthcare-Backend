package com.smarthealth.auth;

import java.util.TimeZone;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
    "spring.cloud.config.enabled=false",
    "spring.config.import=",
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
    "spring.security.oauth2.client.registration.google.client-id=mock-google-client-id",
    "spring.security.oauth2.client.registration.google.client-secret=mock-google-client-secret",
    "spring.security.oauth2.client.registration.github.client-id=mock-github-client-id",
    "spring.security.oauth2.client.registration.github.client-secret=mock-github-client-secret"
})
class AuthServiceApplicationTests {

	@MockitoBean
	private ClientRegistrationRepository clientRegistrationRepository;

	@MockitoBean
	private RegisteredClientRepository registeredClientRepository;

	@MockitoBean
	private OAuth2AuthorizationService authorizationService;

	@MockitoBean
	private OAuth2AuthorizationConsentService authorizationConsentService;

	@MockitoBean
	private DataSource dataSource;

	@MockitoBean
	private Flyway flyway;

	@BeforeAll
	static void setup() {
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
	}

	@Test
	void contextLoads() {
	}

}
