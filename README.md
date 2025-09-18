# Abada Engine

Abada Engine is a lightweight, embeddable BPMN 2.0 workflow engine for Java. It focuses on a small, predictable core you can run in-process or expose over HTTP.

---

## Overview

* **Language/Runtime:** Java 21, Spring Boot 3
* **Persistence:** H2 by default (switchable)
* **Parsing:** Camunda BPMN Model API (parsing only)
* **Use modes:** library (embedded), standalone REST, or Docker

---

## Current status (0.7.0-alpha)

* BPMN parsing
* User Tasks and Service Tasks (stub)
* **Gateways**:
    * Exclusive Gateway (XOR)
    * Parallel Gateway (AND) for forking and joining
    * Inclusive Gateway (OR) for conditional forking and joining
* **Events**:
    * Message Catch Event (point-to-point correlation)
    * Timer Catch Event (persistent, scheduled delays)
    * Signal Catch Event (broadcast to multiple instances)
* Process & task persistence
* REST APIs for processes and tasks
* Validation (schema + basic semantics)

---

## Quick start

### Run with Docker Compose

```yaml
version: '3.8'
services:
  abada-engine:
    image: ghcr.io/bashizip/abada-engine:latest
    container_name: abada-engine
    ports:
      - "5601:5601"
    environment:
      - SPRING_PROFILES_ACTIVE=dev
      - SERVER_PORT=5601
    restart: unless-stopped
```

Then:

```bash
docker compose up -d
```

API base: `http://localhost:5601/abada/api/v1`

### Minimal API flow

* **Deploy** a BPMN file

  ```http
  POST /abada/api/v1/processes/deploy
  Content-Type: multipart/form-data
  Body: file=<your_bpmn_file>
  ```
* **Start** a process instance

  ```http
  POST /abada/api/v1/processes/start
  Content-Type: application/x-www-form-urlencoded
  Body: processId=recipe-cook
  ```
* **List** visible tasks for the current user

  ```http
  GET /abada/api/v1/tasks
  ```
* **Claim** a task

  ```http
  POST /abada/api/v1/tasks/claim?taskId=<runtimeTaskId>
  ```
* **Complete** a task (with variables used by gateways)

  ```http
  POST /abada/api/v1/tasks/complete?taskId=<runtimeTaskId>
  Content-Type: application/json
  {
    "goodOne": true
  }
  ```

---

## Design highlights

* Small, testable core (`ProcessInstance.advance(...)`) with clear token movement
* Explicit variable merge *before* advancement so gateways see inputs
* Condition evaluation supports Camunda‑style `${...}` and simple JS
* Deterministic gateway selection: first matching condition, else default, else error

More details: see `docs/exclusive-gateway.md`.

---

## Roadmap

**Near term (0.8.x)**

* Service Task execution SPI (replace stub)
* Process instance history (audit trail)
* Conditional Event support
* Publish artifacts to Maven Central

**Medium term**

* Lightweight web dashboard
* Improved error handling and problem details in REST
* Pluggable expression engine (MVEL/JEXL) behind an interface

**Open issues / ideas**

* Process variables API improvements
* Full support for BPMN event sub-processes

See the GitHub Issues tab for up‑to‑date items.

---

## Contributing

* Issues and PRs are welcome.
* Please include tests for engine behavior (advance, gateways, API) when possible.

## License

MIT
