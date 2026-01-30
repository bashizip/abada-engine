# Abada Platform

A modular, culturally-rooted BPMN 2.0 process automation platform built on **Java 21** and **Spring Boot 3**. Abada provides a lightweight, embeddable workflow engine with **first-class observability** and production-ready Docker deployment for dev, test, and prod.

For a deep technical overview and deployment architecture, see [`docs/architecture/overview.md`](docs/architecture/overview.md).

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

Abada is a modular platform composed of:
- `abada-engine`: Core BPMN execution engine (REST API)
- `orun`: Monitoring and observability dashboard (frontend)
- `tenda`: End-user task UI (frontend)
- `admin`: Process administration (frontend)
- `semaflow`: Natural-language to BPMN tooling

All components are containerized and deploy independently. For design, boundaries, and runtime topology, see [`docs/architecture/overview.md`](docs/architecture/overview.md).

---

## Quick Start

## Quick Start

### 🚀 Launch Platform (Production/Demo)

The easiest way to run the full Abada Platform (Engine, Tenda, Orun, Observability) is using our automated quickstart script. You only need Docker installed.

```bash
curl -sSL https://raw.githubusercontent.com/bashizip/abada-engine/main/release/quickstart.sh | bash
```

### 💻 Development Environment

For developers contributing to the project:

Hosted with in-memory H2 database, pre-configured users, and full observability tools.

**Option 1: Build & Run (Recommended)**
Builds the engine locally (fast) and launches the **complete** stack.
```bash
./scripts/build-and-run-dev.sh
```

**Option 2: Start Only**
Starts the containers if the image is already built.
```bash
./scripts/start-dev.sh
```

**Option 3: Rebuild Engine Only (Fast Iteration)**
Rebuilds and restarts only the engine service to apply code changes.
```bash
./scripts/build-dev.sh
```

**Services:**
- **Abada Engine**: [http://localhost:5601/api](http://localhost:5601/api)
- **Abada Tenda** (Task UI): [http://localhost:5602](http://localhost:5602)
- **Abada Orun** (Monitoring UI): [http://localhost:5603](http://localhost:5603)
- **Grafana**: [http://localhost:3000](http://localhost:3000) (admin/admin123)
- **Jaeger**: [http://localhost:16686](http://localhost:16686)

For production deployment and manual Docker Compose commands, see the detailed [Docker Deployment Guide](docs/operations/docker-deployment.md).

---

## API Reference

For endpoints, authentication, and examples, see [`docs/development/api.md`](docs/development/api.md).

---

## Engine Status (0.8.4-alpha)

- **Core Execution**: BPMN parsing and execution; user, service, external, and script tasks; message/signal/timer events; XOR/AND/OR gateways.
- **Persistence**: H2 (dev/test) and PostgreSQL (prod) with HikariCP pooling.
- **Observability**: Full OpenTelemetry instrumentation. Tracing for all BPMN elements. Metrics for process/task performance.
- **Deployment**: Traefik load-balanced, stateless, horizontally scalable engine instances.
- **Security**: Header-based user context (Role model planned).

---

## Release Notes

*   [Version 0.8.4-alpha](docs/release-notes/0.8.4-alpha-release-notes.md)
*   [Version 0.8.3-alpha](docs/release-notes/0.8.3-alpha-release-notes.md)
*   [Version 0.8.2-alpha](docs/release-notes/0.8.2-alpha-release-notes.md)

---

## Roadmap

See [`docs/development/roadmap.md`](docs/development/roadmap.md) for the staged milestones from `0.8.2-alpha` to `1.0.0-beta`.

---

## Contributing

Issues and PRs are welcome. Please include tests for any new engine behavior.

## License

MIT
