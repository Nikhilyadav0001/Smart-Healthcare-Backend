package com.smarthealth.observability;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(OpenTelemetryAppender.class)
public class ObservabilityAutoConfiguration {

    @Bean
    InstallOpenTelemetryAppender installOpenTelemetryAppender(OpenTelemetry openTelemetry) {
        return new InstallOpenTelemetryAppender(openTelemetry);
    }

    static class InstallOpenTelemetryAppender implements InitializingBean {

        private final OpenTelemetry openTelemetry;

        InstallOpenTelemetryAppender(OpenTelemetry openTelemetry) {
            this.openTelemetry = openTelemetry;
        }

        @Override
        public void afterPropertiesSet() {
            OpenTelemetryAppender.install(this.openTelemetry);
        }
    }
}