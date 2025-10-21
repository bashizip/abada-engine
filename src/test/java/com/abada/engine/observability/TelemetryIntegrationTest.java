package com.abada.engine.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("telemetry") // Use telemetry profile for integration tests
public class TelemetryIntegrationTest {

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private OpenTelemetry openTelemetry;

    @Autowired
    private Tracer tracer;

    @Test
    void shouldCaptureBasicTelemetry() {
        // Create a test metric
        meterRegistry.counter("test.counter").increment();
        
        // Create a test span
        Span span = tracer.spanBuilder("test.operation").startSpan();
        span.end();

        // Verify metric was recorded
        assertThat(meterRegistry.get("test.counter").counter())
            .isNotNull()
            .extracting("count")
            .isEqualTo(1.0);

        // Verify OpenTelemetry is configured
        assertThat(openTelemetry.getTracerProvider()).isNotNull();
        assertThat(tracer).isNotNull();
    }
}