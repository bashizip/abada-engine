# Docker Deployment Guide for Abada Engine

This guide covers deploying the Abada Engine with a complete observability stack using Docker Compose.

## Architecture Overview

The deployment includes:

- **Abada Engine**: BPMN process engine (scalable instances)
- **OpenTelemetry Collector**: Receives and routes telemetry data
- **Jaeger**: Distributed tracing visualization
- **Prometheus**: Metrics storage and querying
- **Grafana**: Observability dashboards
- **PostgreSQL**: Production database
- **Traefik**: Load balancer and reverse proxy

## Quick Start

### Prerequisites

- Docker and Docker Compose installed
- At least 4GB RAM available
- Ports 80, 3000, 4318, 5432, 5601, 8080, 9090, 16686 available

### Environment Setup

1. Copy the environment template:

```bash
cp env.example .env
```

2. Edit `.env` with your preferred settings:

```bash
# For development
SPRING_PROFILES_ACTIVE=dev
GRAFANA_ADMIN_PASSWORD=admin123
POSTGRES_PASSWORD=secure_password
```

## Building Local Images

You can build the Docker image locally for either production or development.

### Production Build

The standard build is a multi-stage process that compiles the code inside the container. This ensures a consistent build environment but takes longer.

```bash
docker build -t abada-engine:latest .
```

### Development Build (Fast)

For faster iteration, you can build the JAR locally and inject it into the image. This skips the dependency download and build steps inside Docker.

1. Build the JAR file locally:

   ```bash
   ./mvnw clean package -DskipTests
   ```

2. Build the Docker image using the local JAR:

   ```bash
   docker build --build-arg USE_LOCAL_JAR=true -t abada-engine:dev .
   ```

> [!TIP]
> **Helper Scripts available!**
> You can automate this process using the scripts in the `scripts/` directory:
> - `scripts/build-and-run-dev.sh`: Builds JAR, builds image, and starts full stack.
> - `scripts/build-dev.sh`: Rebuilds engine only (useful for iteration).


## Deployment Commands

### Development Environment

Single instance with H2 database, full observability, debug logging:

```bash
docker compose -f docker-compose.yml -f docker-compose.dev.yml up
```

**Access URLs:**

- Abada Engine: <http://localhost:5601/abada/api>
- Abada Tenda: <http://localhost:5602>
- Abada Orun: <http://localhost:5603>
- H2 Console: <http://localhost:5601/abada/api/h2-console>
- Grafana: <http://localhost:3000> (admin/admin123)
- Jaeger: <http://localhost:16686>
- Prometheus: <http://localhost:9090>

### Test Environment

Single instance with in-memory H2, reduced sampling:

```bash
docker-compose -f docker-compose.yml -f docker-compose.test.yml up
```

### Production Environment

Multiple instances with PostgreSQL, load balancing, optimized settings:

```bash
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

**Access URLs:**

- Abada Engine: <http://localhost/abada/api> (via Traefik)
- Traefik Dashboard: <http://localhost:8080>
- Grafana: <http://localhost:3000>
- Jaeger: <http://localhost:16686>
- Prometheus: <http://localhost:9090>

## Scaling

### Scale Engine Instances

```bash
# Scale to 5 instances
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d --scale abada-engine=5

# Scale down to 2 instances
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d --scale abada-engine=2
```

### View Running Instances

```bash
docker-compose -f docker-compose.yml -f docker-compose.prod.yml ps
```

## Monitoring and Observability

### Metrics Flow

1. **Abada Engine** → OpenTelemetry Collector (OTLP)
2. **OpenTelemetry Collector** → Prometheus (metrics)
3. **Prometheus** → Grafana (visualization)

### Traces Flow

1. **Abada Engine** → OpenTelemetry Collector (OTLP)
2. **OpenTelemetry Collector** → Jaeger (traces)
3. **Jaeger** → Grafana (trace visualization)

### Key Metrics

- `abada_process_instances_started` - Process instance creation rate
- `abada_process_instances_completed` - Process completion rate
- `abada_tasks_created` - Task creation rate
- `abada_tasks_completed` - Task completion rate
- `abada_events_published` - Event publication rate
- `abada_events_correlated` - Event correlation success rate

### Dashboards

Pre-configured dashboards available in Grafana:

- **Abada Engine Overview**: High-level process and task metrics
- **Task Details**: Detailed task performance and timing

## Database Configuration

### Development/Test

- Uses embedded H2 database
- Data persisted in `./data` directory
- H2 console available at `/h2-console`

### Production

- Uses PostgreSQL 15
- Connection pooling: 10 connections per instance
- Database: `abada_engine`
- User: `abada`
- Password: Set via `POSTGRES_PASSWORD` environment variable

## Load Balancing

### Traefik Configuration

Production environment uses Traefik for load balancing:

- **Strategy**: Round-robin
- **Health Checks**: `/abada/api/actuator/health`
- **Path**: `/abada` prefix
- **Sticky Sessions**: Disabled (stateless design)

### Health Checks

All services include health checks:

- **Abada Engine**: HTTP health endpoint
- **PostgreSQL**: `pg_isready` command
- **OTEL Collector**: Internal health extension
- **Prometheus/Grafana/Jaeger**: HTTP endpoints

## Troubleshooting

### Common Issues

#### 1. Port Conflicts

```bash
# Check which ports are in use
netstat -tulpn | grep :5601
# Stop conflicting services or change ports in docker-compose files
```

#### 2. Database Connection Issues

```bash
# Check PostgreSQL logs
docker-compose logs postgres

# Check Abada Engine logs
docker-compose logs abada-engine
```

#### 3. Memory Issues

```bash
# Check Docker memory usage
docker stats

# Increase Docker memory limit in Docker Desktop settings
```

#### 4. Telemetry Not Appearing

```bash
# Check OTEL Collector logs
docker-compose logs otel-collector

# Verify OTLP endpoints are accessible
curl http://localhost:4318/v1/metrics
```

### Logs

View logs for specific services:

```bash
# All services
docker-compose logs

# Specific service
docker-compose logs abada-engine
docker-compose logs otel-collector
docker-compose logs prometheus
```

### Debug Mode

Enable debug logging for development:

```bash
# Set in .env file
MANAGEMENT_TRACING_SAMPLING_PROBABILITY=1.0
```

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | `dev` | Spring profile (dev/test/prod) |
| `DB_PASSWORD` | `abada123` | H2 database password |
| `POSTGRES_PASSWORD` | `postgres_secure_password` | PostgreSQL password |
| `GRAFANA_ADMIN_PASSWORD` | `admin` | Grafana admin password |
| `ABADA_ENGINE_REPLICAS` | `3` | Number of engine instances (prod) |
| `MANAGEMENT_TRACING_SAMPLING_PROBABILITY` | `1.0` | Trace sampling rate |

## Security Considerations

### Production Deployment

1. **Change Default Passwords**:

   ```bash
   POSTGRES_PASSWORD=your_secure_password
   GRAFANA_ADMIN_PASSWORD=your_secure_password
   ```

2. **Use Secrets Management**:
   - Consider using Docker secrets
   - Use external secret management systems

3. **Network Security**:
   - Use custom networks
   - Implement firewall rules
   - Consider using reverse proxy with SSL

4. **Resource Limits**:
   - Set appropriate CPU/memory limits
   - Monitor resource usage

## Backup and Recovery

### Database Backup

```bash
# PostgreSQL backup
docker-compose exec postgres pg_dump -U abada abada_engine > backup.sql

# Restore
docker-compose exec -T postgres psql -U abada abada_engine < backup.sql
```

### Configuration Backup

```bash
# Backup all configuration files
tar -czf abada-config-backup.tar.gz docker/ *.yml env.example
```

## Performance Tuning

### Database Optimization

1. **Connection Pooling**: Adjust HikariCP settings in `application-prod.yaml`
2. **Query Optimization**: Monitor slow queries in logs
3. **Indexing**: Add appropriate database indexes

### JVM Tuning

Add JVM options to Dockerfile or docker-compose:

```yaml
environment:
  - JAVA_OPTS=-Xms512m -Xmx1g -XX:+UseG1GC
```

### Monitoring Tuning

1. **Sampling Rate**: Adjust based on load
2. **Retention**: Configure Prometheus retention
3. **Scraping Interval**: Balance between detail and performance

## CI/CD Integration

### Build and Deploy

```bash
# Build image
docker build -t abada-engine:latest .

# Deploy to production
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

### Health Checks

```bash
# Check all services are healthy
docker-compose ps

# Check specific service health
curl http://localhost:5601/abada/api/actuator/health
```

## Support

For issues and questions:

1. Check the troubleshooting section above
2. Review service logs
3. Check the project documentation
4. Create an issue in the project repository
