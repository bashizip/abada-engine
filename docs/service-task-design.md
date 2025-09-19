# Service Task Design

This document outlines the semantics and implementation plan for supporting BPMN Service Tasks in the Abada Engine. The design accommodates two distinct execution modes to support both embedded and standalone (microservice) architectures.

---

### Overall Design: A Unified Approach

The core principle is to have a single, unified concept of a "Service Task" within the engine's model, but with two distinct execution strategies that are chosen based on attributes in the BPMN file.

*   **Embedded Mode**: Triggered by the `camunda:class` attribute.
*   **Standalone Mode**: Triggered by the `camunda:topic` attribute (the "External Task" pattern).

This allows a process diagram to be run in either mode without changing the diagram itself, only the implementation details of the service task.

---

### Mode 1: Embedded (The `JavaDelegate` Pattern)

#### Semantics

In this mode, the engine runs as a library within a larger Spring application. The code to be executed by the service task exists within the same application and can be called directly and synchronously.

*   **Mechanism**: The BPMN file specifies the fully qualified class name of a Java class that implements a specific `JavaDelegate` interface (e.g., `com.abada.engine.delegates.MyDelegate`).
*   **Execution**: When the process reaches the service task, the engine will:
    1.  Instantiate the specified class using reflection.
    2.  Execute a method on it, passing in the current process context (`DelegateExecution`).
    3.  Receive any new variables back from the delegate class.
    4.  Immediately continue to the next task. This is a **synchronous, non-waiting** task.

#### Implementation Plan

1.  **Create the `JavaDelegate` Interface**: Define a standard contract for all service task classes.

    ```java
    // In: src/main/java/com/abada/engine/spi/JavaDelegate.java
    public interface JavaDelegate {
        void execute(DelegateExecution execution);
    }
    ```

2.  **Create the `DelegateExecution` Context Object**: Define an interface to act as a bridge, giving the delegate safe access to the process instance's state.

    ```java
    // In: src/main/java/com/abada/engine/spi/DelegateExecution.java
    public interface DelegateExecution {
        String getProcessInstanceId();
        Map<String, Object> getVariables();
        Object getVariable(String name);
        void setVariable(String name, Object value);
    }
    ```

3.  **Update the `BpmnParser`**: Modify the parser to read `<serviceTask>` elements. If a `camunda:class` attribute is found, store this class name in a new `ServiceTaskMeta` object.

4.  **Update `ProcessInstance.advance()`**: Add a new `else if` block to the main execution loop. When a service task with a `class` is encountered, the engine will use reflection to instantiate and execute the delegate, then immediately advance to the next sequence flow.

---

### Mode 2: Standalone (The "External Task Worker" Pattern)

#### Semantics

In this mode, the engine runs as a standalone service, and the business logic exists in a separate microservice (a "worker").

*   **Mechanism**: The BPMN file specifies a `camunda:topic` attribute (e.g., `topic="charge-credit-card"`).
*   **Execution**: This is an **asynchronous, waiting** task.
    1.  When the process reaches the service task, the engine **pauses**.
    2.  It creates a "job" for the specified topic and saves it to the database.
    3.  External workers periodically poll the engine's API, asking for jobs on the topics they can handle.
    4.  The engine "locks" a job and sends it to the worker.
    5.  The worker performs its logic and then calls back to the engine's API to complete the job, which resumes the process.

#### Implementation Plan

1.  **Create `ExternalTaskEntity` and `ExternalTaskRepository`**: Create a new JPA entity and Spring Data repository to store and manage external task jobs in the database.
2.  **Update the `BpmnParser`**: Enhance the service task parsing logic to also look for and store the `camunda:topic` attribute.
3.  **Update `ProcessInstance.advance()`**: When a service task with a `topic` is encountered, the engine will create a new `ExternalTaskEntity`, save it to the database, and then pause the execution path.
4.  **Create a New `ExternalTaskController`**: This new REST controller will expose endpoints for external workers:
    *   `POST /v1/external-tasks/fetch-and-lock`: For workers to request jobs.
    *   `POST /v1/external-tasks/{taskId}/complete`: For workers to complete a job and resume a process.
    *   `POST /v1/external-tasks/{taskId}/failure`: For workers to report business errors.

---

### Summary & Recommended Order

| | **Embedded (`camunda:class`)** | **Standalone (`camunda:topic`)** |
| :--- | :--- | :--- |
| **Execution** | Synchronous | Asynchronous |
| **Coupling** | Tightly Coupled | Loosely Coupled |
| **Mechanism** | Direct Java method call (Reflection) | REST API polling (External Task Pattern) |
| **Process State** | Does not wait | Pauses and waits for completion |
| **Use Case** | Automating logic within a monolith. | Orchestrating across microservices. |

**Recommendation**: Implement the **Embedded `JavaDelegate`** mode first, as it is simpler and provides a foundation for the standalone mode.
