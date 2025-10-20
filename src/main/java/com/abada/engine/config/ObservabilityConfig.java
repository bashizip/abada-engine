package com.abada.engine.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.registry.otlp.OtlpConfig;
import io.micrometer.registry.otlp.OtlpMeterRegistry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.semconv.ResourceAttributes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

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

    /**
     * Configures the OpenTelemetry SDK with custom resource attributes and span processors.
     */
    @Bean
    @Primary
    public OpenTelemetry openTelemetry() {
        Resource resource = Resource.getDefault()
            .merge(Resource.create(Attributes.builder()
                .put(ResourceAttributes.SERVICE_NAME, serviceName)
                .put(ResourceAttributes.SERVICE_VERSION, appVersion)
                .put(ResourceAttributes.DEPLOYMENT_ENVIRONMENT, environment)
                .build()));

        OtlpGrpcSpanExporter spanExporter = OtlpGrpcSpanExporter.builder()
            .setEndpoint(otlpTracingEndpoint)
            .setTimeout(5, TimeUnit.SECONDS)
            .build();

        SpanProcessor spanProcessor = BatchSpanProcessor.builder(spanExporter)
            .setMaxQueueSize(2048)
            .setMaxExportBatchSize(512)
            .setExporterTimeout(Duration.ofSeconds(30))
            .build();

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
            .setResource(resource)
            .setSampler(Sampler.traceIdRatioBased(samplingProbability))
            .addSpanProcessor(spanProcessor)
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
    
    /**
     * Configures a composite meter registry that includes the OTLP registry.
     */
    @Bean
    @Primary
    public MeterRegistry meterRegistry(OtlpMeterRegistry otlpMeterRegistry) {
        return otlpMeterRegistry;
    }

    // Tracers are auto-configured by Spring Boot

}
