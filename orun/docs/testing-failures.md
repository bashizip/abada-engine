# Testing Failed Jobs

This guide describes how to create failed jobs in the backend for testing purposes and how to validate the failure handling features in the Orun UI.

## Creating Failed Jobs (Backend)

To simulate a job failure, you need to create a process with a service task and a corresponding worker that intentionally fails.

### 1. Create a BPMN Process

Create a BPMN process definition (e.g., `failing-process.bpmn`) with a Service Task. Configure the Service Task with a specific topic or type, for example `failing-service`.

```xml
<bpmn:serviceTask id="ServiceTask_1" name="Failing Task" camunda:type="external" camunda:topic="failing-service" />
```

### 2. Implement a Failing Worker

In the backend application (`abada-engine`), implement a worker that subscribes to the `failing-service` topic. This worker should throw an exception or explicitly report a failure.

**Example (Java/Spring Boot):**

```java
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;

@Component
@ExternalTaskSubscription("failing-service")
public class FailingWorker implements ExternalTaskHandler {

    @Override
    public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        try {
            // Simulate some business logic that fails
            throw new RuntimeException("This is a planned failure for testing purposes.");
        } catch (Exception e) {
            // Report failure to the engine
            externalTaskService.handleFailure(
                externalTask,
                e.getMessage(),
                getStackTrace(e),
                0, // retries (set to 0 to create an incident immediately)
                1000 // retry timeout
            );
        }
    }

    private String getStackTrace(Throwable t) {
        // ... utility to convert stacktrace to string
        return t.toString();
    }
}
```

### 3. Deploy and Start

1. Deploy the `failing-process.bpmn` to the engine.
2. Start a new process instance of `failing-process`.
3. The worker will pick up the task, catch the exception, and report a failure with 0 retries.
4. This will create an "Incident" or "Failed Job" in the engine.

## Validating in Orun UI

Once a failed job is created in the backend, you can validate the feature in the Orun UI.

### 1. Locate the Failed Instance

1. Navigate to the **Process Instances** list.
2. Look for instances with a red `FAILED` or `INCIDENT` badge.
3. Alternatively, check the **Dashboard** or **Jobs** view for a list of failed jobs.

### 2. Inspect Failure Details

1. Click on the failed process instance to view its details.
2. In the **Incidents** or **Failed Jobs** tab, you should see the `Failing Task`.
3. Click on the error message to view the full **Stacktrace**. Verify that it matches the exception thrown in your worker (e.g., "This is a planned failure...").

### 3. Test Retry Logic

1. Click the **Retry** button (often a circular arrow icon) next to the failed job.
2. This sends a request to the backend to increment the retries (e.g., to 1 or 3).
3. The engine will re-queue the job.
4. **Observation**:
    * If the worker is still running and still failing, the job will fail again after a short delay, and the retry count will decrease.
    * If you fixed the worker (or if the failure was transient), the job should complete successfully, and the process instance should move to the next state (or complete).

## Troubleshooting

* **Job not appearing?** Ensure the worker is actually running and connected to the engine. Check backend logs.
* **Retries not working?** Verify the `POST /v1/jobs/{jobId}/retries` endpoint is being called correctly in the browser's Network tab.
