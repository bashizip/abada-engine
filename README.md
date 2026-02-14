# Abada Platform

A modular, culturally-rooted BPMN 2.0 process automation platform built on **Java 21** and **Spring Boot 3**. Abada provides a lightweight, embeddable workflow engine with **first-class observability** and production-ready Docker deployment for dev, test, and prod.

For a comprehensive overview of the platform, see [`docs/platform-overview.md`](docs/platform-overview.md). For a deep technical overview and deployment architecture, see [`docs/architecture/overview.md`](docs/architecture/overview.md).

---

## 🚀 Built-in Observability

Abada is designed for the modern cloud-native stack. Observability is not an add-on; it is woven into the core execution engine.

*   **OpenTelemetry Native**: The engine emits rich OTLP traces, metrics, and logs out of the box. No sidecars or agents required for basic visibility.
*   **Full-Stack Visibility**: Trace every process instance from REST API call → BPMN Element → Database Query, with correlated logs at every step.
*   **Modern Stack**: Integrated with Jaeger (Tracing), Prometheus (Metrics), Loki (Logs), and Grafana (Visualization).
*   **Actionable Metrics**: Pre-configured metrics for process duration, task throughput, and error rates.
*   **Unified Logging**: Centralized log aggregation with automatic trace correlation for debugging distributed workflows.

👉 **See the [Observability Reference Guide](docs/operations/observability.md) for full configuration details.**

---

## Project Architecture

Abada is a modular platform composed of subdirectories in this monorepo:
- `engine/`: Core BPMN execution engine (REST API)
- `orun/`: Monitoring and observability dashboard (frontend)
- `tenda/`: End-user task UI (frontend)
- `admin`: Process administration (frontend, external repo)
- `semaflow`: Natural-language to BPMN tooling (external repo)

All components are containerized and deploy independently from the root. For design, boundaries, and runtime topology, see [`docs/architecture/overview.md`](docs/architecture/overview.md).

---

## Quick Start

### 🚀 Launch Platform (Production/Demo)

The easiest way to run the full Abada Platform (Engine, Tenda, Orun, Observability) is using our automated quickstart script. You only need Docker installed.

```bash
curl -sSL https://raw.githubusercontent.com/bashizip/abada-engine/main/release/quickstart.sh | bash
```

Windows PowerShell:
```bash
irm https://raw.githubusercontent.com/bashizip/abada-engine/main/release/quickstart.ps1 | iex
```

### 💻 Development Environment

Follow these steps for local development and contribution.
Scripts are grouped by environment under `scripts/dev`, `scripts/prod`, and `scripts/test`.

**Option 1: Build & Run (Recommended)**
Builds the engine, tenda, and orun from subdirectories and launches the **complete** stack with Keycloak and Observability.
```bash
./scripts/dev/build-and-run-dev.sh
```

**Option 2: Start Only**
Starts the containers if the images are already built.
```bash
./scripts/dev/start-dev.sh
```

**Local HTTPS (mkcert)**
To avoid browser SSL warnings for `https://*.localhost`, generate and mount a locally trusted certificate.
```bash
./scripts/dev/setup-local-tls.sh docker-compose.dev.yml
```

Manual equivalent:
```bash
mkdir -p docker/traefik/certs
mkcert -install
mkcert \
  -cert-file docker/traefik/certs/localhost.pem \
  -key-file docker/traefik/certs/localhost-key.pem \
  localhost \
  "*.localhost" \
  tenda.localhost \
  orun.localhost \
  keycloak.localhost \
  traefik.localhost
```
Restart Traefik (or the full dev stack) after generating the certs.

| Service | URL | Note  |
| :--- | :--- | :--- |
| **Abada Gateway** (Traefik) | [https://localhost](https://localhost) | Entry point for all services |
| **Abada Engine** (API) | [https://localhost/api](https://localhost/api) | Swagger/Docs at `/api/swagger-ui.html`; build from `engine/` |
| **Abada Tenda** (Task UI) | [https://tenda.localhost](https://tenda.localhost) | End-user task management; build from `tenda/` |
| **Abada Orun** (Monitoring) | [https://orun.localhost](https://orun.localhost) | Process monitoring dashboard; build from `orun/` |
| **Keycloak Admin** | [https://keycloak.localhost](https://keycloak.localhost) | Identity & Access Management |
| **Swagger UI** | [https://localhost/api/swagger-ui.html](https://localhost/api/swagger-ui.html) | Interactive API documentation |
| **Grafana** | [http://localhost:3000](http://localhost:3000) | Metrics & Dashboards (admin/admin123) |
| **Jaeger** | [http://localhost:16686](http://localhost:16686) | Distributed Tracing UI |

---

## Engine Status (v0.8.4-alpha)

- **Execution Core**: BPMN 2.0 compliant; Support for User/Service/Script tasks, Parallel/XOR gateways, and Message/Signal events.
- **Persistence**: H2 for local development; PostgreSQL for production with optimized pooling.
- **Security**: OIDC-compliant authentication via **Keycloak**. JWT validation and role-based access control (RBAC) in progress.
- **High Availability**: Stateless engine design, horizontally scalable with Traefik load-balancing.
- **Capabilities**:
    - [x] BPMN 2.0 (Core)
    - [ ] CMMN (Planned)
    - [ ] DMN (Planned)

### Performance & Scalability
Preliminary benchmarks show the engine capable of handling **hundreds of transactions per second (TPS)** in a standard cluster configuration. The current bottleneck is database I/O, with plans to integrate Redis/Kafka for high-frequency eventing in future versions.

---

## API Reference

For endpoints, authentication, and examples, see [`docs/development/api.md`](docs/development/api.md). Check the engine heartbeat at `/api/v1/info`.

---

## Release Notes

*   [Version 0.8.4-alpha](docs/release-notes/0.8.4-alpha-release-notes.md) - Tenda MVP & Keycloak Integration
*   [Version 0.8.3-alpha](docs/release-notes/0.8.3-alpha-release-notes.md)
*   [Version 0.8.2-alpha](docs/release-notes/0.8.2-alpha-release-notes.md)

---

## Roadmap

See [`docs/development/roadmap.md`](docs/development/roadmap.md) for the staged milestones from `0.8.4-alpha` to `1.0.0-beta`.

---

## Contributing

This is now a monorepo containing engine, tenda, and orun. Issues and PRs are welcome for any subdirectory. Please include tests for any new behavior, and specify the component (e.g., engine, tenda, orun) in your commit messages.

## License

MIT
