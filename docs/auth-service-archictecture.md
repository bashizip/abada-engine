# Abada Engine Authentication & Authorization Architecture

This document explains how **authentication and authorization** are integrated into the **Abada Engine** ecosystem using **Keycloak** and **Traefik Gateway**.

---

## 🔐 Overview

The Abada Engine itself is **auth-agnostic**.
It only needs two identity facts at runtime:

* `username`
* `groups` (roles the user belongs to)

These values are used to evaluate BPMN attributes:

* `assignee="alice"`
* `candidateUsers="bob, charlie"`
* `candidateGroups="finance, hr"`

Authentication and group management are delegated to **Keycloak**, and security enforcement is handled by **Traefik Gateway**.

---

## ⚙️ Architecture

```mermaid
flowchart TB
    %% Clusters
    subgraph DBs [PostgreSQL Cluster]
        AE_DB[(Abada Engine DB)]
        KC_DB[(Keycloak DB)]
    end

    subgraph Identity
        KC[Keycloak]
    end

    subgraph Engine
        AE[Abada Engine]
    end

    subgraph Gateway
        GW[Traefik Gateway]
    end

    subgraph Apps
        AA[Abada Apps (Tenda, Orun)]
        UA[User Apps]
    end

    %% Flows
    U[User] --> AA
    U --> UA
    AA --> GW
    UA --> GW
    GW --> KC
    GW --> AE
    KC --> KC_DB
    AE --> AE_DB
    
    %% Authentication flows
    AA -.->|auth request| KC
    UA -.->|auth request| KC
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
    participant AA as Abada Apps (Tenda/Orun)
    participant UA as User Apps
    participant KC as Keycloak
    participant GW as Traefik Gateway
    participant AE as Abada Engine

    U->>AA: 1. Access Abada App
    U->>UA: 1a. Access User App
    AA->>KC: 2. Redirect to login (OIDC)
    UA->>KC: 2a. Redirect to login (OIDC)
    U->>KC: 3. Submit credentials
    KC-->>AA: 4. Return JWT (OIDC token)
    KC-->>UA: 4a. Return JWT (OIDC token)
    AA->>GW: 5. API call with Bearer JWT
    UA->>GW: 5a. API call with Bearer JWT
    GW->>KC: 6. Validate JWT signature
    KC-->>GW: 7. Token valid
    GW->>AE: 8. Inject X-User + X-Groups
    AE-->>GW: 9. Process response
    GW-->>AA: 10. Return data
    GW-->>UA: 10a. Return data
    AA-->>U: 11. Render UI
    UA-->>U: 11a. Render UI
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
