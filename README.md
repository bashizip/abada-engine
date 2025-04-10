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

- [ ] BPMN start/process instance via REST
- [ ] Human task claiming and completion
- [ ] Timer and boundary events
- [ ] Process history + audit log
- [ ] Custom plugins and extensions

---

## 🧠 Philosophy

> Build your own engine, not your own prison.

Abada Engine is built for reusability, branding, and portability. Perfect for companies or integrators building tailored process solutions across customers.

---

## 📜 License

[MIT License](LICENSE)

---

## 🦄 Made with love by the Abada Team

