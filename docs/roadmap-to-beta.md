# Roadmap to Beta (1.0.0)

This document outlines the development roadmap from the current feature-complete alpha to the first public beta release.

---

### Current Version: `0.8.2-alpha`

**Status:**
-   The core engine is considered feature-solid. All major BPMN constructs are implemented and tested, including gateways (Exclusive, Inclusive, Parallel), events (Message, Timer, Signal), and service tasks (embedded and external).
-   The REST API for external task workers is complete.
-   The BPMN parser and internal data models are stable.

While the engine is powerful, it is currently a "headless" system. The next phases focus on building the necessary user interfaces and validating the engine through real-world use.

---

### 🛠️ Pre-Beta Roadmap

#### `0.8.4-alpha` → Tenda MVP (Tasklist)

*   **Goal**: Validate the user task lifecycle and APIs from an end-user perspective.
*   **Key Features**:
    *   A minimal web-based tasklist UI named "Tenda".
    *   List all active user tasks available to the current user.
    *   Provide functionality to claim and complete tasks.
    *   Display input variables for a task and allow users to submit output variables upon completion.

#### `0.8.5-alpha` → Orun MVP (Cockpit)

*   **Goal**: Validate the process monitoring and history APIs.
*   **Key Features**:
    *   A minimal web-based cockpit UI named "Orun".
    *   List all deployed process definitions.
    *   View all running and completed process instances.
    *   Display a high-level execution history or audit log for a selected process instance.

#### `0.9.0-alpha` → End-to-End Validation

*   **Goal**: Harden the engine and its APIs by running realistic business workflows through the entire ecosystem.
*   **Key Activities**:
    *   Execute complex sample processes from start to finish using:
        *   **Tenda** for all human-based tasks.
        *   **Orun** for monitoring the process state in real-time.
        *   External workers for all asynchronous service tasks.
    *   Identify and fix API inconsistencies, usability pain points, and subtle bugs discovered during this "dogfooding" phase.
    *   Stabilize all REST API contracts and DTOs.

--- 

### 🚀 `1.0.0-beta` → Early Adopter Release

*   **Goal**: Announce that the Abada Engine is feature-complete and stable enough for real-world experiments.
*   **Release Bundle**:
    *   The core Abada Engine (as a Docker image and executable JAR).
    *   A basic Java SDK for external task workers.
    *   The minimal, functional versions of the **Tenda** and **Orun** web applications.
    *   Comprehensive documentation covering all supported BPMN features, APIs, and the future roadmap.
*   **Positioning**:
    > "Abada Engine is stable enough for real experiments with user & process UIs. Feedback from early adopters is welcome as we move towards the final 1.0.0 release."
