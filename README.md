# Abada Platform

A modular, culturally-rooted BPMN 2.0 process automation platform built on **Java 21** and **Spring Boot 3**. Abada provides a lightweight, embeddable workflow engine with **first-class observability** and production-ready Docker deployment for dev, test, and prod.

For a deep technical overview and deployment architecture, see [`docs/architecture-and-deployment-guide.md`](docs/architecture-and-deployment-guide.md).

---

## 🚀 Built-in Observability

Abada is designed for the modern cloud-native stack. Observability is not an add-on; it is woven into the core execution engine.

*   **OpenTelemetry Native**: The engine emits rich OTLP traces, metrics, and logs out of the box. No sidecars or agents required for basic visibility.
*   **Full-Stack Visibility**: Trace every process instance from REST API call → BPMN Element → Database Query, with correlated logs at every step.
*   **Modern Stack Ready**: Seamlessly integrates with **Jaeger** (traces), **Prometheus** (metrics), **Loki** (logs), and **Grafana** (visualization).
*   **Actionable Metrics**: Pre-configured metrics for process duration, task throughput, and error rates.
*   **Unified Logging**: Centralized log aggregation with automatic trace correlation for debugging distributed workflows.

👉 **See the [Observability Reference Guide](docs/observability-reference-guide.md) for full configuration details.**

---

## Project Architecture

Abada is a modular platform composed of:
- `abada-engine`: Core BPMN execution engine (REST API)
- `orun`: Monitoring and observability dashboard (frontend)
- `tenda`: End-user task UI (frontend)
- `admin`: Process administration (frontend)
- `semaflow`: Natural-language to BPMN tooling

All components are containerized and deploy independently. For design, boundaries, and runtime topology, see [`docs/abada_architecture_doc.md`](docs/abada_architecture_doc.md).

---

## Quick Start (Docker)

Get up and running in minutes with our curated Compose stacks:

- **Development**: Hot-reloading and debug ports exposed.
- **Production**: Optimized, secure, and scalable.

See [`docs/docker-deployment.md`](docs/docker-deployment.md) for commands and [`docs/docker-deployment-plan.md`](docs/docker-deployment-plan.md) for the environment strategy.

---

## API Reference

For endpoints, authentication, and examples, see [`docs/api-documentation.md`](docs/api-documentation.md).

---

## Engine Status (0.8.2-alpha)

- **Core Execution**: BPMN parsing and execution; user, service, external, and script tasks; message/signal/timer events; XOR/AND/OR gateways.
- **Persistence**: H2 (dev/test) and PostgreSQL (prod) with HikariCP pooling.
- **Observability**: Full OpenTelemetry instrumentation. Tracing for all BPMN elements. Metrics for process/task performance.
- **Deployment**: Traefik load-balanced, stateless, horizontally scalable engine instances.
- **Security**: Header-based user context (Role model planned).

---

## Release Notes

*   [Version 0.8.2-alpha](docs/0.8.2-alpha-release-notes.md)

---

## Roadmap

See [`docs/roadmap-to-beta.md`](docs/roadmap-to-beta.md) for the staged milestones from `0.8.2-alpha` to `1.0.0-beta`.

---

## Contributing

Issues and PRs are welcome. Please include tests for any new engine behavior.

## License

MIT
