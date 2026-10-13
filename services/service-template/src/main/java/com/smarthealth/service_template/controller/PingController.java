package com.smarthealth.service_template.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/ping")
@Tag(name = "Ping", description = "Template endpoint that proves Swagger works")
public class PingController {

    private static final Logger log = LoggerFactory.getLogger(PingController.class);
    private final String appName;

    public PingController(@Value("${spring.application.name}") String appName) {
        this.appName = appName;
    }

    @Operation(summary = "Returns the service name and status")
    @GetMapping
    public PingResponse ping() {
        log.info("ping received");
        return new PingResponse(appName, "UP");
    }

    public record PingResponse(String service, String status) {}
}