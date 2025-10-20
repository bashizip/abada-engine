package com.abada.engine.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class EngineMetricsCleanupTest {

    private MeterRegistry registry;
    private EngineMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new EngineMetrics(registry);
    }

    @Test
    void shouldCleanupUnusedProcessMetrics() {
        // Create some process metrics
        metrics.recordProcessStarted("process1");
        metrics.recordProcessStarted("process2");
        metrics.recordProcessStarted("process3");
        
        // Record activity for process1 and process2
        metrics.recordProcessCompleted("process1");
        metrics.recordProcessFailed("process2");
        
        // Process3 has no activity after start
        
        // Trigger cleanup
        metrics.cleanupStaleMetrics(60);

        // Verify process3 metrics were cleaned up
        Counter process1Counter = registry.get("abada.process.instances.started")
                                       .tag("process.definition.id", "process1")
                                       .counter();
        Counter process3Counter = registry.get("abada.process.instances.started")
                                       .tag("process.definition.id", "process3")
                                       .counter();

        assertThat(process1Counter).isNotNull();
        assertThat(process3Counter).isNull();
    }

    @Test
    void shouldCleanupUnusedTaskMetrics() {
        // Create some task metrics
        metrics.recordTaskCreated("task1");
        metrics.recordTaskCreated("task2");
        
        // Record activity for task1
        Timer.Sample sample = metrics.startTaskProcessingTimer();
        metrics.recordTaskCompleted("task1");
        metrics.recordTaskProcessingTime(sample, "task1");
        
        // task2 has no activity after creation
        
        // Trigger cleanup
        metrics.cleanupStaleMetrics(60);

        // Verify task2 metrics were cleaned up
        Counter task1Counter = registry.get("abada.tasks.created")
                                    .tag("task.definition.key", "task1")
                                    .counter();
        Counter task2Counter = registry.get("abada.tasks.created")
                                    .tag("task.definition.key", "task2")
                                    .counter();

        assertThat(task1Counter).isNotNull();
        assertThat(task2Counter).isNull();
    }

    @Test
    void shouldHandleLargeNumberOfMetrics() {
        // Create 1500 metrics (above the 1000 threshold)
        for (int i = 0; i < 1500; i++) {
            String processId = "process" + i;
            metrics.recordProcessStarted(processId);
            if (i % 2 == 0) {
                // Add some activity to half of the processes
                metrics.recordProcessCompleted(processId);
            }
        }

        // Trigger cleanup
        metrics.cleanupStaleMetrics(60);

        // Verify that the number of metrics has been reduced
        long remainingMetrics = registry.get("abada.process.instances.started")
                                      .counters()
                                      .size();
        
        // Should have removed ~20% of metrics with no activity
        assertThat(remainingMetrics).isLessThan(1500);
    }

    @Test
    void shouldKeepActiveMetrics() {
        String processId = "activeProcess";
        Timer.Sample sample = metrics.startProcessTimer();
        
        // Create and continuously update metrics
        for (int i = 0; i < 5; i++) {
            metrics.recordProcessStarted(processId);
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        metrics.recordProcessDuration(sample, processId);

        // Trigger cleanup
        metrics.cleanupStaleMetrics(60);

        // Verify active metrics are retained
        Counter processCounter = registry.get("abada.process.instances.started")
                                      .tag("process.definition.id", processId)
                                      .counter();
        Timer processTimer = registry.get("abada.process.duration")
                                   .tag("process.definition.id", processId)
                                   .timer();

        assertThat(processCounter.count()).isEqualTo(5.0);
        assertThat(processTimer.count()).isEqualTo(1L);
    }
}