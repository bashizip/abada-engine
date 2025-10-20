package com.abada.engine.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class EngineMetricsTest {

    private MeterRegistry registry;
    private EngineMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new EngineMetrics(registry);
    }

    @Nested
    class ProcessMetricsTests {
        
        @Test
        void shouldRecordProcessLifecycle() {
            String processId = "test-process";
            Timer.Sample sample = metrics.startProcessTimer();

            // Start process
            metrics.recordProcessStarted(processId);
            assertThat(getProcessCounter("started", processId).count()).isEqualTo(1.0);
            assertThat(metrics.getActiveProcessInstances()).isEqualTo(1.0);

            // Complete process
            metrics.recordProcessCompleted(processId);
            metrics.recordProcessDuration(sample, processId);

            assertThat(getProcessCounter("completed", processId).count()).isEqualTo(1.0);
            assertThat(metrics.getActiveProcessInstances()).isEqualTo(0.0);
            assertThat(getProcessTimer(processId).count()).isEqualTo(1L);
        }

        @Test
        void shouldRecordFailedProcess() {
            String processId = "failed-process";
            
            metrics.recordProcessStarted(processId);
            metrics.recordProcessFailed(processId);

            assertThat(getProcessCounter("failed", processId).count()).isEqualTo(1.0);
            assertThat(metrics.getActiveProcessInstances()).isEqualTo(0.0);
        }

        @Test
        void shouldTrackMultipleProcesses() {
            metrics.recordProcessStarted("process1");
            metrics.recordProcessStarted("process2");
            metrics.recordProcessStarted("process3");

            assertThat(metrics.getActiveProcessInstances()).isEqualTo(3.0);

            metrics.recordProcessCompleted("process1");
            metrics.recordProcessFailed("process2");

            assertThat(metrics.getActiveProcessInstances()).isEqualTo(1.0);
        }

        private Counter getProcessCounter(String type, String processId) {
            return registry.get("abada.process.instances." + type)
                         .tag("process.definition.id", processId)
                         .counter();
        }

        private Timer getProcessTimer(String processId) {
            return registry.get("abada.process.duration")
                         .tag("process.definition.id", processId)
                         .timer();
        }
    }

    @Nested
    class TaskMetricsTests {

        @Test
        void shouldRecordTaskLifecycle() {
            String taskKey = "user-task";
            Timer.Sample waitingSample = metrics.startTaskWaitingTimer();
            
            // Create task
            metrics.recordTaskCreated(taskKey);
            assertThat(getTaskCounter("created", taskKey).count()).isEqualTo(1.0);
            assertThat(metrics.getActiveTasks()).isEqualTo(1.0);

            // Claim task
            metrics.recordTaskClaimed(taskKey);
            metrics.recordTaskWaitingTime(waitingSample, taskKey);
            assertThat(getTaskCounter("claimed", taskKey).count()).isEqualTo(1.0);
            assertThat(getTaskWaitingTimer(taskKey).count()).isEqualTo(1L);

            // Complete task
            Timer.Sample processingSample = metrics.startTaskProcessingTimer();
            metrics.recordTaskCompleted(taskKey);
            metrics.recordTaskProcessingTime(processingSample, taskKey);

            assertThat(getTaskCounter("completed", taskKey).count()).isEqualTo(1.0);
            assertThat(metrics.getActiveTasks()).isEqualTo(0.0);
            assertThat(getTaskProcessingTimer(taskKey).count()).isEqualTo(1L);
        }

        @Test
        void shouldRecordFailedTask() {
            String taskKey = "failed-task";
            
            metrics.recordTaskCreated(taskKey);
            metrics.recordTaskFailed(taskKey);

            assertThat(getTaskCounter("failed", taskKey).count()).isEqualTo(1.0);
            assertThat(metrics.getActiveTasks()).isEqualTo(0.0);
        }

        @Test
        void shouldTrackMultipleTasks() {
            metrics.recordTaskCreated("task1");
            metrics.recordTaskCreated("task2");
            metrics.recordTaskCreated("task3");

            assertThat(metrics.getActiveTasks()).isEqualTo(3.0);

            metrics.recordTaskCompleted("task1");
            metrics.recordTaskFailed("task2");

            assertThat(metrics.getActiveTasks()).isEqualTo(1.0);
        }

        private Counter getTaskCounter(String type, String taskKey) {
            return registry.get("abada.tasks." + type)
                         .tag("task.definition.key", taskKey)
                         .counter();
        }

        private Timer getTaskWaitingTimer(String taskKey) {
            return registry.get("abada.task.waiting_time")
                         .tag("task.definition.key", taskKey)
                         .timer();
        }

        private Timer getTaskProcessingTimer(String taskKey) {
            return registry.get("abada.task.processing_time")
                         .tag("task.definition.key", taskKey)
                         .timer();
        }
    }

    @Nested
    class EventMetricsTests {

        @Test
        void shouldRecordEventLifecycle() {
            String eventType = "message";
            String eventName = "payment-received";

            // Publish event
            metrics.recordEventPublished(eventType, eventName);
            assertThat(getEventCounter("published", eventType, eventName).count()).isEqualTo(1.0);

            // Consume event
            metrics.recordEventConsumed(eventType, eventName);
            assertThat(getEventCounter("consumed", eventType, eventName).count()).isEqualTo(1.0);

            // Correlate event
            metrics.recordEventCorrelated(eventType, eventName);
            assertThat(getEventCounter("correlated", eventType, eventName).count()).isEqualTo(1.0);
        }

        @Test
        void shouldTrackDifferentEventTypes() {
            metrics.recordEventPublished("message", "event1");
            metrics.recordEventPublished("signal", "event2");
            metrics.recordEventPublished("timer", "event3");

            assertThat(getEventCounter("published", "message", "event1").count()).isEqualTo(1.0);
            assertThat(getEventCounter("published", "signal", "event2").count()).isEqualTo(1.0);
            assertThat(getEventCounter("published", "timer", "event3").count()).isEqualTo(1.0);
        }

        private Counter getEventCounter(String type, String eventType, String eventName) {
            return registry.get("abada.events." + type)
                         .tag("event.type", eventType)
                         .tag("event.name", eventName)
                         .counter();
        }
    }

    @Nested
    class JobMetricsTests {

        @Test
        void shouldRecordJobLifecycle() {
            String jobType = "timer";
            Timer.Sample sample = metrics.startJobExecutionTimer();

            // Execute job
            metrics.recordJobExecuted(jobType);
            metrics.recordJobExecutionTime(sample, jobType);

            assertThat(getJobCounter("executed", jobType).count()).isEqualTo(1.0);
            assertThat(getJobTimer(jobType).count()).isEqualTo(1L);
        }

        @Test
        void shouldRecordFailedJob() {
            String jobType = "async-task";
            
            metrics.recordJobExecuted(jobType);
            metrics.recordJobFailed(jobType);

            assertThat(getJobCounter("failed", jobType).count()).isEqualTo(1.0);
        }

        @Test
        void shouldTrackDifferentJobTypes() {
            metrics.recordJobExecuted("timer");
            metrics.recordJobExecuted("async");
            metrics.recordJobExecuted("message");

            assertThat(getJobCounter("executed", "timer").count()).isEqualTo(1.0);
            assertThat(getJobCounter("executed", "async").count()).isEqualTo(1.0);
            assertThat(getJobCounter("executed", "message").count()).isEqualTo(1.0);
        }

        private Counter getJobCounter(String type, String jobType) {
            return registry.get("abada.jobs." + type)
                         .tag("job.type", jobType)
                         .counter();
        }

        private Timer getJobTimer(String jobType) {
            return registry.get("abada.job.execution_time")
                         .tag("job.type", jobType)
                         .timer();
        }
    }

    @Nested
    class ConcurrencyTests {

        @Test
        void shouldHandleConcurrentProcessMetrics() throws InterruptedException {
            int threadCount = 10;
            Thread[] threads = new Thread[threadCount];
            
            for (int i = 0; i < threadCount; i++) {
                final String processId = "concurrent-process-" + i;
                threads[i] = new Thread(() -> {
                    Timer.Sample sample = metrics.startProcessTimer();
                    metrics.recordProcessStarted(processId);
                    try {
                        TimeUnit.MILLISECONDS.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    metrics.recordProcessCompleted(processId);
                    metrics.recordProcessDuration(sample, processId);
                });
                threads[i].start();
            }

            for (Thread thread : threads) {
                thread.join();
            }

            assertThat(metrics.getActiveProcessInstances()).isEqualTo(0.0);
        }

        @Test
        void shouldHandleConcurrentTaskMetrics() throws InterruptedException {
            int threadCount = 10;
            Thread[] threads = new Thread[threadCount];
            
            for (int i = 0; i < threadCount; i++) {
                final String taskKey = "concurrent-task-" + i;
                threads[i] = new Thread(() -> {
                    Timer.Sample waitingSample = metrics.startTaskWaitingTimer();
                    metrics.recordTaskCreated(taskKey);
                    try {
                        TimeUnit.MILLISECONDS.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    metrics.recordTaskClaimed(taskKey);
                    metrics.recordTaskWaitingTime(waitingSample, taskKey);
                    
                    Timer.Sample processingSample = metrics.startTaskProcessingTimer();
                    metrics.recordTaskCompleted(taskKey);
                    metrics.recordTaskProcessingTime(processingSample, taskKey);
                });
                threads[i].start();
            }

            for (Thread thread : threads) {
                thread.join();
            }

            assertThat(metrics.getActiveTasks()).isEqualTo(0.0);
        }
    }
}