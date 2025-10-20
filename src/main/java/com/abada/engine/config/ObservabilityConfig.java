package com.abada.engine.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration for OpenTelemetry observability features.
 * Sets up tracing, metrics, and logging correlation for the Abada Engine.
 * Currently using simple meter registry for metrics.
 */
@Configuration
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

    /**
     * Configures the OpenTelemetry SDK with custom resource attributes.
     */
    @Bean
    @Primary
    public OpenTelemetry openTelemetry() {
        Resource resource = Resource.getDefault()
            .merge(Resource.create(Attributes.builder()
                .put("service.name", serviceName)
                .put("service.version", appVersion)
                .put("deployment.environment", environment)
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
     * Configure simple meter registry for metrics.
     */
    @Bean
    public MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }
}
