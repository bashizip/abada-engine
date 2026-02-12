package com.abada.engine.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles({"test", "observability"})
public class ObservabilitySetupTest {

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private OpenTelemetry openTelemetry;

    @Autowired
    private EngineMetrics engineMetrics;

    @Test
    void shouldHaveCorrectMetricNames() {
        // Create test metrics
        engineMetrics.recordProcessStarted("test-process");
        engineMetrics.recordTaskCreated("test-task");

        // Verify process metrics
        assertThat(meterRegistry.find("abada.process.instances.started")
                .tag("process.definition.id", "test-process")
                .counter())
                .isNotNull();

        // Verify task metrics
        assertThat(meterRegistry.find("abada.tasks.created")
                .tag("task.definition.key", "test-task")
                .counter())
                .isNotNull();
    }

    @Test
    void shouldHaveCorrectOpenTelemetrySetup() {
        assertThat(openTelemetry).isNotNull();
        assertThat(openTelemetry.getTracerProvider()).isNotNull();
        
        // Create a test span to verify tracer functionality
        Tracer tracer = openTelemetry.getTracer("test");
        assertThat(tracer).isNotNull();
    }
}