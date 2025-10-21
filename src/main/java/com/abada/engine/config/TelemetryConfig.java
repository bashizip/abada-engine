package com.abada.engine.config;

import io.micrometer.core.instrument.*;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class TelemetryConfig {
    
    private final MeterRegistry meterRegistry;
    private final OpenTelemetry openTelemetry;

    public TelemetryConfig(MeterRegistry meterRegistry, OpenTelemetry openTelemetry) {
        this.meterRegistry = meterRegistry;
        this.openTelemetry = openTelemetry;
    }

    @Bean
    public EngineMetrics engineMetrics() {
        return new EngineMetrics(meterRegistry);
    }

    @Bean
    public Tracer engineTracer() {
        return openTelemetry.getTracer("com.abada.engine");
    }

    public static class EngineMetrics {
        private final Counter processInstancesStarted;
        private final Counter processInstancesCompleted;
        private final Counter processInstancesFailed;
        private final Timer processExecutionDuration;
        private final Gauge activeProcessInstances;

        private final Counter tasksCreated;
        private final Counter tasksClaimed;
        private final Counter tasksCompleted;
        private final Counter tasksFailed;
        private final Timer taskWaitingTime;
        private final Timer taskProcessingTime;
        private final Gauge activeTasks;

        private final Counter eventsPublished;
        private final Counter eventsConsumed;
        private final Counter eventsCorrelated;

        public EngineMetrics(MeterRegistry registry) {
            // Process metrics
            this.processInstancesStarted = Counter.builder("abada.process.instances.started")
                .description("Total number of process instances started")
                .register(registry);

            this.processInstancesCompleted = Counter.builder("abada.process.instances.completed")
                .description("Total number of process instances completed")
                .register(registry);

            this.processInstancesFailed = Counter.builder("abada.process.instances.failed")
                .description("Total number of process instances failed")
                .register(registry);

            this.processExecutionDuration = Timer.builder("abada.process.duration")
                .description("Process execution duration")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);

            this.activeProcessInstances = Gauge.builder("abada.process.instances.active", 
                () -> 0) // Initial value, will be updated dynamically
                .description("Number of currently active process instances")
                .register(registry);

            // Task metrics
            this.tasksCreated = Counter.builder("abada.tasks.created")
                .description("Total number of tasks created")
                .register(registry);

            this.tasksClaimed = Counter.builder("abada.tasks.claimed")
                .description("Total number of tasks claimed")
                .register(registry);

            this.tasksCompleted = Counter.builder("abada.tasks.completed")
                .description("Total number of tasks completed")
                .register(registry);

            this.tasksFailed = Counter.builder("abada.tasks.failed")
                .description("Total number of tasks failed")
                .register(registry);

            this.taskWaitingTime = Timer.builder("abada.task.waiting_time")
                .description("Time from task creation to claim")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);

            this.taskProcessingTime = Timer.builder("abada.task.processing_time")
                .description("Time from task claim to completion")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);

            this.activeTasks = Gauge.builder("abada.tasks.active",
                () -> 0) // Initial value, will be updated dynamically
                .description("Number of currently active tasks")
                .register(registry);

            // Event metrics
            this.eventsPublished = Counter.builder("abada.events.published")
                .description("Total number of events published")
                .register(registry);

            this.eventsConsumed = Counter.builder("abada.events.consumed")
                .description("Total number of events consumed")
                .register(registry);

            this.eventsCorrelated = Counter.builder("abada.events.correlated")
                .description("Total number of events correlated to instances")
                .register(registry);
        }

        // Process metrics methods
        public void recordProcessStarted(String definitionId) {
            processInstancesStarted.increment();
            Tags tags = Tags.of("process.definition.id", definitionId);
            processInstancesStarted.tags(tags).increment();
        }

        public Timer.Sample startProcessTimer() {
            return Timer.start();
        }

        public void recordProcessCompleted(String definitionId, Timer.Sample sample) {
            processInstancesCompleted.increment();
            Tags tags = Tags.of("process.definition.id", definitionId);
            processInstancesCompleted.tags(tags).increment();
            sample.stop(processExecutionDuration);
        }

        public void recordProcessFailed(String definitionId, Timer.Sample sample) {
            processInstancesFailed.increment();
            Tags tags = Tags.of("process.definition.id", definitionId);
            processInstancesFailed.tags(tags).increment();
            sample.stop(processExecutionDuration);
        }

        public void updateActiveProcessCount(long count) {
            ((AtomicNumber) activeProcessInstances).set(count);
        }

        // Task metrics methods
        public void recordTaskCreated(String taskDefinitionKey) {
            tasksCreated.increment();
            Tags tags = Tags.of("task.definition.key", taskDefinitionKey);
            tasksCreated.tags(tags).increment();
        }

        public Timer.Sample startTaskWaitingTimer() {
            return Timer.start();
        }

        public void recordTaskClaimed(String taskDefinitionKey, Timer.Sample waitingSample) {
            tasksClaimed.increment();
            Tags tags = Tags.of("task.definition.key", taskDefinitionKey);
            tasksClaimed.tags(tags).increment();
            if (waitingSample != null) {
                waitingSample.stop(taskWaitingTime);
            }
        }

        public Timer.Sample startTaskProcessingTimer() {
            return Timer.start();
        }

        public void recordTaskCompleted(String taskDefinitionKey, Timer.Sample processingSample) {
            tasksCompleted.increment();
            Tags tags = Tags.of("task.definition.key", taskDefinitionKey);
            tasksCompleted.tags(tags).increment();
            if (processingSample != null) {
                processingSample.stop(taskProcessingTime);
            }
        }

        public void recordTaskFailed(String taskDefinitionKey) {
            tasksFailed.increment();
            Tags tags = Tags.of("task.definition.key", taskDefinitionKey);
            tasksFailed.tags(tags).increment();
        }

        public void updateActiveTaskCount(long count) {
            ((AtomicNumber) activeTasks).set(count);
        }

        // Event metrics methods
        public void recordEventPublished(String eventType, String eventName) {
            eventsPublished.increment();
            Tags tags = Tags.of(
                "event.type", eventType,
                "event.name", eventName
            );
            eventsPublished.tags(tags).increment();
        }

        public void recordEventConsumed(String eventType, String eventName) {
            eventsConsumed.increment();
            Tags tags = Tags.of(
                "event.type", eventType,
                "event.name", eventName
            );
            eventsConsumed.tags(tags).increment();
        }

        public void recordEventCorrelated(String eventType, String eventName) {
            eventsCorrelated.increment();
            Tags tags = Tags.of(
                "event.type", eventType,
                "event.name", eventName
            );
            eventsCorrelated.tags(tags).increment();
        }
    }
}