package com.smarthealth.adminserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.cloud.config.enabled=false",
    "spring.config.import="
})
class AdminServerApplicationTests {

    @Test
    void contextLoads() {
    }

}
