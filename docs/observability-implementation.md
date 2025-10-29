# Observability Setup

## Overview
This document describes the observability setup for the Abada Engine, including metrics collection, tracing, and monitoring dashboards. The implementation uses OpenTelemetry for instrumentation, Prometheus for metrics collection, and Grafana for visualization.

## Metrics Implementation

### Core Metrics
The engine captures the following key metrics:

1. Task Metrics
   - Task creation rate (`abada_tasks_created_total`)
   - Task completion rate (`abada_tasks_completed_total`)
   - Task failure rate (`abada_tasks_failed_total`)
   - Task waiting time (`abada_task_waiting_time`)
   - Task processing time (`abada_task_processing_time`)

2. Process Metrics
   - Process instance creation rate
   - Process completion rate
   - Process execution time
   - Active process instances

3. Event Metrics
   - Event processing rate
   - Event delivery time
   - Failed event deliveries

## Monitoring Dashboards

### Overview Dashboard
Located at `monitoring/grafana/dashboards/abada-engine-overview.json`

Provides a high-level view of the engine's performance including:
- Process execution metrics
- Task completion rates
- Event processing statistics
- System health indicators

### Task Details Dashboard
Located at `monitoring/grafana/dashboards/abada-task-details.json`

Offers detailed task-level metrics:
- Task waiting and processing time distributions (p50, p95)
- Task creation and completion rates by type
- Success/failure ratios
- Completion rate gauges with thresholds

## Testing Implementation

The observability components are properly tested with mocked dependencies:

```java
// Example test setup
@BeforeEach
void setUp() {
    engineMetrics = mock(EngineMetrics.class);
    tracer = mock(Tracer.class);
    span = mock(Span.class);
    
    // Setup timer samples
    when(engineMetrics.startTaskTimer()).thenReturn(timerSample);
    
    // Setup span creation
    when(tracer.spanBuilder(anyString())).thenReturn(spanBuilder);
    when(spanBuilder.startSpan()).thenReturn(span);
}
```

## Integration with OpenTelemetry

The engine uses OpenTelemetry for distributed tracing and metrics collection:

1. Span Creation
   - Task execution spans
   - Process instance spans
   - Event processing spans

2. Metric Recording
   - Timer-based metrics for duration measurements
   - Counter-based metrics for rate calculations
   - Gauge-based metrics for current state

## Prometheus Configuration

Metrics are exposed via a Prometheus endpoint at `/actuator/prometheus`. The metrics follow the Prometheus naming conventions and include appropriate labels for aggregation and filtering.

## Dashboard Setup

To use the provided dashboards:

1. Import the JSON files into your Grafana instance:
   - `abada-engine-overview.json`
   - `abada-task-details.json`

2. Configure the Prometheus data source in Grafana

3. The dashboards will automatically populate with data once the engine starts generating metrics

## Best Practices

1. Metric Naming
   - Use the `abada_` prefix for all metrics
   - Include units in metric names where applicable
   - Use appropriate metric types (counter, gauge, histogram)

2. Tracing
   - Create spans for all significant operations
   - Include relevant attributes in spans
   - Maintain proper parent-child relationships

3. Testing
   - Mock all observability dependencies
   - Verify metric recording in tests
   - Test span lifecycle management

## Future Enhancements

1. Additional Dashboards
   - Event processing details
   - Error analysis dashboard
   - Resource utilization metrics

2. Alerting
   - Configure alerts for critical metrics
   - Set up notification channels
   - Define SLOs and SLIs

3. Extended Metrics
   - Database operation metrics
   - External service call metrics
   - Resource pool metrics