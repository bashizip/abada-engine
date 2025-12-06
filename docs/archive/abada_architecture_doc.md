
# Abada Platform – Architecture Documentation

## Overview

The **Abada Platform** is a modular BPMN-based process automation system. Each major component is packaged and deployed independently using Docker containers. The platform is designed to be flexible, scalable, and developer-friendly while maintaining a strong identity rooted in African culture and innovation.

---

## Modules and Responsibilities

| Module         | Description                                                                                              |
|----------------|----------------------------------------------------------------------------------------------------------|
| `abada-engine` | Core BPMN execution engine. Parses and runs BPMN 2.0 processes.                                          |
| `orun`         | Monitoring and observability module with real-time dashboards.                                           |
| `tenda`        | Frontend interface for end users to view and complete tasks.                                             |
| `admin`        | Admin panel to deploy and manage process definitions.                                                    |
| `semaflow`     | Converts natural language descriptions to valid BPMN XML using Spring AI and integrates with the engine. |
| `db`           | Central PostgreSQL or H2 database for persistence.                                                       |

---

## Deployment Architecture

All components are deployed using Docker, with optional orchestration via Docker Compose or Kubernetes.

```
[User]
   |
   v
[Admin UI] [Tasklist UI] [Orun] [semaflow]
   |         |            |       |
   --------------------------------
             |
         [Abada Engine API]
             |
         [Shared Database]
```

---

## Natural Language to BPMN (SemaFlow)

The `semaflow` module uses **Spring AI** to convert user-provided natural language into BPMN 2.0-compliant XML.

### Example Prompt

```
"When a user submits a request, it should be reviewed by a manager. If approved, notify the user. If rejected, ask for revision."
```

- Translated to BPMN via AI
- Deployed to the engine via REST
- Validated using internal schema tools

---

## Tenda (Tasklist) – Real-Time Updates

| Mode         | Description |
|--------------|-------------|
| REST         | Initial fetch of task list. |
| Polling      | Periodic updates (e.g., every 10 seconds). |
| WebSocket    | Real-time updates for task assignment/completion. |

Recommendation: Use REST + Polling for MVP, upgrade to WebSocket for production.

---

### Component Visual Identity

| Component    | Name         | Description |
|--------------|--------------|-------------|
| Mascot       | **Blue Unicorn** | Symbolizes power, intelligence, and accessibility. A friendly yet strong identity representing the spirit of Abada. |
| Monitoring   | **Orun**     | Named after the Yoruba spiritual realm. Represents real-time visibility, oversight, and process introspection. Uses a symbolic eye-shaped logo with navy and golden hues. |
| Tasklist     | **Tenda**    | Swahili for “to act” — represents action, completion, and ownership of tasks. The logo features a checklist clipboard in Abada colors. |
| NL2BPMN Tool | **SemaFlow** | From Swahili “sema” = “to speak” — turns spoken or written intent into structured flows. The logo combines a speech bubble and flowchart iconography. |

### Color Palette (Android format)

```xml
<color name="colorPrimary">#205081</color>
<color name="colorSecondary">#4AB0D9</color>
<color name="colorHighlight">#F4A300</color>
<color name="colorBackground">#FDF8F4</color>
<color name="colorTextPrimary">#1A1A1A</color>
```

---

## Future Enhancements

- Add RBAC and user identity injection from JWT
- Enable collaborative design mode in Admin
- Add telemetry dashboard in Orun
- Expand LLM model support in NL2BPMN

---

## Branding Philosophy

The Abada project is rooted in **African mythology and innovation**, focusing on:
- Accessibility
- Observability
- Intelligence
- Cultural relevance

Orun (Yoruba for "divine realm") represents visibility into all automated activities — a modern cockpit, redesigned with heritage in mind.

---
