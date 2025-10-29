# Docker Deployment Plan for Abada Engine with Observability Stack

## Architecture Overview

The deployment will include:
- Abada Engine instances (scalable with Traefik load balancer)
- OpenTelemetry Collector (OTLP receiver, Jaeger & Prometheus exporters)
- Jaeger (distributed tracing)
- Prometheus (metrics storage)
- Grafana (visualization)
- PostgreSQL (production database)
- Traefik (load balancer & reverse proxy)

## File Structure

```
.
├── docker-compose.yml              # Base configuration
├── docker-compose.dev.yml          # Dev overrides (H2, debug logging)
├── docker-compose.test.yml         # Test overrides (H2, reduced sampling)
├── docker-compose.prod.yml         # Prod overrides (PostgreSQL, optimized)
├── docker/
│   ├── otel-collector-config.yaml  # OTEL Collector configuration
│   ├── prometheus.yml              # Prometheus scrape configuration
│   ├── grafana/
│   │   ├── dashboards/
│   │   │   ├── abada-engine-overview.json (existing)
│   │   │   └── abada-task-details.json (existing)
│   │   └── provisioning/
│   │       ├── datasources.yml     # Auto-configure Prometheus & Jaeger
│   │       └── dashboards.yml      # Auto-load dashboards
│   └── traefik/
│       └── traefik.yml             # Traefik configuration
└── .env.example                    # Environment variables template
```

## Implementation Steps

### 1. Create Base docker-compose.yml

Core services that are common across all environments:
- OTEL Collector (port 4318 OTLP HTTP, 4317 OTLP gRPC)
- Jaeger (port 16686 UI)
- Prometheus (port 9090)
- Grafana (port 3000, pre-configured with datasources)
- Traefik (ports 80, 8080 dashboard)

Network: Create shared network `abada-network` for service communication

### 2. Create docker-compose.dev.yml

Development-specific overrides:
- Abada Engine: H2 database, verbose logging, single instance
- OTLP endpoints: `http://otel-collector:4318`
- Sampling: 100%
- Expose H2 console on /h2-console
- Mount local data volume for persistence
- No Traefik (direct port exposure)

### 3. Create docker-compose.test.yml

Test-specific overrides:
- Abada Engine: H2 in-memory database
- Reduced sampling (50%)
- Minimal logging
- Single instance
- No persistent volumes

### 4. Create docker-compose.prod.yml

Production-specific overrides:
- PostgreSQL service (port 5432, with health checks)
- Abada Engine: PostgreSQL connection, optimized logging, multiple replicas (3)
- Traefik load balancer with:
  - Round-robin load balancing
  - Health checks on /actuator/health
  - Sticky sessions disabled (stateless)
- Sampling: 10-20% (configurable)
- Resource limits (CPU, memory)
- Persistent volumes for all data stores
- Restart policies: always

### 5. Create OTEL Collector Configuration

File: `docker/otel-collector-config.yaml`

Configuration includes:
- Receivers: OTLP (HTTP 4318, gRPC 4317)
- Processors: batch, memory_limiter, resource attributes
- Exporters:
  - Jaeger (traces) → `jaeger:14250`
  - Prometheus (metrics) → expose on `:8889/metrics`
- Service pipelines for traces and metrics

### 6. Create Prometheus Configuration

File: `docker/prometheus.yml`

Scrape targets:
- OTEL Collector metrics: `otel-collector:8889`
- Abada Engine actuator: `abada-engine:5601/abada/api/actuator/prometheus`
- Prometheus self-monitoring

Retention: 15 days default, configurable

### 7. Create Grafana Provisioning

Files:
- `docker/grafana/provisioning/datasources.yml`: Auto-configure Prometheus & Jaeger datasources
- `docker/grafana/provisioning/dashboards.yml`: Auto-load from monitoring/grafana/dashboards/

Copy existing dashboards to docker/grafana/dashboards/

### 8. Create Traefik Configuration

File: `docker/traefik/traefik.yml`

Features:
- Docker provider for auto-discovery
- Health checks
- Load balancing strategy: round-robin
- Access logs for debugging
- Dashboard on port 8080

### 9. Update Application Configuration Files

**application-dev.yaml:**
- Change OTLP endpoints to use service names: `http://otel-collector:4318`

**application-prod.yaml:**
- Add PostgreSQL datasource configuration
- Update OTLP endpoints: `http://otel-collector:4318`
- Set sampling to 0.1 (10%)
- Optimize logging levels

**application-test.yaml:**
- Update OTLP endpoints
- Set sampling to 0.5 (50%)

### 10. Create Environment Files

**`.env.example`:**
```
SPRING_PROFILES_ACTIVE=dev
DB_PASSWORD=abada123
POSTGRES_PASSWORD=postgres_secure_password
GRAFANA_ADMIN_PASSWORD=admin
JAEGER_STORAGE_TYPE=memory
ABADA_ENGINE_REPLICAS=3
```

### 11. Update Dockerfile (if needed)

Ensure Dockerfile:
- Supports passing SPRING_PROFILES_ACTIVE via environment
- Exposes actuator endpoints
- Has proper health check support

### 12. Create Documentation

**`docs/docker-deployment.md`:**
- Quick start guide for each environment
- Commands:
  - Dev: `docker-compose -f docker-compose.yml -f docker-compose.dev.yml up`
  - Test: `docker-compose -f docker-compose.yml -f docker-compose.test.yml up`
  - Prod: `docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d`
- Access URLs for all services
- Scaling instructions: `docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d --scale abada-engine=5`
- Troubleshooting guide

## Key Technical Details

### Service Discovery
All services use Docker's internal DNS. Engine instances connect to:
- Database: `postgres:5432` (prod) or embedded H2 (dev/test)
- OTEL Collector: `otel-collector:4318`

### Load Balancing
Traefik configuration in docker-compose.prod.yml:
```yaml
labels:
  - "traefik.enable=true"
  - "traefik.http.routers.abada.rule=PathPrefix(`/abada`)"
  - "traefik.http.services.abada.loadbalancer.server.port=5601"
  - "traefik.http.services.abada.loadbalancer.healthcheck.path=/abada/api/actuator/health"
```

### Database Connection Pooling
Each engine instance maintains its own connection pool (HikariCP). PostgreSQL configured with:
- Max connections: 100
- Shared state via database transactions
- Connection pooling per instance: 10 connections

### Health Checks
All services have health checks:
- Abada Engine: `/abada/api/actuator/health`
- PostgreSQL: `pg_isready`
- OTEL Collector: internal health extension
- Prometheus/Grafana/Jaeger: HTTP endpoints

## Execution Order

1. Create directory structure and configuration files
2. Create base docker-compose.yml
3. Create environment-specific override files
4. Update application YAML files with service names
5. Create OTEL Collector config
6. Create Prometheus config
7. Set up Grafana provisioning
8. Create Traefik config
9. Create .env.example
10. Test each environment configuration
11. Create documentation

## Testing Strategy

After implementation:
1. Test dev environment: Single instance, H2, full observability
2. Test test environment: Verify reduced sampling, in-memory DB
3. Test prod environment: Multiple instances, PostgreSQL, Traefik load balancing
4. Verify metrics flow: Engine → OTEL Collector → Prometheus → Grafana
5. Verify traces flow: Engine → OTEL Collector → Jaeger
6. Test scaling: Add/remove engine instances dynamically
7. Test failover: Stop one engine instance, verify others continue serving

## Benefits

- Profile-based configuration (DRY principle)
- Easy local development with full observability stack
- Production-ready with load balancing and PostgreSQL
- Horizontally scalable engine instances
- Complete observability: metrics, traces, and dashboards
- Service mesh ready architecture
- Easy CI/CD integration

