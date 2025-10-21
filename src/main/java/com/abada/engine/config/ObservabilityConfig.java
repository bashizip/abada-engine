package com.abada.engine.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.registry.otlp.OtlpConfig;
import io.micrometer.registry.otlp.OtlpMeterRegistry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * Configuration for OpenTelemetry observability features.
 * Sets up tracing and metrics for the Abada Engine.
 */
@Configuration
@Profile("!test") // Exclude from test profile
@ConditionalOnProperty(name = "management.tracing.enabled", matchIfMissing = true)
public class ObservabilityConfig {

    @Value("${app.version:unknown}")
    private String appVersion;

    @Value("${spring.application.name:abada-engine}")
    private String serviceName;

    @Value("${management.tracing.sampling.probability:1.0}")
    private double samplingProbability;

    @Value("${spring.profiles.active:default}")
    private String environment;

    @Value("${management.otlp.metrics.export.step:10s}")
    private String metricExportInterval;

    @Value("${management.otlp.metrics.endpoint:http://localhost:4318/v1/metrics}")
    private String otlpEndpoint;

    /**
     * Configures the OpenTelemetry SDK with resource attributes and sampling.
     */
    @Bean
    @Primary
    public OpenTelemetry openTelemetry() {
        Resource resource = Resource.getDefault()
            .merge(Resource.create(Attributes.builder()
                .put("service.name", serviceName)
                .put("service.version", appVersion)
                .put("deployment.environment", environment)
                .put("service.namespace", "com.abada")
                .build()));

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
            .setResource(resource)
            .setSampler(Sampler.traceIdRatioBased(samplingProbability))
            .build();

        return OpenTelemetrySdk.builder()
            .setTracerProvider(tracerProvider)
            .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
            .buildAndRegisterGlobal();
    }

    /**
     * Creates a tracer instance for manual instrumentation.
     */
    @Bean
    public Tracer tracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer(serviceName, appVersion);
    }

    /**
     * Configure OTLP meter registry for metrics with appropriate step interval.
     */
    @Bean
    public MeterRegistry meterRegistry() {
        OtlpConfig config = new OtlpConfig() {
            @Override
            public String get(String key) {
                return getConfigValue(key);
            }

            private String getConfigValue(String key) {
                return switch (key) {
                    case "otlp.url" -> otlpEndpoint;
                    case "otlp.step" -> metricExportInterval;
                    default -> null;
                };
            }
        };

        return new OtlpMeterRegistry(config, io.micrometer.core.instrument.Clock.SYSTEM);
    }
}
