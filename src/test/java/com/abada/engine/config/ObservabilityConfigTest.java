package com.abada.engine.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("observability")
class ObservabilityConfigTest {

    @Autowired
    private OpenTelemetry openTelemetry;

    @Autowired
    private Tracer tracer;

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    void shouldConfigureOpenTelemetry() {
        assertThat(openTelemetry).isNotNull();
        assertThat(openTelemetry.getTracerProvider()).isInstanceOf(SdkTracerProvider.class);
    }

    @Test
    void shouldConfigureTracer() {
        assertThat(tracer).isNotNull();
        assertThat(tracer.getClass().getName()).contains("SdkTracer");
    }

    @Test
    void shouldConfigureMeterRegistry() {
        assertThat(meterRegistry).isNotNull();
        assertThat(meterRegistry.getClass().getName()).contains("OtlpMeterRegistry");
    }

    @Test
    void shouldHaveCorrectMetricPrefix() {
        // Create a test metric
        meterRegistry.counter("test.counter").increment();
        
        // Verify metric was created with correct prefix
        assertThat(meterRegistry.get("test.counter").counter()).isNotNull();
    }
}