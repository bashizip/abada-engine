36package com.abada.engine.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EngineMetricsTest {

    private MeterRegistry registry;
    private EngineMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new EngineMetrics(registry);
    }

    @Test
    void shouldRecordProcessMetrics() {
        String processDefId = "test-process";
        
        metrics.incrementProcessInstancesStarted(processDefId);
        metrics.incrementProcessInstancesCompleted(processDefId);
        metrics.recordProcessDuration(processDefId, 1000);

        Counter startedCounter = registry.get("abada.process.instances.started")
                                      .tag("process.definition.id", processDefId)
                                      .counter();

        Counter completedCounter = registry.get("abada.process.instances.completed")
                                        .tag("process.definition.id", processDefId)
                                        .counter();

        Timer durationTimer = registry.get("abada.process.duration")
                                   .tag("process.definition.id", processDefId)
                                   .timer();

        assertThat(startedCounter.count()).isEqualTo(1.0);
        assertThat(completedCounter.count()).isEqualTo(1.0);
        assertThat(durationTimer.count()).isEqualTo(1L);
    }

    @Test
    void shouldRecordTaskMetrics() {
        String taskKey = "user-task";
        
        metrics.incrementTasksCreated(taskKey);
        metrics.incrementTasksClaimed(taskKey);
        metrics.recordTaskWaitingTime(taskKey, 500);
        metrics.recordTaskProcessingTime(taskKey, 1500);

        Counter createdCounter = registry.get("abada.tasks.created")
                                      .tag("task.definition.key", taskKey)
                                      .counter();

        Counter claimedCounter = registry.get("abada.tasks.claimed")
                                      .tag("task.definition.key", taskKey)
                                      .counter();

        Timer waitingTimer = registry.get("abada.task.waiting_time")
                                  .tag("task.definition.key", taskKey)
                                  .timer();

        Timer processingTimer = registry.get("abada.task.processing_time")
                                     .tag("task.definition.key", taskKey)
                                     .timer();

        assertThat(createdCounter.count()).isEqualTo(1.0);
        assertThat(claimedCounter.count()).isEqualTo(1.0);
        assertThat(waitingTimer.count()).isEqualTo(1L);
        assertThat(processingTimer.count()).isEqualTo(1L);
    }

    @Test
    void shouldRecordEventMetrics() {
        String eventType = "message";
        String eventName = "payment-received";

        metrics.incrementEventsPublished(eventType, eventName);
        metrics.incrementEventsConsumed(eventType, eventName);
        metrics.incrementEventsCorrelated(eventType, eventName);

        Counter publishedCounter = registry.get("abada.events.published")
                                        .tag("event.type", eventType)
                                        .tag("event.name", eventName)
                                        .counter();

        Counter consumedCounter = registry.get("abada.events.consumed")
                                       .tag("event.type", eventType)
                                       .tag("event.name", eventName)
                                       .counter();

        Counter correlatedCounter = registry.get("abada.events.correlated")
                                         .tag("event.type", eventType)
                                         .tag("event.name", eventName)
                                         .counter();

        assertThat(publishedCounter.count()).isEqualTo(1.0);
        assertThat(consumedCounter.count()).isEqualTo(1.0);
        assertThat(correlatedCounter.count()).isEqualTo(1.0);
    }

    @Test
    void shouldRecordJobMetrics() {
        String jobType = "timer";

        metrics.incrementJobsExecuted(jobType);
        metrics.recordJobExecutionTime(jobType, 750);

        Counter executedCounter = registry.get("abada.jobs.executed")
                                       .tag("job.type", jobType)
                                       .counter();

        Timer executionTimer = registry.get("abada.job.execution_time")
                                    .tag("job.type", jobType)
                                    .timer();

        assertThat(executedCounter.count()).isEqualTo(1.0);
        assertThat(executionTimer.count()).isEqualTo(1L);
    }
}