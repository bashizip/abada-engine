# Event-Based Gateway Architectural Design

This document outlines the semantics and implementation plan for supporting event-based gateways in the Abada Engine. This feature will enable processes to react to external stimuli, such as messages, timers, and signals.

---

### General Design & Architectural Changes

To support asynchronous events, a new architectural component is required to manage waiting process instances and trigger their resumption.

#### Proposed New Component: `EventManager`

A new Spring service named `EventManager` will be created with the following responsibilities:

1.  **Registering Wait States**: When a `ProcessInstance` reaches an event-based gateway, it will pause and register itself with the `EventManager`, indicating which event(s) it is waiting for.
2.  **Providing a Public API**: The `EventManager` will expose public methods that external systems can call to trigger events (e.g., `eventManager.correlateMessage(...)`).
3.  **Correlating and Resuming**: When an event is triggered, the `EventManager` will find the correct waiting `ProcessInstance`(s) and resume their execution by calling back into the `AbadaEngine`.

#### Changes to `ProcessInstance`

*   The `activeTokens` list will now contain the ID of the event node it's waiting at.
*   The `advance()` method will be updated to recognize when it hits an event-based gateway and will stop execution for that path, registering it as a wait state.

---

### 1. Message Event

#### Semantics
A Message Event represents a point-to-point communication. A specific process instance waits at the gateway until a named message arrives that is correlated to that instance, typically via a unique business key (e.g., an order ID).

#### Implementation Plan

1.  **`BpmnParser`**: Update the parser to recognize `<eventBasedGateway>` and its subsequent `<intermediateCatchEvent>` elements containing a `<messageEventDefinition>`. Extract the `messageRef` (the message name).
2.  **`ProcessInstance.advance()`**: When the engine encounters a message catch event, it will add the event's ID to the `activeTokens` list, effectively pausing that execution path.
3.  **`EventManager`**: 
    *   Create a public method: `correlateMessage(String messageName, String correlationKey, Map<String, Object> variables)`.
    *   Maintain an internal registry mapping `messageName -> correlationKey -> processInstanceId`.
    *   Upon receiving a message, find the correct `processInstanceId` and call the `AbadaEngine` to resume it.
4.  **`AbadaEngine`**: 
    *   Create a new method: `resumeFromEvent(String processInstanceId, String eventId, Map<String, Object> variables)`.
    *   This method will load the `ProcessInstance`, merge the incoming variables, and call `instance.advance(eventId)` to resume the correct path.

---

### 2. Timer Event

#### Semantics
A Timer Event pauses a path until a specific time is reached or a duration has passed. This is essential for handling SLAs, timeouts, or scheduled follow-ups.

#### Implementation Plan

1.  **`BpmnParser`**: Update the parser to handle `<timerEventDefinition>` and extract the timer configuration (`timeDuration`, `timeDate`, or `timeCycle`).
2.  **New Component: `JobScheduler` & `JobEntity`**:
    *   Create a new JPA `@Entity` named `JobEntity` to persist scheduled jobs (`processInstanceId`, `eventId`, `executionTimestamp`).
    *   Create a `JobRepository` (Spring Data JPA interface).
    *   Create a `JobScheduler` service.
3.  **`ProcessInstance.advance()`**: When a path reaches a timer event, it will calculate the `executionTimestamp` and call `jobScheduler.scheduleJob(...)` to save it to the database. The path will then wait.
4.  **`JobScheduler` Execution**: 
    *   The `JobScheduler` will have a Spring `@Scheduled` method that runs periodically.
    *   This method will query the `JobRepository` for any jobs that are due.
    *   For each due job, it will call `abadaEngine.resumeFromEvent(...)` and then delete the job from the database.

---

### 3. Signal Event

#### Semantics
A Signal Event is a broadcast (one-to-many) mechanism. A single signal can be received by multiple process instances that are waiting for it. This is useful for synchronizing state across different processes.

#### Implementation Plan

1.  **`BpmnParser`**: Update the parser to handle `<signalEventDefinition>` and extract the `signalRef` (the signal name).
2.  **`ProcessInstance.advance()`**: The process path stops at the signal event node.
3.  **`EventManager`**: 
    *   Create a public method: `broadcastSignal(String signalName, Map<String, Object> variables)`.
    *   Maintain an internal registry mapping `signalName -> List<processInstanceId>`.
    *   When a signal is broadcast, loop through all waiting instances and call `abadaEngine.resumeFromEvent(...)` for each one.

---

### 4. Conditional Event

#### Semantics
A Conditional Event triggers when a specific condition involving process variables becomes `true`. This allows for highly dynamic and reactive process flows.

#### Implementation Plan

1.  **`BpmnParser`**: Update the parser to handle `<conditionalEventDefinition>` and extract the condition expression.
2.  **`ProcessInstance.advance()`**: The process path stops at the conditional event node.
3.  **Evaluation Trigger**: The condition must be re-evaluated every time the process variables are updated. This logic must be placed inside the `setVariable` and `putAllVariables` methods of the `ProcessInstance` class.
4.  **`ProcessInstance` Variable Setters**: 
    *   Modify `setVariable` and `putAllVariables`.
    *   After variables are updated, check if any `activeTokens` are waiting at a conditional event.
    *   If so, re-evaluate the conditions for those events.
    *   If a condition evaluates to `true`, the engine must immediately trigger the resumption for that path (e.g., by calling `this.advance(eventId)` from within the setter).

---

### Recommended Order of Implementation

1.  **Message Event**: Introduces the core `EventManager` concept.
2.  **Signal Event**: Builds on the `EventManager` with broadcast logic.
3.  **Timer Event**: Introduces the `JobScheduler` for asynchronous, time-based events.
4.  **Conditional Event**: The most complex, requiring modifications to the core variable handling.
# Event-Based Gateway Architectural Design

This document outlines the semantics and implementation plan for supporting event-based gateways in the Abada Engine. This feature will enable processes to react to external stimuli, such as messages, timers, and signals.

---

### General Design & Architectural Changes

To support asynchronous events, a new architectural component is required to manage waiting process instances and trigger their resumption.

#### Proposed New Component: `EventManager`

A new Spring service named `EventManager` will be created with the following responsibilities:

1.  **Registering Wait States**: When a `ProcessInstance` reaches an event-based gateway, it will pause and register itself with the `EventManager`, indicating which event(s) it is waiting for.
2.  **Providing a Public API**: The `EventManager` will expose public methods that external systems can call to trigger events (e.g., `eventManager.correlateMessage(...)`).
3.  **Correlating and Resuming**: When an event is triggered, the `EventManager` will find the correct waiting `ProcessInstance`(s) and resume their execution by calling back into the `AbadaEngine`.

#### Changes to `ProcessInstance`

*   The `activeTokens` list will now contain the ID of the event node it's waiting at.
*   The `advance()` method will be updated to recognize when it hits an event-based gateway and will stop execution for that path, registering it as a wait state.

---

### 1. Message Event

#### Semantics
A Message Event represents a point-to-point communication. A specific process instance waits at the gateway until a named message arrives that is correlated to that instance, typically via a unique business key (e.g., an order ID).

#### Implementation Plan

1.  **`BpmnParser`**: Update the parser to recognize `<eventBasedGateway>` and its subsequent `<intermediateCatchEvent>` elements containing a `<messageEventDefinition>`. Extract the `messageRef` (the message name).
2.  **`ProcessInstance.advance()`**: When the engine encounters a message catch event, it will add the event's ID to the `activeTokens` list, effectively pausing that execution path.
3.  **`EventManager`**: 
    *   Create a public method: `correlateMessage(String messageName, String correlationKey, Map<String, Object> variables)`.
    *   Maintain an internal registry mapping `messageName -> correlationKey -> processInstanceId`.
    *   Upon receiving a message, find the correct `processInstanceId` and call the `AbadaEngine` to resume it.
4.  **`AbadaEngine`**: 
    *   Create a new method: `resumeFromEvent(String processInstanceId, String eventId, Map<String, Object> variables)`.
    *   This method will load the `ProcessInstance`, merge the incoming variables, and call `instance.advance(eventId)` to resume the correct path.

---

### 2. Timer Event

#### Semantics
A Timer Event pauses a path until a specific time is reached or a duration has passed. This is essential for handling SLAs, timeouts, or scheduled follow-ups.

#### Implementation Plan

1.  **`BpmnParser`**: Update the parser to handle `<timerEventDefinition>` and extract the timer configuration (`timeDuration`, `timeDate`, or `timeCycle`).
2.  **New Component: `JobScheduler` & `JobEntity`**:
    *   Create a new JPA `@Entity` named `JobEntity` to persist scheduled jobs (`processInstanceId`, `eventId`, `executionTimestamp`).
    *   Create a `JobRepository` (Spring Data JPA interface).
    *   Create a `JobScheduler` service.
3.  **`ProcessInstance.advance()`**: When a path reaches a timer event, it will calculate the `executionTimestamp` and call `jobScheduler.scheduleJob(...)` to save it to the database. The path will then wait.
4.  **`JobScheduler` Execution**: 
    *   The `JobScheduler` will have a Spring `@Scheduled` method that runs periodically.
    *   This method will query the `JobRepository` for any jobs that are due.
    *   For each due job, it will call `abadaEngine.resumeFromEvent(...)` and then delete the job from the database.

---

### 3. Signal Event

#### Semantics
A Signal Event is a broadcast (one-to-many) mechanism. A single signal can be received by multiple process instances that are waiting for it. This is useful for synchronizing state across different processes.

#### Implementation Plan

1.  **`BpmnParser`**: Update the parser to handle `<signalEventDefinition>` and extract the `signalRef` (the signal name).
2.  **`ProcessInstance.advance()`**: The process path stops at the signal event node.
3.  **`EventManager`**: 
    *   Create a public method: `broadcastSignal(String signalName, Map<String, Object> variables)`.
    *   Maintain an internal registry mapping `signalName -> List<processInstanceId>`.
    *   When a signal is broadcast, loop through all waiting instances and call `abadaEngine.resumeFromEvent(...)` for each one.

---

### 4. Conditional Event

#### Semantics
A Conditional Event triggers when a specific condition involving process variables becomes `true`. This allows for highly dynamic and reactive process flows.

#### Implementation Plan

1.  **`BpmnParser`**: Update the parser to handle `<conditionalEventDefinition>` and extract the condition expression.
2.  **`ProcessInstance.advance()`**: The process path stops at the conditional event node.
3.  **Evaluation Trigger**: The condition must be re-evaluated every time the process variables are updated. This logic must be placed inside the `setVariable` and `putAllVariables` methods of the `ProcessInstance` class.
4.  **`ProcessInstance` Variable Setters**: 
    *   Modify `setVariable` and `putAllVariables`.
    *   After variables are updated, check if any `activeTokens` are waiting at a conditional event.
    *   If so, re-evaluate the conditions for those events.
    *   If a condition evaluates to `true`, the engine must immediately trigger the resumption for that path (e.g., by calling `this.advance(eventId)` from within the setter).

---

### Recommended Order of Implementation

1.  **Message Event**: Introduces the core `EventManager` concept.
2.  **Signal Event**: Builds on the `EventManager` with broadcast logic.
3.  **Timer Event**: Introduces the `JobScheduler` for asynchronous, time-based events.
4.  **Conditional Event**: The most complex, requiring modifications to the core variable handling.
