# Abada Engine Authentication & Authorization Architecture

This document explains how **authentication and authorization** are integrated into the **Abada Engine** ecosystem using **Keycloak** and **Traefik Gateway**.

---

## 🔐 Overview

The Abada Engine itself is **auth-agnostic**.
It only needs two identity facts at runtime:

* `username`
* `groups` (roles the user belongs to)

These values are used to evaluate BPMN attributes:
s
* `assignee="alice"`
* `candidateUsers="bob, charlie"`
* `candidateGroups="finance, hr"`

Authentication and group management are delegated to **Keycloak**, and security enforcement is handled by **Traefik Gateway**.

---

## ⚙️ Architecture

```mermaid

---
config:
  layout: elk
---
flowchart TB
    U["User"] --> AA["Abada Apps<br>Tenda, Orun"] & UA["User Apps"]
    AA --> GW["Traefik Gateway"] & KC["Keycloak"]
    UA --> GW & KC
    GW --> KC & AE["Abada Engine"]
    KC --> KC_DB[("Keycloak DB")]
    AE --> AE_DB[("Abada Engine DB")]
```

* **Keycloak** handles login, group membership, and token issuance.
* **Traefik Gateway** validates JWT tokens and injects `username + groups`.
* **Abada Engine** consumes identity info but does not implement auth logic.
* **Postgres** provides persistence (two DBs: one for Keycloak, one for Abada).

---

## 🔄 Sequence Flow

```mermaid
sequenceDiagram
    participant U as User
    participant A as Any App (Abada/User Apps)
    participant KC as Keycloak
    participant GW as Traefik Gateway
    participant AE as Abada Engine

    U->>A: 1. Access Application
    A->>KC: 2. Redirect to login (OIDC)
    U->>KC: 3. Submit credentials
    KC-->>A: 4. Return JWT token
    A->>GW: 5. API call with Bearer JWT
    GW->>KC: 6. Validate JWT signature
    KC-->>GW: 7. Token valid
    GW->>AE: 8. Forward request + Inject headers
    AE-->>GW: 9. Process response
    GW-->>A: 10. Return data
    A-->>U: 11. Render UI
```

---

## 🏗️ Components

* **Keycloak**

  * Provides login, group management, and JWT issuance.
  * Stores its state in a dedicated PostgreSQL database.

* **Traefik Gateway**

  * Validates JWTs against Keycloak.
  * Extracts claims (`sub`, `groups`) and injects them into headers.
  * Routes requests to the correct backend service.

* **Abada Engine**

  * Stateless regarding authentication.
  * Reads `X-User` and `X-Groups` headers to evaluate BPMN assignments.
  * Uses its own PostgreSQL database for process state and audit.

* **PostgreSQL Cluster**

  * `abada` database: for engine state, tasks, audit.
  * `keycloak` database: for users, groups, roles, sessions.

* **User Applications (Any App including Tenda and Orun)**

  * Frontend or backend apps that users interact with.
  * Delegate authentication to Keycloak.
  * Call Abada Engine APIs through the gateway with validated JWTs.
