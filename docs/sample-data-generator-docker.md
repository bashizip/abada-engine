# Docker Setup for Sample Data Generator

## Quick Answer

**Yes!** The `abada.generate-sample-data=true` property works in Docker. You just need to pass it as an environment variable.

## How to Enable in Docker

### Option 1: Environment Variable in docker-compose.yml

Add to the `abada-engine` service environment section:

```yaml
services:
  abada-engine:
    environment:
      - ABADA_GENERATE_SAMPLE_DATA=true
```

### Option 2: Using .env File

Create or edit `.env` file in the project root:

```bash
# .env
ABADA_GENERATE_SAMPLE_DATA=true
```

Then reference it in docker-compose.yml:

```yaml
services:
  abada-engine:
    environment:
      - ABADA_GENERATE_SAMPLE_DATA=${ABADA_GENERATE_SAMPLE_DATA:-false}
```

### Option 3: Command Line Override

```bash
# For dev environment
ABADA_GENERATE_SAMPLE_DATA=true docker-compose -f docker-compose.yml -f docker-compose.dev.yml up

# For prod environment
ABADA_GENERATE_SAMPLE_DATA=true docker-compose -f docker-compose.yml -f docker-compose.prod.yml up
```

## Complete Example: docker-compose.dev.yml

Here's what your `docker-compose.dev.yml` would look like with sample data enabled:

```yaml
version: '3.8'

services:
  abada-engine:
    build:
      context: .
      dockerfile: Dockerfile
      args:
        USE_LOCAL_JAR: "true"
    image: abada-engine:dev
    container_name: abada-engine
    environment:
      - SPRING_PROFILES_ACTIVE=dev
      - SERVER_PORT=5601
      - DB_PASSWORD=${DB_PASSWORD:-abada123}
      - MANAGEMENT_TRACING_SAMPLING_PROBABILITY=1.0
      - MANAGEMENT_OTLP_TRACING_ENDPOINT=http://otel-collector:4318/v1/traces
      - MANAGEMENT_OTLP_METRICS_ENDPOINT=http://otel-collector:4318/v1/metrics
      - OTEL_RESOURCE_ATTRIBUTES_DEPLOYMENT_ENVIRONMENT=dev
      - OTEL_SERVICE_NAME=abada-engine-dev
      - OTEL_RESOURCE_ATTRIBUTES=project=abada,service.version=0.8.3-alpha
      # Enable sample data generation
      - ABADA_GENERATE_SAMPLE_DATA=${ABADA_GENERATE_SAMPLE_DATA:-false}
    ports:
      - "5601:5601"
    volumes:
      - ./data:/app/data:z
      - ./logs:/app/logs:z
    networks:
      - abada-network
    restart: unless-stopped
    healthcheck:
      test: [ "CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:5601/abada/api/actuator/health" ]
      interval: 30s
      timeout: 10s
      retries: 3
    depends_on:
      otel-collector:
        condition: service_started
```

## Environment Variable Naming

Spring Boot automatically converts environment variables:

- `ABADA_GENERATE_SAMPLE_DATA` → `abada.generate-sample-data`
- Underscores (`_`) become dots (`.`)
- Uppercase becomes lowercase

## Usage Examples

### Development with Sample Data

```bash
# Set environment variable
export ABADA_GENERATE_SAMPLE_DATA=true

# Start dev stack
./scripts/start-dev.sh

# Or directly with docker-compose
docker-compose -f docker-compose.yml -f docker-compose.dev.yml up
```

### One-Time Sample Data Load

```bash
# Start with sample data, then stop
ABADA_GENERATE_SAMPLE_DATA=true docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d

# Wait for data to be generated (check logs)
docker logs -f abada-engine

# Once you see "Sample Data Generation Complete", you can restart without the flag
docker-compose -f docker-compose.yml -f docker-compose.dev.yml restart abada-engine
```

### Production (Disabled by Default)

```yaml
# docker-compose.prod.yml
services:
  abada-engine:
    environment:
      # Explicitly disable in production
      - ABADA_GENERATE_SAMPLE_DATA=false
```

## Verification in Docker

After starting the container with sample data enabled:

```bash
# Check logs to see sample data generation
docker logs abada-engine | grep "Sample Data"

# Expected output:
# Starting Sample Data Generation
# Deployed recipe-cook.bpmn
# Deployed parallel-gateway-test.bpmn
# Scenario 1: Completed Recipe Process
# ...
# Sample Data Generation Complete

# Verify via API
curl http://localhost:5601/abada/api/v1/processes/instances

# Check tasks
curl -H "X-User: alice" -H "X-Groups: customers" \
  http://localhost:5601/abada/api/v1/tasks
```

## Important Notes

### Data Persistence

- **H2 (Dev)**: Data persists in `./data` volume
- **PostgreSQL (Prod)**: Data persists in database
- Sample data is generated **once** on startup
- Restarting the container won't regenerate data (unless you clear the database)

### When to Use

✅ **Good for:**

- Demo environments
- Development testing
- UI development
- Integration testing

❌ **Avoid for:**

- Production environments
- Performance testing (use dedicated test data)
- Load testing

### Troubleshooting

**Sample data not appearing:**

```bash
# Check if environment variable is set
docker exec abada-engine env | grep ABADA

# Check application logs
docker logs abada-engine | grep -i "sample"

# Verify the property is recognized
docker exec abada-engine cat /app/application.properties
```

**BPMN files not found:**

```bash
# Verify BPMN files are in the image
docker exec abada-engine ls -la /app/BOOT-INF/classes/bpmn/

# Check test resources are included
docker exec abada-engine find /app -name "*.bpmn"
```

## Shell Script for Docker

Create `scripts/start-dev-with-sample-data.sh`:

```bash
#!/bin/bash
set -e

echo "Starting Abada Engine (Dev) with Sample Data..."

export ABADA_GENERATE_SAMPLE_DATA=true

./scripts/start-dev.sh

echo ""
echo "Sample data will be generated on first startup."
echo "Check logs: docker logs -f abada-engine"
```

Make it executable:

```bash
chmod +x scripts/start-dev-with-sample-data.sh
```

## Summary

The sample data generator works seamlessly in Docker by:

1. Adding `ABADA_GENERATE_SAMPLE_DATA=true` to environment variables
2. Spring Boot automatically converts it to `abada.generate-sample-data=true`
3. The generator runs on container startup
4. Data persists in the configured database/volume

For the easiest setup, add the environment variable to your `.env` file or docker-compose configuration!
