# 🧠 Persistence and Recovery in Abada Engine

This document describes how Abada Engine stores runtime state (process instances and tasks) and how it recovers them after a restart.

---

## 🗃️ Where State Is Stored

- **Process Instances** are persisted in the `process_instance` table via the `ProcessInstanceEntity` class.
- **User Tasks** are stored in the `task` table via the `TaskEntity` class.
- Both are persisted automatically by the `PersistenceService` interface (currently using a JDBC-backed H2 implementation).

---

## 🔁 How Recovery Works on Startup

The `StateReloadService` handles runtime memory reconstruction by:

1. Fetching all persisted `ProcessInstanceEntity` rows.
2. Rebuilding `ProcessInstance` objects in memory (via `ProcessInstance` + `ParsedProcessDefinition`).
3. Re-parsing the associated BPMN XML for each process (stored in the DB as a string).
4. Fetching all tasks for each instance and recreating `TaskInstance` objects in memory.
5. Registering both process and tasks in the in-memory engine (used by `AbadaEngine` and `TaskManager`).

The recovery process is triggered at application startup via `@PostConstruct`.

---

## 📄 BPMN Storage Format

- Each deployed BPMN file is stored **as raw XML** in the `bpmnXml` field of the `ProcessDefinitionEntity`.
- On reload, this XML is parsed again using `BpmnParser` to reconstruct task definitions and flows.

---

## ⚠️ Limitations in `0.5.0-alpha`

- **Only user tasks are supported.**
    - No support yet for service tasks, gateways, timers, or event subprocesses.
- **All recovery is in-memory after startup.**
    - Runtime changes (e.g., new process deployment) are persisted and recovered next time.
- **Task metadata**
    - `candidateUsers` and `candidateGroups` are currently loaded as collections, but query optimizations are still minimal.
- **No historic audit log yet** (only active runtime state is persisted).

---

## ✅ What's Covered

| Feature | Supported |
|--------|------------|
| User Task recovery | ✅ |
| Process instance resumption | ✅ |
| BPMN re-parsing from DB | ✅ |
| Task visibility by user/group after restart | ✅ |
| Full test coverage for reboot scenario | ✅ (`AbadaEnginePersistenceIntegrationTest`) |

---

## 🔮 Next Steps

- Add service task execution logic and recovery
- Introduce activity history table
- Externalize BPMN viewer/editor via `GET /processes/{id}/diagram`

---

ℹ️ See `DatabaseInitializer.java` and `StateReloadService.java` for the bootstrapping logic.
