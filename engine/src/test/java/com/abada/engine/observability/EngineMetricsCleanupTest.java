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
        // Create and complete process1 to keep metrics
        metrics.recordProcessStarted("process1");
        Timer.Sample sample1 = metrics.startProcessTimer();
        metrics.recordProcessCompleted("process1");
        metrics.recordProcessDuration(sample1, "process1");

        // Create and fail process2 to keep metrics
        metrics.recordProcessStarted("process2");
        metrics.recordProcessFailed("process2");
        
        // Record current metric counts for verification
        double process1CompletedCount = registry.get("abada.process.instances.completed")
                                             .tag("process.definition.id", "process1")
                                             .counter()
                                             .count();
        double process2FailedCount = registry.get("abada.process.instances.failed")
                                          .tag("process.definition.id", "process2")
                                          .counter()
                                          .count();
                                          
        // Create process3 but don't complete it
        metrics.recordProcessStarted("process3");

        // Verify initial state
        assertThat(process1CompletedCount).isEqualTo(1.0);
        assertThat(process2FailedCount).isEqualTo(1.0);
        assertThat(metrics.getActiveProcessInstances()).isGreaterThan(0);

        // Trigger cleanup
        metrics.cleanupStaleMetrics(60);

        // Verify process3 is no longer tracked in active instances
        assertThat(metrics.getActiveProcessInstances()).isEqualTo(0);

        // Verify completed process metrics are retained
        assertThat(registry.get("abada.process.instances.completed")
                        .tag("process.definition.id", "process1")
                        .counter()
                        .count()).isEqualTo(1.0);
        assertThat(registry.get("abada.process.instances.failed")
                        .tag("process.definition.id", "process2")
                        .counter()
                        .count()).isEqualTo(1.0);
    }

    @Test
    void shouldCleanupUnusedTaskMetrics() {
        // Create and complete task1
        metrics.recordTaskCreated("task1");
        Timer.Sample sample = metrics.startTaskProcessingTimer();
        metrics.recordTaskCompleted("task1");
        metrics.recordTaskProcessingTime(sample, "task1");

        // Create and claim task2
        metrics.recordTaskCreated("task2");
        metrics.recordTaskClaimed("task2");

        // Record current metric counts
        double task1CompletedCount = registry.get("abada.tasks.completed")
                                        .tag("task.definition.key", "task1")
                                        .counter()
                                        .count();
        double task2ClaimedCount = registry.get("abada.tasks.claimed")
                                        .tag("task.definition.key", "task2")
                                        .counter()
                                        .count();

        // Create task3 but don't complete it
        metrics.recordTaskCreated("task3");

        // Verify initial state
        assertThat(task1CompletedCount).isEqualTo(1.0);
        assertThat(task2ClaimedCount).isEqualTo(1.0);
        assertThat(metrics.getActiveTasks()).isGreaterThan(0);

        // Trigger cleanup
        metrics.cleanupStaleMetrics(60);

        // Verify task3 is no longer tracked in active tasks
        assertThat(metrics.getActiveTasks()).isEqualTo(0);

        // Verify completed and claimed task metrics are retained
        assertThat(registry.get("abada.tasks.completed")
                        .tag("task.definition.key", "task1")
                        .counter()
                        .count()).isEqualTo(1.0);
        assertThat(registry.get("abada.tasks.claimed")
                        .tag("task.definition.key", "task2")
                        .counter()
                        .count()).isEqualTo(1.0);
    }

    @Test
    void shouldHandleLargeNumberOfMetrics() {
        // Start with a clean state
        metrics.cleanupStaleMetrics(0);
        
        // Create fewer processes but with all metric types to stay under limit
        // Each process creates multiple metrics (started, completed/failed, duration)
        for (int i = 0; i < 200; i++) {
            String processId = "process" + i;
            metrics.recordProcessStarted(processId);
            
            // Complete even-numbered processes
            if (i % 2 == 0) {
                metrics.recordProcessCompleted(processId);
            }
        }
        
        // Verify initial state
        assertThat(metrics.getActiveProcessInstances())
            .as("Before cleanup: Number of active processes")
            .isEqualTo(100);  // Odd-numbered processes are still active
        
        // Trigger cleanup
        metrics.cleanupStaleMetrics(60);
        
        // Verify cleanup effects
        assertThat(metrics.getActiveProcessInstances())
            .as("After cleanup: Active processes should be cleaned up")
            .isEqualTo(0);
            
        // Verify metric counts
        // Check specific process metrics exist for even processes
        for (int i = 0; i < 10; i += 2) {  // Check first few even processes
            String processId = "process" + i;
            Counter completedCounter = registry.get("abada.process.instances.completed")
                                            .tag("process.definition.id", processId)
                                            .counter();
            assertThat(completedCounter)
                .as("Process " + processId + " completion metric")
                .isNotNull();
            assertThat(completedCounter.count())
                .as("Process " + processId + " completion count")
                .isEqualTo(1.0);
        }

        // Verify odd-numbered processes don't have completion metrics
        for (int i = 1; i < 10; i += 2) {  // Check first few odd processes
            String processId = "process" + i;
            Counter completedCounter = registry.find("abada.process.instances.completed")
                                            .tag("process.definition.id", processId)
                                            .counter();
            assertThat(completedCounter)
                .as("Process " + processId + " should not have completion metric")
                .isNull();
        }
            
        // Verify metric size limits
        long totalMetricCount = registry.getMeters().size();
        assertThat(totalMetricCount)
            .as("Total number of metrics should be under limit")
            .isLessThanOrEqualTo(1000);
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