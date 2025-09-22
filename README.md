# Abada Platform

A modular, culturally-rooted BPMN 2.0 process automation platform built on Java 21 and Spring Boot 3. Abada provides a lightweight, embeddable workflow engine and a suite of tools for monitoring, task management, and administration.

---

## Project Architecture

The Abada Platform is composed of several independent modules, each with a distinct responsibility:

| Module         | Description                                                                 |
|----------------|-----------------------------------------------------------------------------|
| `abada-engine` | Core BPMN execution engine exposed via a REST API.                          |
| `orun`         | **(Frontend)** Monitoring and observability dashboard.                      |
| `tenda`        | **(Frontend)** End-user interface for viewing and completing tasks.         |
| `admin`        | **(Frontend)** Admin panel to deploy and manage process definitions.        |
| `semaflow`     | Converts natural language descriptions to valid BPMN XML using Spring AI.   |

All components are designed to be containerized and deployed independently. For a complete overview of the project's vision, component identity, and deployment strategy, please see the **[Architecture Documentation](docs/abada_architecture_doc.md)**.

---

## Quick Start (Docker)

The fastest way to run the `abada-engine` is with Docker Compose:

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

Run `docker compose up -d`, and the engine will be available at `http://localhost:5601`.

---

## API Usage

The Abada Engine exposes a versioned REST API under the `/v1` path.

### Authentication

The API uses an identity provider pattern. All requests **must** include the following headers to define the user context:

*   `X-User`: The unique identifier of the user (e.g., `alice`).
*   `X-Groups`: A comma-separated list of groups the user belongs to (e.g., `customers,reviewers`).

### Example Flow

Here is a minimal flow to deploy a process and complete a task.

*   **1. Deploy a BPMN file**

    ```http
    POST /v1/processes/deploy
    X-User: admin
    X-Groups: administrators
    Content-Type: multipart/form-data

    file=<your_bpmn_file.bpmn>
    ```

*   **2. Start a process instance**

    ```http
    POST /v1/processes/start
    X-User: alice
    X-Groups: customers
    Content-Type: application/x-www-form-urlencoded

    processId=your-process-id
    ```

*   **3. List visible tasks**

    ```http
    GET /v1/tasks
    X-User: alice
    X-Groups: customers
    ```

*   **4. Claim and Complete a task**

    ```http
    # First, claim the task
    POST /v1/tasks/claim?taskId=<runtimeTaskId>
    X-User: alice
    X-Groups: customers

    # Then, complete it with variables
    POST /v1/tasks/complete?taskId=<runtimeTaskId>
    X-User: alice
    X-Groups: customers
    Content-Type: application/json

    {
      "goodOne": true
    }
    ```

For more details, see the **[API Documentation](docs/api-documentation.md)**.

---

## Frontend Development

The `Tenda` (task list) and `Orun` (monitoring) frontends are developed as separate Next.js applications that consume the `abada-engine` API.

We have prepared a comprehensive guide for frontend developers to get started quickly. It includes the project requirements, proposed code structure, and a list of key API files to reference.

**&rarr; [View the Frontend Development Prompt](docs/frontend-development-prompt.md)**

---

## Engine Status (0.7.0-alpha)

*   **Core:** BPMN parsing, User Tasks, Service Tasks (stub).
*   **Gateways:** Exclusive (XOR), Parallel (AND), Inclusive (OR).
*   **Events:** Message, Timer, and Signal Catch Events.
*   **Persistence:** H2 (default) or PostgreSQL.
*   **API:** Versioned REST API (`/v1`) with header-based authentication.

---

## Roadmap

See the GitHub Issues tab for the most up-to-date roadmap.

---

## Contributing

Issues and PRs are welcome. Please include tests for any new engine behavior.

## License

MIT
