package com.abada.engine.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Centralized metrics management for the Abada Engine.
 * Provides counters, histograms, and gauges for process execution, task management,
 * event processing, and job scheduling.
 */
@Component
public class EngineMetrics {

    private final MeterRegistry meterRegistry;

    // Process Metrics
    private final Counter processInstancesStarted;
    private final Counter processInstancesCompleted;
    private final Counter processInstancesFailed;
    private final Timer processDuration;
    private final AtomicLong activeProcessInstances = new AtomicLong(0);
    
    // Cached counters for tagged metrics
    private final ConcurrentHashMap<String, Counter> processStartedCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> processCompletedCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> processFailedCounters = new ConcurrentHashMap<>();

    // Task Metrics
    private final Counter tasksCreated;
    private final Counter tasksClaimed;
    private final Counter tasksCompleted;
    private final Counter tasksFailed;
    private final Timer taskWaitingTime;
    private final Timer taskProcessingTime;
    private final AtomicLong activeTasks = new AtomicLong(0);

    // Event Metrics
    private final Counter eventsPublished;
    private final Counter eventsConsumed;
    private final Counter eventsCorrelated;

    // Job Metrics
    private final Counter jobsExecuted;
    private final Counter jobsFailed;
    private final Timer jobExecutionTime;

    public EngineMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // Initialize Process Metrics
        this.processInstancesStarted = Counter.builder("abada.process.instances.started")
                .description("Total number of process instances started")
                .register(meterRegistry);

        this.processInstancesCompleted = Counter.builder("abada.process.instances.completed")
                .description("Total number of process instances completed")
                .register(meterRegistry);

        this.processInstancesFailed = Counter.builder("abada.process.instances.failed")
                .description("Total number of process instances failed")
                .register(meterRegistry);

        this.processDuration = Timer.builder("abada.process.duration")
                .description("Process execution duration")
                .register(meterRegistry);

        Gauge.builder("abada.process.instances.active")
                .description("Number of currently active process instances")
                .register(meterRegistry, activeProcessInstances, AtomicLong::get);

        // Initialize Task Metrics
        this.tasksCreated = Counter.builder("abada.tasks.created")
                .description("Total number of tasks created")
                .register(meterRegistry);

        this.tasksClaimed = Counter.builder("abada.tasks.claimed")
                .description("Total number of tasks claimed")
                .register(meterRegistry);

        this.tasksCompleted = Counter.builder("abada.tasks.completed")
                .description("Total number of tasks completed")
                .register(meterRegistry);

        this.tasksFailed = Counter.builder("abada.tasks.failed")
                .description("Total number of tasks failed")
                .register(meterRegistry);

        this.taskWaitingTime = Timer.builder("abada.task.waiting_time")
                .description("Time from task creation to claim")
                .register(meterRegistry);

        this.taskProcessingTime = Timer.builder("abada.task.processing_time")
                .description("Time from task claim to completion")
                .register(meterRegistry);

        Gauge.builder("abada.tasks.active")
                .description("Number of currently active tasks")
                .register(meterRegistry, activeTasks, AtomicLong::get);

        // Initialize Event Metrics
        this.eventsPublished = Counter.builder("abada.events.published")
                .description("Total number of events published")
                .register(meterRegistry);

        this.eventsConsumed = Counter.builder("abada.events.consumed")
                .description("Total number of events consumed")
                .register(meterRegistry);

        this.eventsCorrelated = Counter.builder("abada.events.correlated")
                .description("Total number of events correlated to process instances")
                .register(meterRegistry);

        // Initialize Job Metrics
        this.jobsExecuted = Counter.builder("abada.jobs.executed")
                .description("Total number of jobs executed")
                .register(meterRegistry);

        this.jobsFailed = Counter.builder("abada.jobs.failed")
                .description("Total number of jobs failed")
                .register(meterRegistry);

        this.jobExecutionTime = Timer.builder("abada.job.execution_time")
                .description("Job execution duration")
                .register(meterRegistry);
    }

    // Process Metrics Methods
    public void recordProcessStarted(String processDefinitionId) {
        processInstancesStarted.increment();
        activeProcessInstances.incrementAndGet();
    }

    public void recordProcessCompleted(String processDefinitionId) {
        processInstancesCompleted.increment();
        activeProcessInstances.decrementAndGet();
    }

    public void recordProcessFailed(String processDefinitionId) {
        processInstancesFailed.increment();
        activeProcessInstances.decrementAndGet();
    }

    public Timer.Sample startProcessTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordProcessDuration(Timer.Sample sample, String processDefinitionId) {
        sample.stop(processDuration);
    }

    // Task Metrics Methods
    public void recordTaskCreated(String taskDefinitionKey) {
        tasksCreated.increment();
        activeTasks.incrementAndGet();
    }

    public void recordTaskClaimed(String taskDefinitionKey) {
        tasksClaimed.increment();
    }

    public void recordTaskCompleted(String taskDefinitionKey) {
        tasksCompleted.increment();
        activeTasks.decrementAndGet();
    }

    public void recordTaskFailed(String taskDefinitionKey) {
        tasksFailed.increment();
        activeTasks.decrementAndGet();
    }

    public Timer.Sample startTaskWaitingTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordTaskWaitingTime(Timer.Sample sample, String taskDefinitionKey) {
        sample.stop(taskWaitingTime);
    }

    public Timer.Sample startTaskProcessingTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordTaskProcessingTime(Timer.Sample sample, String taskDefinitionKey) {
        sample.stop(taskProcessingTime);
    }

    // Event Metrics Methods
    public void recordEventPublished(String eventType, String eventName) {
        eventsPublished.increment();
    }

    public void recordEventConsumed(String eventType, String eventName) {
        eventsConsumed.increment();
    }

    public void recordEventCorrelated(String eventType, String eventName) {
        eventsCorrelated.increment();
    }

    // Job Metrics Methods
    public void recordJobExecuted(String jobType) {
        jobsExecuted.increment();
    }

    public void recordJobFailed(String jobType) {
        jobsFailed.increment();
    }

    public Timer.Sample startJobExecutionTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordJobExecutionTime(Timer.Sample sample, String jobType) {
        sample.stop(jobExecutionTime);
    }

    // Gauge Methods
    public double getActiveProcessInstances() {
        return activeProcessInstances.get();
    }

    public double getActiveTasks() {
        return activeTasks.get();
    }
}
