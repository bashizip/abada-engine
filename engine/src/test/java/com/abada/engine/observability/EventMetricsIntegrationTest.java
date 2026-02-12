package com.abada.engine.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EventMetricsIntegrationTest {

    private MeterRegistry registry;
    private EngineMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new EngineMetrics(registry);
    }

    @Test
    void shouldRecordCompleteEventLifecycle() {
        String eventType = "MESSAGE";
        String eventName = "payment-received";

        // Test event publication
        metrics.recordEventPublished(eventType, eventName);
        assertThat(getEventCounter("published", eventType, eventName).count()).isEqualTo(1.0);

        // Test event consumption
        metrics.recordEventConsumed(eventType, eventName);
        assertThat(getEventCounter("consumed", eventType, eventName).count()).isEqualTo(1.0);

        // Test event correlation
        metrics.recordEventCorrelated(eventType, eventName);
        assertThat(getEventCounter("correlated", eventType, eventName).count()).isEqualTo(1.0);

        // Test event processing latency
        Timer.Sample sample = metrics.startEventProcessingTimer();
        try {
            Thread.sleep(10); // Simulate processing time
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        metrics.recordEventProcessingLatency(sample, eventType, eventName);
        assertThat(getEventProcessingLatencyTimer(eventType, eventName).count()).isEqualTo(1L);

        // Test event queue size management
        assertThat(metrics.getEventQueueSize()).isEqualTo(0.0);
        metrics.incrementEventQueueSize();
        assertThat(metrics.getEventQueueSize()).isEqualTo(1.0);
        metrics.decrementEventQueueSize();
        assertThat(metrics.getEventQueueSize()).isEqualTo(0.0);
    }

    @Test
    void shouldTrackEventQueueSizeGauge() {
        // Verify gauge is registered
        Gauge gauge = registry.get("abada.events.queue_size").gauge();
        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isEqualTo(0.0);

        // Test queue size changes
        metrics.incrementEventQueueSize();
        assertThat(gauge.value()).isEqualTo(1.0);

        metrics.incrementEventQueueSize();
        assertThat(gauge.value()).isEqualTo(2.0);

        metrics.decrementEventQueueSize();
        assertThat(gauge.value()).isEqualTo(1.0);
    }

    @Test
    void shouldTrackEventProcessingLatency() {
        String eventType = "SIGNAL";
        String eventName = "order-completed";

        // Test processing latency recording first to create the timer
        Timer.Sample sample = metrics.startEventProcessingTimer();
        try {
            Thread.sleep(50); // Simulate processing time
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        metrics.recordEventProcessingLatency(sample, eventType, eventName);

        // Now verify the timer was created and has the correct values
        Timer timer = getEventProcessingLatencyTimer(eventType, eventName);
        assertThat(timer.count()).isEqualTo(1L);
        assertThat(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)).isGreaterThan(40.0);
    }

    private Counter getEventCounter(String type, String eventType, String eventName) {
        return registry.get("abada.events." + type)
                     .tag("event.type", eventType)
                     .tag("event.name", eventName)
                     .counter();
    }

    private Timer getEventProcessingLatencyTimer(String eventType, String eventName) {
        return registry.get("abada.event.processing_latency")
                     .tag("event.type", eventType)
                     .tag("event.name", eventName)
                     .timer();
    }
}
