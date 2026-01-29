# Build Scripts

This directory contains helper scripts for building and testing the Abada Engine.

## Quick Start

**Development (fast iteration):**

```bash
./scripts/dev-build.sh
```

**Production (self-contained build):**

```bash
./scripts/prod-build.sh
```

## Development Scripts

### `dev-build.sh`

Optimized for **fast local development** with quick iteration cycles.

**Usage:**

```bash
./scripts/dev-build.sh
```

**What it does:**

1. Builds JAR locally using Maven wrapper (`./mvnw`) - **~9 seconds**
2. Builds Docker image using pre-built JAR - **~5 seconds**
3. Starts dev environment with docker-compose
4. Displays service URLs

**Build Strategy:**

- ✅ Uses local Maven cache (`~/.m2`) - dependencies downloaded once
- ✅ Skips Maven in Docker (sets `USE_LOCAL_JAR=true`)
- ✅ Fast rebuilds when code changes
- ⚠️ Requires local Maven/JDK setup

**When to use:** Daily development, testing code changes

---

### `prod-build.sh`

Builds a **self-contained production image** from source.

**Usage:**

```bash
# Build and start
./scripts/prod-build.sh

# Build only (don't start)
./scripts/prod-build.sh --build-only
```

### `push-to-dockerhub.sh`

Builds production-ready images (using `Dockerfile.prod` files) and pushes them to Docker Hub.

**Usage:**

```bash
# Push to default user (bashizip) as latest
./scripts/push-to-dockerhub.sh

# Push to custom user with custom version
export DOCKER_USERNAME=myorg
export VERSION=v1.0.0
./scripts/push-to-dockerhub.sh
```

**Prerequisites:**
- You must be logged in to Docker Hub (`docker login`)
- You must have permissions to push to the target repositories

**What it does:**
1. Builds production images for Engine, Tenda, and Orun
2. Pushes them to Docker Hub with `${VERSION}` and `:latest` tags


**What it does:**

1. Builds Docker image from source (Maven runs inside Docker)
2. Downloads all dependencies inside Docker
3. Optionally starts production stack

**Build Strategy:**

- ✅ Self-contained (no local dependencies needed)
- ✅ Maven dependencies cached in Docker layers
- ✅ Reproducible builds
- ⏱️ First build: ~2-3 minutes (downloads dependencies)
- ⏱️ Subsequent builds: ~30 seconds (if `pom.xml` unchanged)

**When to use:** Production deployments, CI/CD pipelines, clean builds

---

### `generate_traffic.sh`

Generates HTTP traffic for testing observability (Jaeger traces, metrics).

**Usage:**

```bash
./scripts/generate_traffic.sh
```

**What it does:**

- Makes 20 iterations of requests to health, API, and tasks endpoints
- Displays HTTP status codes
- Useful for verifying Jaeger tracing and metrics collection

---

### `generate-sample-data.sh`

Generates sample process instances to preload the engine with test data.

**Usage:**

```bash
./scripts/generate-sample-data.sh
```

**What it does:**

- Starts the Abada Engine with sample data generation enabled
- Deploys `recipe-cook.bpmn` and `parallel-gateway-test.bpmn`
- Creates 6 process instances in various states:
  - Completed processes
  - In-progress processes at different stages
  - Processes with different variable values
  - Failed/looped scenarios

**When to use:**

- Setting up demo environments
- Testing UI with realistic data
- Development and testing

See [docs/sample-data-generator.md](../docs/sample-data-generator.md) for detailed documentation.

---
docke
## Docker Build Architecture

The single `Dockerfile` supports both dev and prod builds via the `USE_LOCAL_JAR` build argument.

### Production Build (Default)

```dockerfile
# Dockerfile behavior when USE_LOCAL_JAR=false (default)
ARG USE_LOCAL_JAR=false

# Step 1: Copy pom.xml
COPY pom.xml .

# Step 2: Download dependencies (CACHED LAYER)
RUN ./mvnw dependency:go-offline

# Step 3: Copy source and build
COPY src ./src
RUN ./mvnw clean package spring-boot:repackage
```

**Docker Layer Caching:**

- Layer 1: Base Maven image (cached)
- Layer 2: `pom.xml` copy (cached until pom changes)
- Layer 3: **Dependency download** (cached until pom changes) ⭐
- Layer 4: Source copy (invalidated on code change)
- Layer 5: Build (runs on every code change)

**Result:** Dependencies only download once, subsequent builds are fast!

### Development Build

```dockerfile
# Dockerfile behavior when USE_LOCAL_JAR=true
ARG USE_LOCAL_JAR=true

# Skips Maven entirely, copies pre-built JAR from context
COPY target/ ./target/
```

**Result:** No Maven in Docker, uses local build (fastest)

---

## Build Performance Comparison

| Scenario | Dev Build | Prod Build |
|----------|-----------|------------|
| **First build** | ~14s (local Maven + Docker) | ~3min (download deps in Docker) |
| **Code change** | ~14s (rebuild JAR + Docker) | ~30s (use cached deps) |
| **No changes** | ~5s (Docker only) | ~5s (all cached) |
| **pom.xml change** | ~14s (local Maven cache) | ~3min (re-download deps) |

---

## Configuration Files

### `docker-compose.dev.yml`

```yaml
build:
  args:
    USE_LOCAL_JAR: "true"  # Use pre-built JAR
```

### `docker-compose.prod.yml`

```yaml
build: .  # USE_LOCAL_JAR defaults to false
```

---

## Troubleshooting

**Dev build fails with "jar not found":**

- Run `./mvnw clean package spring-boot:repackage -DskipTests` first
- Or just use `./scripts/dev-build.sh` which does this automatically

**Prod build is slow:**

- First build downloads all dependencies (~2-3 min) - this is normal
- Subsequent builds should be fast if `pom.xml` hasn't changed
- Check Docker layer cache: `docker history abada-engine:latest`

**Dependencies not caching in prod:**

- Ensure `pom.xml` isn't changing between builds
- Check `.dockerignore` isn't excluding `pom.xml`
- Clear Docker cache: `docker builder prune`
