![logo](https://github.com/bashizip/abada-engine/blob/main/assets/logo_small.png)

# 🦄 Abada Engine

**Abada Engine** is a lightweight, embeddable, and cloud-native BPMN 2.0 workflow engine built in modern Java. Designed for developers and teams seeking a streamlined, flexible process automation core — without the bloat, lock-in, or complexity of legacy platforms.

> Inspired by the mythical African unicorn *Abada* — rare, agile, and powerful.

---

## 🚀 What Makes It Different?

- ✅ **Lightweight and modular** — no heavyweight runtimes
- 🧠 **Developer-first** — clean Java API, simple embedding in any JVM-based application
- 🌐 **REST-first architecture** — can also run fully standalone as a remote workflow service
- 📦 **Container-ready** — easily deployed via Docker in modern CI/CD pipelines
- ☁️ **Cloud-native mindset** — built for microservices, automation, and scale
- ⚙️ **Standard BPMN 2.0 support** — including user tasks, service tasks, and gateways
- 🔐 **Authentication agnostic** — pluggable security handled by your host app
- 🔁 **Process persistence** — reliable state recovery after reboot
- 🧪 **Battle-tested core** — strong test coverage and deterministic behavior
- 📄 **BPMN 2.0 compatible** — fully interoperable with [bpmn.io](https://bpmn.io) and Camunda Modeler

---

## 💡 Usage Modes

**1. Embedded SDK**  
Use it directly as a Java library inside your Spring Boot (or plain Java) application.

**2. Standalone Engine**  
Run as a self-contained RESTful service. Ideal for frontend clients, no-code tools, or external systems that just need an HTTP interface.

**3. Containerized**  
Deploy with Docker for maximum portability and cloud-native integration.

---

## 🛠 Tech Stack

- Java 21
- Spring Boot 3.4
- H2 (default) — switchable to PostgreSQL or others
- Maven
- Camunda BPMN Model API (for parsing only)

---

## 🧪 Current Capabilities

| Feature                    | Status         |
|---------------------------|----------------|
| BPMN 2.0 Parsing           | ✅ Fully supported (Camunda-compatible) |
| User Tasks                | ✅ Implemented |
| Service Tasks             | ✅ Implemented |
| Exclusive Gateways        | ✅ Implemented |
| Conditional Gateways      | 🔄 In progress |
| Process & Task Persistence| ✅ Implemented |
| REST API                  | ✅ Available |
| BPMN Validation           | ✅ Schema + semantic |
| JWT Auth                  | ❌ Delegated to host app for the embedded mode|
| Multi-tenancy             | 🚧 Planned |

---

## 📦 Quick Start (Standalone)

### 🐳 Run with Docker Compose

To run **Abada Engine** using Docker Compose:

1. Create a `docker-compose.yml` file:

```yaml
version: '3.8'

services:
  abada-engine:
    image: ghcr.io/bashizip/abada-engine:latest
    container_name: abada-engine
    ports:
      - "5601:5601"
    volumes:
      - .data:/app/data
    environment:
      - SPRING_PROFILES_ACTIVE=dev
      - SERVER_PORT=5601
    restart: unless-stopped

volumes:
  abada-data:
```

Then:

```bash
docker compose up -d
```

This will start the engine and expose the API at:

```
http://localhost:5601/abada/api/v1
```

### Example Endpoints

- **Deploy a BPMN process**
  ```http
  POST /abada/api/v1/processes/deploy
  Content-Type: multipart/form-data
  Body: file=<your_bpmn_file>
  ```

- **Start a process instance**
  ```http
  POST /abada/api/v1/processes/start
  Content-Type: application/x-www-form-urlencoded
  Body: processId=recipeProcess
  ```

- **Get available tasks for the user**
  ```http
  GET /abada/api/v1/tasks
  ```

- **Claim a task**
  ```http
  POST /abada/api/v1/tasks/claim?taskId=choose-recipe
  ```

- **Complete a task**
  ```http
  POST /abada/api/v1/tasks/complete?taskId=choose-recipe
  ```


## 🧠 Philosophy

> Build your own engine — not your own prison.

Abada Engine is built to be lightweight, hackable, and open. Whether you're building internal automation or selling workflow-driven platforms, Abada gives you full control — from task routing to UI integration.

---

## 📜 License

[MIT License](https://github.com/bashizip/abada-engine/blob/main/LICENCE)

---

## 🦄 Made with love by Patrick Bashizi
