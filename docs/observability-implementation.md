# Observability Setup

## Overview
This document describes the observability setup for the Abada Engine, including metrics collection, distributed tracing, and log aggregation. The implementation uses OpenTelemetry for instrumentation, Prometheus for metrics collection, Jaeger for distributed tracing, Loki for log aggregation, and Grafana for unified visualization.

The observability stack implements the **three pillars of observability**:
- **Metrics**: Prometheus + Micrometer
- **Traces**: Jaeger + OpenTelemetry
- **Logs**: Loki + OpenTelemetry Logback Appender

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

## Log Aggregation with Loki

### Overview
Loki is integrated as the centralized log aggregation system, receiving logs from the application via the OpenTelemetry Collector. This provides:
- Centralized log storage and querying
- Automatic correlation between logs and traces using trace IDs
- Efficient log storage with label-based indexing
- Seamless integration with Grafana for log visualization

### Architecture
```
Spring Boot App → OpenTelemetry Logback Appender → OTel Collector → Loki → Grafana
```

### Configuration

#### Application Configuration
The application uses the OpenTelemetry Logback Appender to send logs via OTLP:

**logback-spring.xml**:
```xml
<appender name="OTEL" class="io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender">
    <captureExperimentalAttributes>true</captureExperimentalAttributes>
    <captureCodeAttributes>true</captureCodeAttributes>
    <captureMdcAttributes>*</captureMdcAttributes>
</appender>
```

**application.yaml**:
```yaml
management:
  otlp:
    logs:
      endpoint: http://otel-collector:4318/v1/logs
```

#### OpenTelemetry Collector
The OTel Collector receives logs via OTLP and forwards them to Loki:

```yaml
exporters:
  otlphttp/loki:
    endpoint: http://loki:3100/otlp
    tls:
      insecure: true

service:
  pipelines:
    logs:
      receivers: [otlp]
      processors: [memory_limiter, batch, resource]
      exporters: [otlphttp/loki, debug]
```

#### Loki Configuration
Loki is configured with:
- **Development**: In-memory ring for single-node operation
- **Production**: Consul-based ring for distributed coordination
- BoltDB shipper for index storage
- Filesystem storage for chunks
- 14-day retention period

### Querying Logs

#### Via Grafana Explore
1. Navigate to Grafana Explore view
2. Select Loki as the data source
3. Use LogQL queries:
   ```
   {service_name="abada-engine"}
   {service_name="abada-engine"} |= "error"
   {service_name="abada-engine"} | json | level="ERROR"
   ```

#### Via API
```bash
curl -G -s "http://localhost:3100/loki/api/v1/query_range" \
  --data-urlencode 'query={service_name="abada-engine"}' \
  --data-urlencode "start=$(date -d '5 minutes ago' +%s)000000000" \
  --data-urlencode "end=$(date +%s)000000000"
```

### Trace-to-Logs Correlation
Logs automatically include `traceId` and `spanId` from the MDC context, enabling:
- Jump from traces in Jaeger to related logs in Loki
- Filter logs by specific trace IDs
- Correlate distributed operations across services

Example LogQL query for a specific trace:
```
{service_name="abada-engine"} | json | traceId="abc123def456"
```

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
   - Log analytics dashboard with common queries

2. Alerting
   - Configure alerts for critical metrics
   - Set up notification channels
   - Define SLOs and SLIs
   - Log-based alerting rules

3. Extended Metrics
   - Database operation metrics
   - External service call metrics
   - Resource pool metrics

4. Advanced Log Features
   - Log sampling for high-volume scenarios
   - Structured logging enhancements
   - Log-based metrics derivation