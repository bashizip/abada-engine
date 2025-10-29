# Abada Platform

A modular, culturally-rooted BPMN 2.0 process automation platform built on Java 21 and Spring Boot 3. Abada provides a lightweight, embeddable workflow engine with first-class observability and production-ready Docker deployment for dev, test, and prod.

For a deep technical overview and deployment architecture, see `docs/architecture-and-deployment-guide.md`.

---

## Project Architecture

Abada is a modular platform composed of:
- `abada-engine`: Core BPMN execution engine (REST API)
- `orun`: Monitoring and observability dashboard (frontend)
- `tenda`: End-user task UI (frontend)
- `admin`: Process administration (frontend)
- `semaflow`: Natural-language to BPMN tooling

All components are containerized and deploy independently. For design, boundaries, and runtime topology, see `docs/abada_architecture_doc.md` and `docs/architecture-and-deployment-guide.md`.

---

## Quick Start (Docker)

Use the curated Compose stacks for dev, test, and prod:
- Quick start and commands: `docs/docker-deployment.md`
- Design and environment strategy: `docs/docker-deployment-plan.md`
- Full architecture and ops guidance: `docs/architecture-and-deployment-guide.md`

---

## API Reference

For endpoints, authentication, and examples, see `docs/api-documentation.md`.

---

## Frontend Development

The `Tenda` (tasks) and `Orun` (monitoring) apps are separate Next.js frontends consuming the engine API. See `docs/frontend-development-prompt.md` to get started.

---

## Engine Status (0.8.2-alpha)

- **Core execution**: BPMN parsing and execution; user, service, external, and script tasks; message/signal/timer events; XOR/AND/OR gateways
- **Persistence**: H2 (dev/test) and PostgreSQL (prod) with HikariCP pooling
- **Observability**: Micrometer + OpenTelemetry (Jaeger, Prometheus, Grafana) with process, task, and event metrics and spans
- **Deployment**: Docker Compose stacks for dev/test/prod; Traefik load-balanced, stateless, horizontally scalable engine instances
- **Security**: Header-based user context; role model and fine-grained auth planned

---

## Release Notes

*   [Version 0.8.2-alpha](docs/0.8.2-alpha-release-notes.md)

---

## Roadmap

See the GitHub Issues tab for the most up-to-date roadmap.

---

## Contributing

Issues and PRs are welcome. Please include tests for any new engine behavior.

## License

MIT
