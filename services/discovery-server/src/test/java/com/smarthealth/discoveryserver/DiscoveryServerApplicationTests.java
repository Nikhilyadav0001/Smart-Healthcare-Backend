package com.smarthealth.discoveryserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.cloud.config.enabled=false",
    "spring.config.import="
})
class DiscoveryServerApplicationTests {

    @Test
    void contextLoads() {
    }

}
