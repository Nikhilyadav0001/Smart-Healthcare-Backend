package com.smarthealth.userprofile;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
    "spring.cloud.config.enabled=false",
    "spring.config.import=",
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MariaDBDialect"
})
class UserProfileServiceApplicationTests {

	@MockitoBean
	private DataSource dataSource;

	@MockitoBean
	private Flyway flyway;

	@Test
	void contextLoads() {
	}
}
