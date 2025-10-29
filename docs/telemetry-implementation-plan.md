# Telemetry Implementation Plan

This document outlines the focused implementation plan for adding OpenTelemetry observability to the Abada Engine. 

## Implementation Principles

1. **Test Strategy**
   - EXCLUDE all telemetry instrumentation from unit tests
   - Create a single integration test class to verify telemetry capture on application startup
   - Use NoopTelemetry in test profile to avoid interference

2. **Security Scope**
   - Security features are OUT of scope for this implementation
   - Default to basic configurations without security enhancements

## Implementation Phases

### Phase 1: Core Telemetry Setup

1. **Basic Configuration**
   - Configure OTLP exporters for traces and metrics
   - Set up basic resource attributes
   - Configure sampling (100% in dev, configurable in prod)

2. **Metric Implementation**
   - Process metrics
     - Instance creation/completion rates
     - Execution duration
     - Active instances count
   
   - Task metrics
     - Creation/completion rates
     - Processing time
     - Waiting time
     - Active tasks count
   
   - Event metrics
     - Processing rates
     - Correlation success/failure

3. **Tracing Implementation**
   - Process execution spans
   - Task lifecycle spans
   - Event processing spans
   - Basic error tracking

### Phase 2: Visualization

1. **Monitoring Setup**
   - Basic Grafana dashboard for process metrics
   - Basic Grafana dashboard for task metrics
   - Simple event monitoring view

2. **Alert Rules**
   - Basic process failure alerts
   - Task stagnation alerts
   - Event processing delay alerts

### Phase 3: Integration Testing

1. **Integration Test Class**
   - Verify OTLP metric export
   - Verify trace generation
   - Basic dashboard validation

## Technical Specifications

### Metric Naming Convention
```
abada_<component>_<metric_name>_<unit>
```

Examples:
- `abada_process_instances_active`
- `abada_tasks_completed_total`
- `abada_task_processing_time_seconds`

### Span Naming Convention
```
abada.<component>.<operation>
```

Examples:
- `abada.process.start`
- `abada.task.complete`
- `abada.event.correlate`

## Dependencies

Required Maven dependencies:
```xml
<!-- OpenTelemetry -->
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-api</artifactId>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-sdk</artifactId>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>

<!-- Micrometer -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-otlp</artifactId>
</dependency>
```

## Implementation Tasks

1. **Configure Base Telemetry Setup**
   - [x] Update ObservabilityConfig with OTLP setup
   - [x] Create TestObservabilityConfig for unit tests
   - [x] Configure application.yaml with telemetry settings
   - [x] Set up resource attributes
   - [x] Create basic integration test

2. **Implement Engine Metrics Service** ✓
   - [x] Create EngineMetrics service class
   - [x] Define metric naming constants
   - [x] Implement counter metrics methods
   - [x] Implement timer metrics methods
   - [x] Implement gauge metrics methods

3. **Add Process Metrics** ✓
   - [x] Process instance creation counter
   - [x] Process completion counter
   - [x] Process execution duration timer
   - [x] Active process instances gauge
   - [x] Process failure counter

4. **Add Task Metrics** ✓
   - [x] Task creation counter
   - [x] Task completion counter
   - [x] Task processing time timer
   - [x] Task waiting time timer
   - [x] Active tasks gauge
   - [x] Task failure counter

5. **Add Event Metrics** ✓
   - [x] Event processing counter
   - [x] Event correlation timer
   - [x] Event delivery success/failure counters
   - [x] Event queue size gauge
   - [x] Event processing latency timer

6. **Add Core Tracing** ✓
   - [x] Process execution spans
   - [x] Task lifecycle spans
   - [x] Event processing spans
   - [x] Error tracking spans
   - [x] Add span attributes

## Current Progress

**ALL TASKS COMPLETED** ✅

Task 1 ✓ Completed:
- Base configuration implemented
- Test configuration in place
- Basic integration test created

Task 2 ✓ Completed:
- EngineMetrics service class implemented
- Metric naming constants defined
- Counter, timer, and gauge methods implemented

Task 3 ✓ Completed:
- Process metrics fully implemented
- Process instance creation/completion/failure counters
- Process execution duration timer
- Active process instances gauge

Task 4 ✓ Completed:
- Task metrics fully implemented
- Task creation/completion/failure counters
- Task processing and waiting time timers
- Active tasks gauge

Task 5 ✓ Completed:
- Event metrics fully implemented
- Event published/consumed/correlated counters
- Event processing latency timer
- Event queue size gauge
- Event metrics integration in EventManager

Task 6 ✓ Completed:
- Core tracing fully implemented
- Process execution spans in AbadaEngine
- Task lifecycle spans in TaskManager
- Event processing spans in EventManager
- Error tracking spans across all components
- Comprehensive span attributes

**IMPLEMENTATION STATUS: 100% COMPLETE** 🎉