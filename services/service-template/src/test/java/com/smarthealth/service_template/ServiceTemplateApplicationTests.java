package com.smarthealth.service_template;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.cloud.config.enabled=false",
    "spring.config.import="
})
class ServiceTemplateApplicationTests {

	@Test
	void contextLoads() {
	}

}
