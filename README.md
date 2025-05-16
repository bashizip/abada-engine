[![Build with Maven](https://github.com/bashizip/abada-engine/actions/workflows/build-test.yml/badge.svg)](https://github.com/bashizip/abada-engine/actions/workflows/build-test.yml)

![logo](https://github.com/bashizip/abada-engine/blob/main/assets/logo_small.png)

# 🦄 Abada Engine

**Abada Engine** is a lightweight, embeddable BPMN-based workflow engine written in Java. Designed for teams that want full control over business process automation — without relying on bloated or proprietary platforms.

> Inspired by the mythical African unicorn *Abada* — rare, agile, and powerful.

---

## ✨ Features

- 🚀 Lightweight and pluggable — embeddable into any Spring Boot app
- 🧠 Clean BPMN 2.0 execution model (start events, user tasks, sequence flows)
- 🧾 Support for assignee, candidate users, and groups
- 💾 Persistent process and task state with automatic recovery on reboot
- 🔁 State reload logic with BPMN XML stored directly in the database
- 🔍 REST API to deploy, start, claim, complete, and inspect processes
- 📦 Java 21 + Spring Boot 3.4 compatible
- 🖼️ Ready for integration with [bpmn-js](https://bpmn.io/toolkit/bpmn-js/) for live rendering

---

## 🚀 Getting Started

```bash
git clone https://github.com/bashizip/abada-engine.git
cd abada-engine
mvn spring-boot:run
```

Visit `http://localhost:8080/engine/processes` to list deployed processes.

---

## 🧪 Current Milestone: `v0.5.0-alpha`

- ✅ Process and task persistence using H2 (with support for JDBC)
- ✅ State reload after reboot — recover processes and tasks in memory
- ✅ Clean orchestration with `advance()` and `UserTaskPayload`
- ✅ Auto-deploy `simple-two-task.bpmn` in `dev` profile
- ✅ REST APIs:
    - `GET /engine/processes` — list deployed processes
    - `GET /engine/processes/{id}` — process metadata + BPMN XML
    - `GET /engine/processes/{id}/diagram` — raw XML for bpmn-js
- ✅ Integration test: Alice → Bob → complete flow
- ✅ `/docs/persistence.md`: internal docs on recovery & limitations

---

## 📄 Roadmap

| Status | Task                                | Description                                                       | Version      |
|--------|-------------------------------------|-------------------------------------------------------------------|--------------|
| ✅     | BPMN Parser (basic)                 | Parse XML: start, user tasks, sequence flows                      | v0.1.0-alpha |
| ✅     | Deploy via REST                     | Upload `.bpmn` via multipart                                      | v0.1.0-alpha |
| ✅     | Start / Claim / Complete            | Execute process, manage user tasks                                | v0.1.0-alpha |
| ✅     | Assignee & Candidate Logic          | Support `assignee`, `candidateUsers`, `candidateGroups`          | v0.3.0-alpha |
| ✅     | In-memory to DB persistence         | Store process instances & tasks                                   | v0.4.0-alpha |
| ✅     | Reload from DB at startup           | Rebuild engine state on reboot                                    | v0.5.0-alpha |
| ✅     | Process listing API                 | Query deployed processes via REST                                 | v0.5.0-alpha |
| 🕓     | Publish to Maven Central            | Make it a reusable Java library                                   | v0.7.0-alpha |
| 🕓     | Exclusive + Parallel Gateway        | Add condition-based routing and fork/join                         | v0.6.0-alpha |
| 🕓     | Service Task Support                | Simulate service tasks or call stubs                              | v0.6.0-alpha |
| 🕓     | Sub-processes & Events              | Add `<subProcess>`, `<boundaryEvent>`                             | v0.8.0-alpha |
| 🕓     | BPMN Schema Validation              | Validate input XML using official BPMN XSD                        | v0.6.0-alpha |
| 🕓     | History Logging                     | Track transitions & task states                                   | v0.6.0-alpha |


## 🧠 Philosophy

> Build your own engine — not your own prison.

Abada Engine is built to be lightweight, hackable, and open. Whether you're building internal automation or selling workflow-driven platforms, Abada gives you full control — from task routing to UI integration.

---

## 📜 License

[MIT License](https://github.com/bashizip/abada-engine/blob/main/LICENCE)

---

## 🦄 Made with love by Patrick Bashizi
