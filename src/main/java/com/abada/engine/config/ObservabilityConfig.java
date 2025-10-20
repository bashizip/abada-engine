package com.abada.engine.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.registry.otlp.OtlpConfig;
import io.micrometer.registry.otlp.OtlpMeterRegistry;
import io.micrometer.tracing.Tracer;
import io.opentelemetry.api.trace.Tracer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration for OpenTelemetry observability features.
 * Sets up tracing, metrics, and logging correlation for the Abada Engine.
 */
@Configuration
public class ObservabilityConfig {

    @Value("${app.version:unknown}")
    private String appVersion;

    @Value("${spring.application.name:abada-engine}")
    private String serviceName;

    @Value("${management.otlp.tracing.endpoint:http://localhost:4318/v1/traces}")
    private String otlpTracingEndpoint;

    @Value("${management.otlp.metrics.endpoint:http://localhost:4318/v1/metrics}")
    private String otlpMetricsEndpoint;

    @Value("${management.tracing.sampling.probability:1.0}")
    private double samplingProbability;

    @Value("${spring.profiles.active:default}")
    private String environment;

    // OpenTelemetry SDK is auto-configured by Spring Boot

    /**
     * Configure OTLP meter registry for metrics export.
     */
    @Bean
    public OtlpMeterRegistry otlpMeterRegistry() {
        OtlpConfig otlpConfig = new OtlpConfig() {
            @Override
            public String url() {
                return otlpMetricsEndpoint;
            }

            @Override
            public Duration step() {
                return Duration.ofSeconds(10);
            }

            @Override
            public String get(String key) {
                return null;
            }
        };

        return new OtlpMeterRegistry(otlpConfig, io.micrometer.core.instrument.Clock.SYSTEM);
    }

    // Tracers are auto-configured by Spring Boot
}
