# OpenTelemetry Observability Guide

This document provides a comprehensive guide to the OpenTelemetry observability features integrated into the Abada BPMN Engine.

## Overview

The Abada Engine is fully instrumented with OpenTelemetry to provide comprehensive observability through:

- **Distributed Tracing**: Track requests across the entire BPMN process execution
- **Metrics**: Monitor performance, throughput, and system health
- **Logging**: Structured logs with trace correlation and centralized aggregation

## Architecture

### Components

1. **ObservabilityConfig**: Main configuration class that sets up OpenTelemetry SDK
2. **EngineMetrics**: Centralized metrics management for all engine operations
3. **Manual Instrumentation**: Custom spans and metrics in core engine components
4. **Auto-instrumentation**: Spring Boot automatic instrumentation for HTTP requests

### Data Flow

```
Application → OpenTelemetry SDK → OTLP Exporter → Observability Backend
```

## Configuration

### Environment Variables

```bash
# OTLP Endpoints
OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4318
OTEL_EXPORTER_OTLP_TRACES_ENDPOINT=http://localhost:4318/v1/traces
OTEL_EXPORTER_OTLP_METRICS_ENDPOINT=http://localhost:4318/v1/metrics
OTEL_EXPORTER_OTLP_LOGS_ENDPOINT=http://localhost:4318/v1/logs

# Service Information
OTEL_SERVICE_NAME=abada-engine
OTEL_SERVICE_VERSION=0.8.2-alpha
OTEL_DEPLOYMENT_ENVIRONMENT=development

# Sampling
OTEL_TRACES_SAMPLER=traceidratio
OTEL_TRACES_SAMPLER_ARG=1.0
```

### Application Properties

```yaml
management:
  tracing:
    sampling:
      probability: 1.0
  otlp:
    tracing:
      endpoint: http://localhost:4318/v1/traces
    metrics:
      endpoint: http://localhost:4318/v1/metrics
    logs:
      endpoint: http://localhost:4318/v1/logs

otel:
  service:
    name: abada-engine
    version: 0.8.2-alpha
  resource:
    attributes:
      service.name: abada-engine
      service.version: 0.8.2-alpha
      deployment.environment: development
```

## Metrics

### Process Metrics

| Metric Name | Type | Description | Labels |
|-------------|------|-------------|---------|
| `abada.process.instances.started` | Counter | Total process instances started | `process.definition.id` |
| `abada.process.instances.completed` | Counter | Total process instances completed | `process.definition.id` |
| `abada.process.instances.failed` | Counter | Total process instances failed | `process.definition.id` |
| `abada.process.duration` | Histogram | Process execution duration | `process.definition.id` |
| `abada.process.instances.active` | Gauge | Currently active process instances | - |

### Task Metrics

| Metric Name | Type | Description | Labels |
|-------------|------|-------------|---------|
| `abada.tasks.created` | Counter | Total tasks created | `task.definition.key` |
| `abada.tasks.claimed` | Counter | Total tasks claimed | `task.definition.key` |
| `abada.tasks.completed` | Counter | Total tasks completed | `task.definition.key` |
| `abada.tasks.failed` | Counter | Total tasks failed | `task.definition.key` |
| `abada.task.waiting_time` | Histogram | Time from task creation to claim | `task.definition.key` |
| `abada.task.processing_time` | Histogram | Time from task claim to completion | `task.definition.key` |
| `abada.tasks.active` | Gauge | Currently active tasks | - |

### Event Metrics

| Metric Name | Type | Description | Labels |
|-------------|------|-------------|---------|
| `abada.events.published` | Counter | Total events published | `event.type`, `event.name` |
| `abada.events.consumed` | Counter | Total events consumed | `event.type`, `event.name` |
| `abada.events.correlated` | Counter | Total events correlated to instances | `event.type`, `event.name` |

### Job Metrics

| Metric Name | Type | Description | Labels |
|-------------|------|-------------|---------|
| `abada.jobs.executed` | Counter | Total jobs executed | `job.type` |
| `abada.jobs.failed` | Counter | Total jobs failed | `job.type` |
| `abada.job.execution_time` | Histogram | Job execution duration | `job.type` |

## Tracing

### Span Structure

#### Process Execution Trace
```
abada.process.start
├── abada.task.create
│   ├── abada.task.claim
│   └── abada.task.complete
├── abada.event.correlate.message
└── abada.job.execute
```

#### Key Spans

| Span Name | Description | Attributes |
|-----------|-------------|------------|
| `abada.process.deploy` | Process definition deployment | `process.definition.id`, `process.definition.name`, `process.definition.version` |
| `abada.process.start` | Process instance creation | `process.instance.id`, `process.definition.id`, `process.definition.name` |
| `abada.process.get` | Process instance retrieval | `process.instance.id`, `process.definition.id`, `process.status` |
| `abada.task.create` | Task creation | `task.id`, `task.definition.key`, `task.name`, `process.instance.id`, `task.status` |
| `abada.task.claim` | Task claiming | `task.id`, `task.definition.key`, `user.name`, `process.instance.id` |
| `abada.task.complete` | Task completion | `task.id`, `task.definition.key`, `user.name`, `process.instance.id` |
| `abada.task.fail` | Task failure | `task.id`, `task.definition.key`, `user.name`, `process.instance.id` |
| `abada.event.correlate.message` | Message event correlation | `event.name`, `event.type`, `correlation.key`, `process.instance.id` |
| `abada.event.broadcast.signal` | Signal event broadcasting | `event.name`, `event.type`, `instances.count` |
| `abada.job.schedule` | Job scheduling | `job.id`, `process.instance.id`, `event.id`, `execution.timestamp`, `job.type` |
| `abada.job.execute` | Job execution | `job.id`, `process.instance.id`, `event.id`, `job.type` |

### Trace Context Propagation

The engine automatically propagates trace context through:
- HTTP requests (via Spring Boot auto-instrumentation)
- Process instance execution
- Task lifecycle operations
- Event correlation

## Logging

### Log Format

All logs include trace correlation information:

```
2024-01-15 10:30:45.123 [http-nio-8080-exec-1] INFO [abc123def456,789xyz012] com.abada.engine.core.AbadaEngine - Started process instance: proc_001
```

Where:
- `abc123def456` is the trace ID
- `789xyz012` is the span ID

### Log Levels

| Component | Level | Description |
|-----------|-------|-------------|
| `com.abada.engine` | DEBUG | All engine operations |
| `io.opentelemetry` | INFO | OpenTelemetry SDK operations |
| `io.micrometer` | INFO | Micrometer metrics operations |
| `org.springframework.web` | INFO | HTTP request/response |
| `org.hibernate.SQL` | DEBUG | SQL queries |

### Log Aggregation with Loki

#### Overview

Loki provides centralized log aggregation for the Abada Engine using Promtail for log collection:
- **File-based Collection**: Promtail tails application log files
- **Efficient Storage**: Label-based indexing for fast queries
- **Grafana Integration**: Seamless log visualization and exploration

#### Architecture

```
Spring Boot → Logback (File) → Promtail → Loki → Grafana
```

#### Configuration

##### Logback Configuration

The application writes logs to files which are then picked up by Promtail.

```xml
<!-- File Appender -->
<appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>logs/abada-engine.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
        <fileNamePattern>logs/abada-engine.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
        <timeBasedFileNamingAndTriggeringPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">
            <maxFileSize>10MB</maxFileSize>
        </timeBasedFileNamingAndTriggeringPolicy>
        <maxHistory>30</maxHistory>
    </rollingPolicy>
    <encoder>
        <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%X{traceId},%X{spanId}] %logger{36} - %msg%n</pattern>
    </encoder>
</appender>
```

##### Promtail Configuration

Promtail is configured to scrape the log files and forward them to Loki.

```yaml
scrape_configs:
  - job_name: abada-engine
    static_configs:
      - targets:
          - localhost
        labels:
          job: abada-engine
          service_name: abada-engine
          __path__: /var/log/abada/*.log
```

##### Docker Compose

Promtail mounts the application logs directory:

```yaml
  promtail:
    image: grafana/promtail:2.9.1
    volumes:
      - ./logs:/var/log/abada:ro
      - ./promtail-config.yaml:/etc/promtail/config.yml
    command: -config.file=/etc/promtail/config.yml
```

### Querying Logs

#### LogQL Examples

```logql
# All logs from abada-engine
{service_name="abada-engine"}

# Filter by log level
{service_name="abada-engine"} | json | level="ERROR"

# Search for specific text
{service_name="abada-engine"} |= "process instance"

# Logs for a specific trace
{service_name="abada-engine"} | json | traceId="abc123def456"

# Logs from a specific component
{service_name="abada-engine"} | json | logger=~"com.abada.engine.core.*"
```

#### API Queries

```bash
# Query logs from last 5 minutes
curl -G -s "http://localhost:3100/loki/api/v1/query_range" \
  --data-urlencode 'query={service_name="abada-engine"}' \
  --data-urlencode "start=$(date -d '5 minutes ago' +%s)000000000" \
  --data-urlencode "end=$(date +%s)000000000"

# Get available labels
curl -s http://localhost:3100/loki/api/v1/labels

# Get label values
curl -s http://localhost:3100/loki/api/v1/label/service_name/values
```

### Trace-to-Logs Correlation

Logs automatically include trace context from MDC:
- `traceId`: Links logs to distributed traces
- `spanId`: Links logs to specific span operations

This enables:
1. **Trace → Logs**: View logs for a specific trace in Grafana
2. **Logs → Trace**: Jump from error logs to the full trace
3. **Debugging**: Correlate application behavior across services

## Integration Examples

### Jaeger

```yaml
# docker-compose.yml
version: '3.8'
services:
  jaeger:
    image: jaegertracing/all-in-one:latest
    ports:
      - "16686:16686"
      - "14250:14250"
    environment:
      - COLLECTOR_OTLP_ENABLED=true
```

### Grafana + Prometheus

```yaml
# docker-compose.yml
version: '3.8'
services:
  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
```

### OpenTelemetry Collector

```yaml
# otel-collector-config.yml
receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:4317
      http:
        endpoint: 0.0.0.0:4318

processors:
  batch:

exporters:
  jaeger:
    endpoint: jaeger:14250
    tls:
      insecure: true
  prometheus:
    endpoint: "0.0.0.0:8889"
  otlphttp/loki:
    endpoint: http://loki:3100/otlp
    tls:
      insecure: true

service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [batch]
      exporters: [jaeger]
    metrics:
      receivers: [otlp]
      processors: [batch]
      exporters: [prometheus]
```

## Monitoring Dashboards

### Key Metrics to Monitor

1. **Process Health**
   - Process completion rate
   - Average process duration
   - Failed process instances

2. **Task Performance**
   - Task waiting time (SLA compliance)
   - Task processing time
   - Task completion rate

3. **System Performance**
   - Active process instances
   - Active tasks
   - Event correlation success rate

4. **Error Rates**
   - Failed processes
   - Failed tasks
   - Failed jobs

### Sample Grafana Queries

```promql
# Process completion rate
rate(abada_process_instances_completed_total[5m]) / rate(abada_process_instances_started_total[5m])

# Average task waiting time
histogram_quantile(0.95, rate(abada_task_waiting_time_bucket[5m]))

# Active process instances
abada_process_instances_active

# Error rate
rate(abada_process_instances_failed_total[5m]) / rate(abada_process_instances_started_total[5m])
```

## Troubleshooting

### Common Issues

1. **No traces appearing**
   - Check OTLP endpoint configuration
   - Verify sampling configuration
   - Check network connectivity to observability backend

2. **Missing metrics**
   - Verify Micrometer OTLP registry configuration
   - Check metric export interval settings
   - Ensure metrics are being recorded in code

3. **High memory usage**
   - Adjust span batch size in configuration
   - Increase export timeout settings
   - Consider reducing sampling rate in production

### Debug Configuration

```yaml
# Enable debug logging
logging:
  level:
    io.opentelemetry: DEBUG
    io.micrometer: DEBUG
    com.abada.engine: DEBUG

# Use console exporter for debugging
otel:
  traces:
    exporter: logging
  metrics:
    exporter: logging
```

## Performance Considerations

### Sampling

- **Development**: 100% sampling for complete visibility
- **Production**: 1-10% sampling to balance observability with performance
- **High-traffic**: Use rate-based sampling

### Batch Configuration

```yaml
# Optimize for production
otel:
  traces:
    batch:
      max_export_batch_size: 512
      export_timeout: 30s
      schedule_delay: 5s
```

### Resource Usage

- **Memory**: ~1MB per 1000 active spans
- **CPU**: <1% overhead with proper sampling
- **Network**: Batch exports minimize bandwidth usage

## Security

### Data Privacy

- No sensitive data in span attributes
- User IDs are hashed in metrics labels
- Process variables are not included in traces

### Network Security

- Use TLS for OTLP exports in production
- Configure firewall rules for observability endpoints
- Use authentication tokens for cloud backends

## Best Practices

1. **Span Naming**: Use consistent naming conventions
2. **Attribute Limits**: Keep span attributes under 128 characters
3. **Error Handling**: Always record exceptions in spans
4. **Sampling**: Use appropriate sampling rates for environment
5. **Monitoring**: Set up alerts for key metrics
6. **Documentation**: Keep this guide updated with changes

## Future Enhancements

- [ ] Custom dashboards for BPMN-specific metrics
- [ ] Integration with process mining tools
- [ ] Advanced correlation with external systems
- [ ] Automated anomaly detection
- [ ] Performance optimization recommendations


# Implementation Details

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