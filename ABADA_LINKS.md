# Abada Platform Service Links

This document provides a quick reference for all the accessible service links in the Abada development environment.

## Main Application Gateways (HTTPS)

| Service | URL | Description |
| :--- | :--- | :--- |
| **Abada Gateway** | [https://localhost](https://localhost) | Main entry point (redirects to API Info) |
| **Abada Engine API** | [https://localhost/api](https://localhost/api) | Core BPMN execution engine API |
| **Swagger UI** | [https://localhost/api/swagger-ui.html](https://localhost/api/swagger-ui.html) | Interactive API documentation |
| **Abada Tenda** | [https://tenda.localhost](https://tenda.localhost) | Task Management UI (Protected by Auth) |
| **Abada Orun** | [https://orun.localhost](https://orun.localhost) | Operations Cockpit (Protected by Auth) |
| **Keycloak Admin** | [https://keycloak.localhost](https://keycloak.localhost) | Identity & Access Management console |
| **Traefik Dashboard** | [https://traefik.localhost/dashboard/](https://traefik.localhost/dashboard/) | Traefik routing & proxy overview |

## Monitoring & Infrastructure (Local Ports)

| Service | URL | Description |
| :--- | :--- | :--- |
| **Grafana** | [http://localhost:3000](http://localhost:3000) | Metrics dashboards and visualization |
| **Prometheus** | [http://localhost:9090](http://localhost:9090) | Metrics collection and alerting engine |
| **Jaeger UI** | [http://localhost:16686](http://localhost:16686) | Distributed tracing visualization |
| **Consul UI** | [http://localhost:8500](http://localhost:8500) | Service discovery and KV store |
| **Loki API** | [http://localhost:3100](http://localhost:3100) | Log aggregation system (Ready endpoint) |

---
**Note:** All `.localhost` domains require the Abada Traefik container to be running.
