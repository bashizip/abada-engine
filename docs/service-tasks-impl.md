# Using Service Tasks

This document explains how to implement and use Service Tasks in the Abada Engine. Service Tasks are used to automate steps in your process, either by executing Java code directly or by creating jobs for external workers.

---

### Mode 1: Embedded (`camunda:class`)

This pattern is used when the engine is embedded as a library within a larger Spring application. The business logic is implemented as a Java class within the same application and is executed synchronously.

#### How to Implement

**1. Create a JavaDelegate Class**

Create a Java class that implements the `com.abada.engine.spi.JavaDelegate` interface. This class must have a public, no-argument constructor.

```java
package com.example.delegates;

import com.abada.engine.spi.JavaDelegate;
import com.abada.engine.spi.DelegateExecution;

public class ChargeCreditCardDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String customerId = (String) execution.getVariable("customerId");
        Double amount = (Double) execution.getVariable("amount");

        System.out.println("Charging card for customer " + customerId + " for amount " + amount);

        execution.setVariable("paymentConfirmed", true);
    }
}
```

**2. Reference the Class in your BPMN File**

In your BPMN diagram, add a Service Task and set the `camunda:class` attribute to the fully qualified name of your delegate class.

```xml
<bpmn:serviceTask
    id="ChargeCreditCardTask"
    name="Charge Credit Card"
    camunda:class="com.example.delegates.ChargeCreditCardDelegate" />
```

When the process reaches this task, the engine will automatically instantiate and execute your class, then immediately proceed to the next task.

---

### Mode 2: Standalone / External (`camunda:topic`)

This pattern is used when the engine is running as a standalone service and the business logic resides in external microservices, often called "workers". This is an asynchronous, polling-based pattern.

#### How it Works

1.  **BPMN Configuration**: In your BPMN diagram, you define a Service Task with a `camunda:topic` attribute. This topic name is like a queue for a specific type of work.

    ```xml
    <bpmn:serviceTask
        id="ChargeCreditCardTask"
        name="Charge Credit Card"
        camunda:topic="credit-card-charges" />
    ```

2.  **Engine Behavior**: When the process reaches this task, it pauses and creates a persistent job for the `credit-card-charges` topic in the database.

3.  **Worker Implementation**: You must build a separate application (the worker) that periodically polls the Abada Engine for jobs.

#### The Polling Mechanism: Pull, Don't Push

A crucial aspect of the external task pattern is that the worker **pulls** for work; the engine does not **push** it. The worker is a client that continuously asks the engine if there are any jobs available.

This polling mechanism is used for several key architectural reasons:
-   **Simplicity & Decoupling**: The engine doesn't need to know where workers are or how to contact them. Workers are responsible for initiating communication.
-   **Firewall Friendliness**: The worker only needs to make outbound HTTP requests. The engine never needs to initiate a connection *to* the worker, which is much easier to manage in secure networks.
-   **Scalability**: You can easily scale your workforce by running more instances of your worker application. They will all poll the same endpoints, and the engine will naturally distribute the available jobs among them.

#### Building an External Worker

A worker is a simple, long-running service that executes the following loop:

**1. Fetch and Lock Jobs**

The worker makes a `POST` request to the Abada Engine's `/v1/external-tasks/fetch-and-lock` endpoint.

**Request Body:**
```json
{
    "workerId": "worker-123",
    "topics": ["credit-card-charges", "send-receipts"],
    "lockDuration": 60000 // Lock for 60 seconds
}
```

**Response (if a job is found):**
```json
[
    {
        "id": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
        "topicName": "credit-card-charges",
        "variables": {
            "amount": 129.99,
            "customerId": "CUST-456"
        }
    }
]
```

**2. Execute Business Logic**

Using the `variables` from the response, the worker performs its business logic (e.g., calls a payment gateway API).

**3. Complete the Job**

After the logic is complete, the worker sends a `POST` request to the `/v1/external-tasks/{id}/complete` endpoint, using the unique `id` it received from the lock response.

**Request URL:**
`/v1/external-tasks/a1b2c3d4-e5f6-7890-1234-567890abcdef/complete`

**Request Body (with new variables):**
```json
{
    "paymentTransactionId": "txn_xyz789"
}
```

When the engine receives this call, it deletes the job and resumes the waiting process instance, merging the new variables into the process scope.

**4. Wait and Repeat**

If no jobs were found, or after completing a job, the worker should wait for a configured interval (e.g., 10 seconds) before polling again.

#### Example Worker Pseudocode

Here is a conceptual example of what the main loop of a worker application might look like:

```java
// In your worker application's main service
while (true) {
    try {
        // 1. Ask the engine for a job
        List<LockedExternalTask> lockedTasks = fetchAndLock("my-worker-id", List.of("credit-card-charges"));

        if (!lockedTasks.isEmpty()) {
            LockedExternalTask task = lockedTasks.get(0);

            // 2. Perform the business logic
            System.out.println("Processing job: " + task.id() + " for topic: " + task.topicName());
            Map<String, Object> results = performBusinessLogic(task.variables());

            // 3. Complete the job
            completeTask(task.id(), results);
        } else {
            // No jobs were available
            System.out.println("No jobs found. Waiting...");
        }

    } catch (Exception e) {
        // Log errors to avoid crashing the worker
        System.err.println("An error occurred: " + e.getMessage());
    }

    // 4. Wait before polling again
    Thread.sleep(10000); // Wait for 10 seconds
}
```
