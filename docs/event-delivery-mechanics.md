# Event Delivery Mechanics

This document outlines the various architectural patterns and mechanics that can be used to deliver external events (specifically Message Events) to a running process instance in the Abada Engine.

---

### The Central Entry Point: `EventManager`

The core of the event system is the `EventManager` component. Regardless of the chosen delivery mechanism, the ultimate goal is always to call the following Java method:

```java
// In EventManager.java
public void correlateMessage(String messageName, String correlationKey, Map<String, Object> variables)
```

This method is the single, authoritative entry point for resuming a process that is waiting for a specific message. The architectural choice, therefore, is how to get the required information (`messageName`, `correlationKey`, etc.) from an external system into a component that can make this method call.

Below are several common patterns, each with its own use cases and trade-offs.

---

### 1. REST API

This is the most direct, web-friendly approach and is already implemented in the `EventController`.

-   **How it Works**: An external system makes a synchronous HTTP `POST` request to a dedicated endpoint (e.g., `/api/v1/events/messages`). The request body contains the message payload in JSON format.
-   **Best for**: 
    -   Simple web-based integrations.
    -   Administrative UIs or dashboards that need to manually trigger a process.
    -   Systems where a simple, synchronous request/response model is sufficient.

### 2. Message Queues (MQ)

This is the most robust and common pattern for asynchronous, decoupled communication between microservices.

-   **Technologies**: RabbitMQ, ActiveMQ, AWS SQS, Google Cloud Pub/Sub.
-   **How it Works**:
    1.  An external service publishes a message to a specific queue or topic (e.g., a queue named `order-payments`).
    2.  You create a listener component within the Abada Engine application using an annotation like `@RabbitListener`.
    3.  When a message arrives, the listener method is automatically invoked, parses the payload, and calls `eventManager.correlateMessage()`.
-   **Best for**:
    -   **Reliability**: MQs guarantee message delivery, even if the engine is temporarily offline.
    -   **Decoupling**: The sending service has no knowledge of the engine's location or API.
    -   **Load Balancing**: Easily distributes messages across multiple engine instances.

### 3. Event Streaming Platforms

This pattern is ideal for high-throughput, ordered, and replayable streams of events.

-   **Technologies**: Apache Kafka, AWS Kinesis.
-   **How it Works**:
    1.  An external service produces an event to a specific Kafka topic (e.g., `payment-events`).
    2.  You create a Kafka consumer in the engine application using `@KafkaListener`.
    3.  The listener consumes events, filters for the ones it cares about, and calls `eventManager.correlateMessage()`.
-   **Best for**:
    -   **High-Volume Data**: Systems that generate a massive number of events in real-time.
    -   **Event Sourcing**: When you need a persistent, replayable log of everything that has happened.
    -   **Stream Processing**: When multiple different services need to react to the same stream of events.

### 4. gRPC (Remote Procedure Call)

This provides a high-performance, tightly coupled way for internal services to communicate.

-   **How it Works**:
    1.  You define a formal service contract using a `.proto` file, specifying a `CorrelateMessage` RPC.
    2.  You implement a gRPC server endpoint within the Abada Engine application that simply delegates the call to `eventManager.correlateMessage()`.
    3.  Other internal microservices use a gRPC client to call this method directly.
-   **Best for**:
    -   **High-Performance Internal Communication**: Much faster than REST/JSON for server-to-server calls.
    -   **Strongly-Typed APIs**: The contract ensures the client and server are always in sync.

### 5. Database Polling (The "Outbox" Pattern)

This is a very robust, albeit less direct, pattern often used for integrating with legacy systems or ensuring transactional consistency.

-   **How it Works**:
    1.  The external system writes a record to a specific table in a shared database (e.g., an `EVENTS_OUTBOX` table).
    2.  A new `@Scheduled` method in the Abada Engine application periodically polls this table for unprocessed events.
    3.  For each event it finds, it calls `eventManager.correlateMessage()` and marks the row as processed.
-   **Best for**:
    -   **Legacy System Integration**: When the other system can only write to a database.
    -   **Extreme Reliability**: Can be made fully transactional, guaranteeing that an event is processed if and only if the corresponding business transaction was committed.

### Summary Table

| Mechanic | How it Works | Best For |
| :--- | :--- | :--- |
| **REST API** | Synchronous HTTP `POST` request. | Simple, web-based integrations, and administrative UIs. |
| **Message Queues** | Asynchronous message sent to a broker. | Reliable, decoupled communication between microservices. |
| **Event Streams** | Asynchronous event published to a log. | High-throughput, real-time event processing. |
| **gRPC** | Direct, high-performance remote method call. | Low-latency, internal server-to-server communication. |
| **Database Polling** | Writing a record to a shared database table. | Legacy system integration and extreme transactional reliability. |
| **Direct Java Call** | `@Autowired` the `EventManager` directly. | Embedded engine use within a monolithic application. |
