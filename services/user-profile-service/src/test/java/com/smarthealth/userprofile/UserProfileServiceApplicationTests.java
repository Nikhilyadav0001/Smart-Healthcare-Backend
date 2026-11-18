package com.smarthealth.userprofile;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.smarthealth.userprofile.kafka.OutboxPublisher;

@SpringBootTest(properties = {
    "spring.cloud.config.enabled=false",
    "spring.config.import=",
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MariaDBDialect",
    "spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false",
    "eureka.client.enabled=false",
    "spring.cloud.discovery.enabled=false"
})
class UserProfileServiceApplicationTests {

	@MockitoBean
	private DataSource dataSource;

	@MockitoBean
	private Flyway flyway;

	@MockitoBean
	private OutboxPublisher outboxPublisher;

	@Test
	void contextLoads() {
	}
}
