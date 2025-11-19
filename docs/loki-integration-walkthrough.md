# Loki Integration Walkthrough

## Overview

Successfully integrated Loki as the centralized log aggregation system for the Abada Engine, completing the "three pillars of observability" (metrics, traces, and logs) using a unified OpenTelemetry pipeline.

## Changes Made

### 1. Infrastructure Configuration

#### Fixed Loki Configuration Files
- **[loki-config-dev.yaml](file:///home/pbashizi/IdeaProjects/abada-engine/docker/loki-config-dev.yaml)**: Fixed ring configuration to use proper `ingester.lifecycler.ring` structure for in-memory operation
- **[loki-config-prod.yaml](file:///home/pbashizi/IdeaProjects/abada-engine/docker/loki-config-prod.yaml)**: Fixed ring configuration to use Consul-based coordination for production

#### OpenTelemetry Collector
- **[otel-collector-config.yaml](file:///home/pbashizi/IdeaProjects/abada-engine/docker/otel-collector-config.yaml)**:
  - Added `otlphttp/loki` exporter pointing to `http://loki:3100/otlp`
  - Added logs pipeline with OTLP receiver, processors, and Loki exporter
  - Configured proper batching and resource attribution

---

### 2. Application Dependencies

#### Maven Dependencies
- **[pom.xml](file:///home/pbashizi/IdeaProjects/abada-engine/pom.xml)**:
  - Added `opentelemetry-logback-appender-1.0` (v2.10.0-alpha) for OTLP log export
  - Added `logstash-logback-encoder` (v8.0) - previously missing dependency

---

### 3. Application Logging Configuration

#### Logback Configuration
- **[logback-spring.xml](file:///home/pbashizi/IdeaProjects/abada-engine/src/main/resources/logback-spring.xml)**:
  - Added OpenTelemetry appender (`OTEL`) with full MDC capture
  - Configured to capture trace IDs, span IDs, and all context attributes
  - Added OTEL appender to all logger configurations

#### Application Properties
- **[application-dev.yaml](file:///home/pbashizi/IdeaProjects/abada-engine/src/main/resources/application-dev.yaml)**: Added `management.otlp.logs.endpoint`
- **[application-prod.yaml](file:///home/pbashizi/IdeaProjects/abada-engine/src/main/resources/application-prod.yaml)**: Added `management.otlp.logs.endpoint`

---

### 4. Documentation Updates

#### Observability Implementation Guide
- **[observability-implementation.md](file:///home/pbashizi/IdeaProjects/abada-engine/docs/observability-implementation.md)**:
  - Updated overview to include Loki and the three pillars of observability
  - Added comprehensive "Log Aggregation with Loki" section covering:
    - Architecture diagram
    - Application configuration examples
    - OpenTelemetry Collector configuration
    - Loki configuration details
    - LogQL query examples
    - API query examples
    - Trace-to-logs correlation explanation

#### README
- **[README.md](file:///home/pbashizi/IdeaProjects/abada-engine/README.md)**:
  - Updated observability section to mention Loki
  - Added unified logging as a key feature
  - Clarified the modern stack integration (Jaeger, Prometheus, Loki, Grafana)

#### Observability Reference Guide
- **[observability-reference-guide.md](file:///home/pbashizi/IdeaProjects/abada-engine/docs/observability-reference-guide.md)**:
  - Updated overview to include centralized log aggregation
  - Added logs endpoint to environment variables
  - Added logs endpoint to application properties
  - Added comprehensive "Log Aggregation with Loki" section with:
    - Configuration examples
    - LogQL query examples
    - API query examples
    - Trace-to-logs correlation details
  - Updated OpenTelemetry Collector integration example to include Loki

---

## Architecture

The complete observability pipeline now follows this flow:

```
┌─────────────────┐
│  Spring Boot    │
│  Application    │
└────────┬────────┘
         │
         ├─── Traces ───┐
         ├─── Metrics ──┤
         └─── Logs ─────┤
                        │
                        ▼
              ┌──────────────────┐
              │ OpenTelemetry    │
              │   Collector      │
              └────────┬─────────┘
                       │
         ┌─────────────┼─────────────┐
         │             │             │
         ▼             ▼             ▼
    ┌────────┐   ┌──────────┐   ┌──────┐
    │ Jaeger │   │Prometheus│   │ Loki │
    └────────┘   └──────────┘   └──────┘
         │             │             │
         └─────────────┴─────────────┘
                       │
                       ▼
                 ┌──────────┐
                 │ Grafana  │
                 └──────────┘
```

## Key Features

### 1. Unified Observability Pipeline
All telemetry data (traces, metrics, logs) flows through a single OpenTelemetry Collector, providing:
- Consistent configuration
- Centralized processing
- Unified resource attribution

### 2. Automatic Trace Correlation
Logs automatically include `traceId` and `spanId` from the MDC context, enabling:
- Jump from traces in Jaeger to related logs in Loki
- Filter logs by specific trace IDs
- Correlate distributed operations across services

### 3. Efficient Log Storage
Loki uses label-based indexing instead of full-text indexing:
- Faster queries
- Lower storage costs
- Better performance at scale

### 4. Seamless Grafana Integration
All observability data is accessible in Grafana:
- Metrics dashboards from Prometheus
- Trace exploration from Jaeger
- Log queries from Loki
- Unified view of system behavior

## Usage Examples

### Querying Logs in Grafana

1. Navigate to Grafana Explore view
2. Select Loki as the data source
3. Use LogQL queries:

```logql
# All logs from abada-engine
{service_name="abada-engine"}

# Filter by log level
{service_name="abada-engine"} | json | level="ERROR"

# Logs for a specific trace
{service_name="abada-engine"} | json | traceId="abc123def456"
```

### Trace-to-Logs Workflow

1. Find a trace in Jaeger (http://localhost:16686)
2. Copy the trace ID
3. In Grafana Explore, query Loki:
   ```logql
   {service_name="abada-engine"} | json | traceId="<paste-trace-id>"
   ```
4. View all logs associated with that specific request

## Next Steps

To complete the verification:

1. **Rebuild the application** with new dependencies:
   ```bash
   mvn clean package -DskipTests
   docker-compose build abada-engine
   ```

2. **Restart services**:
   ```bash
   docker-compose restart otel-collector
   docker-compose restart abada-engine
   ```

3. **Configure Grafana**:
   - Add Loki as a data source (http://loki:3100)
   - Test log queries in Explore view

4. **Verify trace correlation**:
   - Make API requests to generate traces
   - Verify logs appear in Loki with trace IDs
   - Test jumping between Jaeger traces and Loki logs

## Troubleshooting

### Loki Container Issues
- **Permission errors**: Fixed by configuring proper ingester ring structure
- **Configuration errors**: Ensure `ingester.lifecycler.ring` is used instead of top-level `ring`

### OTel Collector Issues
- **Unknown exporter**: Use `otlphttp/loki` instead of `loki` exporter
- **Connection errors**: Verify Loki is running and accessible at `http://loki:3100`

### Application Issues
- **Missing dependencies**: Ensure `opentelemetry-logback-appender-1.0` and `logstash-logback-encoder` are in pom.xml
- **No logs in Loki**: Check that OTEL appender is added to logger configurations
