# Abada Engine - Project Context

## Project Overview

**Abada Platform** is a modular, culturally-rooted **BPMN 2.0 process automation platform** built on **Java 21** and **Spring Boot 3**. It provides a lightweight, embeddable workflow engine with first-class observability and production-ready Docker deployment.

### Core Components (Monorepo)

| Component | Description | Technology |
|-----------|-------------|------------|
| `engine/` | Core BPMN execution engine with REST API | Java 21, Spring Boot 3, Maven |
| `tenda/` | End-user task management UI | React 18, TypeScript, Vite, bpmn-js |
| `orun/` | Monitoring and observability dashboard | React 19, TypeScript, Vite, recharts |
| `docker/` | Docker Compose configs, Traefik, observability stack | Docker, Traefik, Keycloak |

### Key Features

- **BPMN 2.0 Core**: User/Service/Script tasks, Parallel/XOR gateways, Message/Signal events
- **OpenTelemetry Native**: Traces, metrics, logs via OTLP to Jaeger/Prometheus/Grafana/Loki
- **Stateless Design**: Horizontally scalable with shared PostgreSQL/H2 database
- **Security**: OIDC authentication via Keycloak, OAuth2-Proxy for JWT validation
- **Load Balancing**: Traefik with HTTPS, path-based routing

---

## Building and Running

### Prerequisites

- **Java 21+** and **Maven** (for engine)
- **Node.js 18+** (for frontend components)
- **Docker** and **Docker Compose**
- **mkcert** (optional, for local HTTPS)

### Quick Start

#### Development Environment

```bash
# Build and run full stack (Engine + Tenda + Orun + Observability + Keycloak)
./scripts/dev/build-and-run-dev.sh

# Or start only if images are already built
./scripts/dev/start-dev.sh
```

#### Production Build

```bash
./scripts/prod/build-prod.sh
```

### Service URLs (Development)

| Service | URL | Credentials |
|---------|-----|-------------|
| Abada Engine API | https://localhost/api | OAuth2 via Keycloak |
| Swagger UI | https://localhost/api/swagger-ui.html | - |
| Abada Tenda | https://tenda.localhost | OAuth2 via Keycloak |
| Abada Orun | https://orun.localhost | OAuth2 via Keycloak |
| Keycloak Admin | https://keycloak.localhost | admin/admin |
| Grafana | http://localhost:3000 | admin/admin123 |
| Jaeger | http://localhost:16686 | - |
| Prometheus | http://localhost:9090 | - |
| Traefik Dashboard | http://localhost:8080 | - |

### Key Commands

#### Engine (Java/Maven)

```bash
cd engine

# Build JAR
./mvnw clean package -DskipTests

# Run tests
./mvnw test

# Run locally (dev profile)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

#### Tenda (React/TypeScript)

```bash
cd tenda

# Install dependencies
npm install

# Development server
npm run dev

# Build for production
npm run build
```

#### Orun (React/TypeScript)

```bash
cd orun

# Install dependencies
npm install

# Development server
npm run dev

# Build for production
npm run build
```

#### Docker Compose

```bash
# Full dev stack
docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d

# View logs
docker-compose logs -f abada-engine

# Stop all
docker-compose -f docker-compose.yml -f docker-compose.dev.yml down

# Scale engine (production)
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d --scale abada-engine=3
```

---

## Development Conventions

### Code Style

- **Java**: Spring Boot conventions, Lombok-friendly, package-by-feature
- **TypeScript/React**: Functional components, hooks, strict typing
- **Naming**: camelCase for variables/methods, PascalCase for classes/components

### Testing Practices

- **Engine**: JUnit 5, AssertJ, Spring Boot Test, Testcontainers (planned)
- **Frontend**: Vitest, React Testing Library (planned)
- Include tests for new features; specify component in commit messages

### Commit Messages

- Clear, concise, focused on "why" over "what"
- Specify component: `(engine)`, `(tenda)`, `(orun)`, `(docker)`, `(docs)`
- Example: `(engine) Add message correlation tracing for debugging`

### Environment Variables

Key variables in `.env` (copy from `env.example`):

```bash
# Database
DB_PASSWORD=abada123

# Keycloak
KEYCLOAK_ADMIN_USERNAME=admin
KEYCLOAK_ADMIN_PASSWORD=admin
KEYCLOAK_DB_PASSWORD=keycloak_dev_pw

# OAuth2 Proxy
OAUTH2_PROXY_CLIENT_ID=abada-frontend
OAUTH2_PROXY_CLIENT_SECRET=dev-secret
OAUTH2_PROXY_COOKIE_SECRET=<generate-32-char>

# Grafana
GRAFANA_ADMIN_PASSWORD=admin
```

### Local HTTPS Setup

To avoid browser SSL warnings for `*.localhost` domains:

```bash
./scripts/dev/setup-local-tls.sh docker-compose.dev.yml
```

This generates trusted certificates using `mkcert` and places them in `docker/traefik/certs/`.

---

## Architecture Highlights

### Observability Stack

- **OTEL Collector**: Receives OTLP traces/metrics from engine and Traefik
- **Jaeger**: Distributed tracing visualization
- **Prometheus**: Metrics storage (15-day retention)
- **Grafana**: Dashboards for metrics visualization
- **Loki + Promtail**: Log aggregation with trace correlation

### Database Strategy

| Environment | Database | Notes |
|-------------|----------|-------|
| Development | H2 (file-based) | Console at `/api/h2-console` |
| Production | PostgreSQL 15 | HikariCP pooling (10 connections/instance) |
| Keycloak | PostgreSQL 15 | Separate instance for identity data |

### API Authentication

All API endpoints require headers injected by OAuth2-Proxy:

- `X-User`: User identifier (e.g., `alice`)
- `X-Groups`: Comma-separated groups (e.g., `customers,managers`)

---

## Project Structure

```
abada-engine/
├── engine/              # Java/Spring Boot BPMN engine
│   ├── src/main/java/com/abada/engine/
│   │   ├── api/         # REST controllers
│   │   ├── core/        # BPMN execution logic
│   │   ├── persistence/ # JPA entities, repositories
│   │   ├── observability/ # Metrics, tracing
│   │   ├── parser/      # BPMN XML parsing
│   │   └── security/    # Auth, RBAC
│   ├── pom.xml
│   └── Dockerfile.engine
├── tenda/               # Task management UI
│   ├── src/
│   ├── package.json
│   └── Dockerfile
├── orun/                # Monitoring dashboard
│   ├── src/
│   ├── package.json
│   └── Dockerfile
├── docker/              # Docker configs
│   ├── traefik/
│   ├── grafana/
│   ├── prometheus.yml
│   └── otel-collector-config.yaml
├── scripts/
│   ├── dev/             # Development scripts
│   ├── prod/            # Production scripts
│   └── test/            # Test/validation scripts
├── docs/                # Documentation
│   ├── architecture/
│   ├── development/
│   └── operations/
├── docker-compose*.yml  # Compose files for all environments
└── QWEN.md              # This file
```

---

## Documentation References

- **Platform Overview**: [`docs/platform-overview.md`](docs/platform-overview.md)
- **Architecture Details**: [`docs/architecture/overview.md`](docs/architecture/overview.md)
- **API Reference**: [`docs/development/api.md`](docs/development/api.md)
- **Observability Guide**: [`docs/operations/observability.md`](docs/operations/observability.md)
- **Roadmap**: [`docs/development/roadmap.md`](docs/development/roadmap.md)
- **Release Notes**: [`docs/release-notes/`](docs/release-notes/)

---

## Current Version: 0.9.0

**Status**:
- ✅ BPMN 2.0 Core (tasks, gateways, events)
- ✅ OpenTelemetry integration
- ✅ Keycloak authentication
- ✅ Multi-environment Docker deployment
- 🚧 RBAC (in progress)
- 🚧 CMMN/DMN (planned)
