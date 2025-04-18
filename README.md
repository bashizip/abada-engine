![logo](https://github.com/bashizip/abada-engine/blob/main/assets/logo_small.png)

# 🦄 Abada Engine

**Abada Engine** is a lightweight, pluggable BPMN-based workflow engine built in Java. Designed for developers and teams that want full control over business process automation without relying on bloated or proprietary platforms.

> Inspired by the mythical African unicorn *Abada* — rare, agile, and powerful.

---

## ✨ Features

- 🚀 Lightweight and embeddable — no heavy runtimes
- 🧹 BPMN 2.0 process execution (basic tasks, events, gateways)
- ⚖️ Java 21 and Spring Boot 3.4 support
- 🔐 Built-in support for JWT authentication
- 📃 In-memory H2 database for quick development
- 🔄 REST API for interacting with processes and tasks
- 🖼️ Ready for integration with [bpmn-js](https://bpmn.io/toolkit/bpmn-js/) or any custom UI

---

## 📦 Tech Stack

- Java 21
- Spring Boot 3.4
- H2 Database
- Custom BPMN Runtime (no Camunda/Flowable dependency)
- JWT Authentication

---

## 📄 Getting Started

```bash
git clone https://github.com/your-org/abada-engine.git
cd abada-engine
mvn spring-boot:run
```

Visit `http://localhost:8080/api` to explore the API.

---

## 🧪 Roadmap

| Task                                | Description                                                                 | Status    | Version         | Note                                                              |
|-------------------------------------|-----------------------------------------------------------------------------|-----------|------------------|-------------------------------------------------------------------|
| ✅ BPMN XML Parser (basic)          | Load BPMN 2.0 XML, extract start events, user tasks, sequence flows         | Done      | v0.1.0-alpha     | Supports minimal flow to bootstrap engine                        |
| ✅ REST API: Deploy BPMN            | Upload `.bpmn20.xml` and register definition                                | Done      | v0.1.0-alpha     | Uses `multipart/form-data`                                       |
| ✅ REST API: Start + Complete       | Start process instance, claim and complete tasks                            | Done      | v0.1.0-alpha     | MVP core loop                                                     |
| ⏳ Process History Logging          | Log transitions, task state changes per instance                            | Planned   | v0.2.0-alpha     | Use internal event model                                         |
| ✅ Assignee & Candidate Group Logic | Parse and enforce `assignee`, `candidateUsers`, `candidateGroups`           | Planned   | v0.3.0-alpha     | Enables realistic user routing logic                             |
| ⏳ Persistence (in-memory to H2)    | Persist process definitions, instances, task state                          | Planned   | v0.4.0-alpha     | Enables recovery and scaling                                     |
| ⏳ BPMN: Exclusive Gateway          | Support decision branching with `<exclusiveGateway>` and conditions         | Planned   | v0.5.0-alpha     | Add XML condition support                                        |
| ⏳ BPMN: Parallel Gateway           | Add `<parallelGateway>` split/join execution                                | Planned   | v0.5.0-alpha     | Can be simple fork/join engine with join counter                 |
| ⏳ BPMN: Sub-Process                | Support nested `<subProcess>` elements                                      | Planned   | v0.5.0-alpha     | Inline only (no call activity)                                   |
| ⏳ BPMN: Boundary Timer Event       | Add support for `<boundaryEvent>` with `<timerEventDefinition>`             | Planned   | v0.5.0-alpha     | Timer delay logic, non-interrupting not required yet             |
| ⏳ BPMN: Service Task (Stub)        | Accept `<serviceTask>` and simulate placeholder execution                   | Planned   | v0.5.0-alpha     | Log action only or trigger mock endpoint                         |
| ⏳ BPMN: Script Task (Optional)     | Support simple Java-based or mock scripting task                            | Optional  | v0.6.0-alpha     | Can be skipped or mocked for now                                 |
| ⏳ Validation: BPMN Schema Check    | Validate input BPMN files against BPMN 2.0 XSD                              | Planned   | v0.6.0-alpha     | Reject broken XML early                                          |

---

## 🧠 Philosophy

> Build your own engine, not your own prison.

Abada Engine is built for reusability, branding, and portability. Perfect for companies or integrators building tailored process solutions across customers.

---

## 📜 License

[MIT License](LICENSE)

---

## 🦄 Made with love by the Abada Team

