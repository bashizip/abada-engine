# Abada Engine API Documentation

This document provides a detailed and accurate overview of the Abada Engine REST API endpoints. All request and response bodies are in JSON format.

---

## Authentication

All API endpoints require the following headers to be sent with each request to establish the user's identity and permissions:

- `X-User`: The unique identifier for the user (e.g., `alice`).
- `X-Groups`: A comma-separated list of groups the user belongs to (e.g., `customers,managers`).

---

## Standard Error Response

When an API call fails due to a client-side error (e.g., providing an unknown ID, violating a business rule), the server will respond with a `400 Bad Request` status and a standardized JSON error body:

```json
{
  "status": 400,
  "message": "A clear, specific error message.",
  "path": "/v1/the-endpoint-that-was-called"
}
```

---

## Process Controller

### Deploy a Process

Deploys a new BPMN process definition from an XML file.

- **Method & URL**: `POST /v1/processes/deploy`
- **Request Type**: `multipart/form-data`
  - `file`: The BPMN 2.0 XML file.
- **Success Response** (`200 OK`):

  ```json
  {
    "status": "Deployed"
  }
  ```

### List Deployed Processes

Retrieves a list of all deployed process definitions.

- **Method & URL**: `GET /v1/processes`
- **Success Response** (`200 OK`):

  ```json
  [
    {
      "id": "process_1",
      "name": "My Process",
      "documentation": "This is the official process for handling customer orders."
    }
  ]
  ```

**Notes:**

- Process definitions may include `candidateStarterGroups` and `candidateStarterUsers` attributes in their BPMN XML
- These attributes define authorization rules for who can start the process
- Example in BPMN:

  ```xml
  <bpmn:process id="recipe-cook" 
                camunda:candidateStarterGroups="customers,managers"
                camunda:candidateStarterUsers="alice,bob">
  ```

- These values are parsed during deployment and stored in the database
- Client applications can use these values to control process start authorization in their UI

### Start a Process Instance

Starts a new instance of a deployed process.

- **Method & URL**: `POST /v1/processes/start`
- **Query Parameters**:
  - `processId` (string, required): The ID of the process to start. Example: `/v1/processes/start?processId=recipe-cook`
  - **`username` (string, optional)**: The username of the person starting the process. If not provided, defaults to `"system"`. Example: `/v1/processes/start?processId=recipe-cook&username=alice`
- **Success Response** (`200 OK`):

  ```json
  {
    "processInstanceId": "a1b2c3d4-e5f6-7890-1234-567890abcdef"
  }
  ```

- **Error Response** (`400 Bad Request`):

  ```json
  {
    "status": 400,
    "message": "Unknown process ID: invalid-process-id",
    "path": "/v1/processes/start"
  }
  ```

**Notes:**

- The `username` parameter enables audit tracking of who started each process instance
- The value is stored in the `startedBy` field of the process instance
- If omitted, the process is marked as started by `"system"`

### List All Process Instances

Retrieves a list of all process instances.

- **Method & URL**: `GET /v1/processes/instances`
- **Success Response** (`200 OK`):

  ```json
  [
    {
      "id": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
      "processDefinitionId": "recipe-cook",
      "processDefinitionName": "Recipe Cook Process",
      "currentActivityId": "cook-recipe",
      "status": "RUNNING",
      "startDate": "2024-01-01T12:00:00Z",
      "endDate": null,
      "startedBy": "alice",
      "variables": {}
    }
  ]
  ```

### Get a Process Instance by ID

Retrieves a specific process instance by its ID.

- **Method & URL**: `GET /v1/processes/instance/{id}`
- **Path Parameters**:
  - `{id}` (string, required): The unique ID of the process instance. **This must be part of the URL path.**
- **Success Response** (`200 OK`):

  ```json
  {
    "id": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
    "processDefinitionId": "recipe-cook",
    "processDefinitionName": "Recipe Cook Process",
    "currentActivityId": "cook-recipe",
    "status": "RUNNING",
    "startDate": "2024-01-01T12:00:00Z",
    "endDate": null,
    "startedBy": "alice",
    "variables": {
      "orderId": "order_456"
    }
  }
  ```

**Response Fields:**

- `id`: Unique identifier for the process instance
- `processDefinitionId`: ID of the process definition this instance is based on
- `processDefinitionName`: Human-readable name of the process definition
- `currentActivityId`: ID of the current activity (task/event) where the process is waiting
- `status`: Current status of the process instance (`RUNNING`, `COMPLETED`, `FAILED`)
- `startDate`: Timestamp when the process instance was started (ISO 8601 format)
- `endDate`: Timestamp when the process instance completed (null if still running)
- **`startedBy`**: Username of the person who started the process (or `"system"` for automated starts)
- `variables`: Key-value pairs of process variables

### Fail a Process Instance

Marks a running process instance as FAILED.

- **Method & URL**: `POST /v1/processes/instance/{id}/fail`
- **Path Parameters**:
  - `{id}` (string, required): The unique ID of the process instance to fail. **This must be part of the URL path.**
- **Success Response** (`200 OK`):

  ```json
  {
    "status": "Failed",
    "processInstanceId": "a1b2c3d4-e5f6-7890-1234-567890abcdef"
  }
  ```

### Get a Process Definition by ID

Retrieves a specific process definition by its ID.

- **Method & URL**: `GET /v1/processes/{id}`
- **Path Parameters**:
  - `{id}` (string, required): The ID of the process definition. **This must be part of the URL path.**
- **Success Response** (`200 OK`):

  ```json
  {
    "id": "recipe-cook",
    "name": "Recipe Cook Process",
    "documentation": "A simple process for cooking a recipe.",
    "bpmnXml": "<bpmn:definitions>..."
  }
  ```

---

## Task Controller

### List Visible Tasks

Retrieves a list of tasks visible to the current user.

- **Method & URL**: `GET /v1/tasks`
- **Query Parameters**:
  - `status` (string, optional): Filters tasks by their current status. (e.g., `AVAILABLE`, `CLAIMED`).
- **Success Response** (`200 OK`):

  ```json
  [
    {
      "id": "task_789",
      "name": "Review Order",
      "assignee": "patrick",
      "status": "CLAIMED",
      "startDate": "2024-01-01T12:00:00Z",
      "endDate": null
    }
  ]
  ```

### Get Task by ID

Retrieves the details of a specific task by its ID.

- **Method & URL**: `GET /v1/tasks/{id}`
- **Path Parameters**:
  - `{id}` (string, required): The unique ID of the task. **This must be part of the URL path.**
- **Success Response** (`200 OK`):

  ```json
  {
    "id": "task_789",
    "name": "Review Order",
    "assignee": "patrick",
    "status": "CLAIMED",
    "startDate": "2024-01-01T12:00:00Z",
    "endDate": null,
    "variables": {
      "orderId": "order_456"
    }
  }
  ```

### Claim a Task

Claims an unassigned task for the current user.

- **Method & URL**: `POST /v1/tasks/claim`
- **Query Parameters**:
  - `taskId` (string, required): The ID of the task to claim. Example: `/v1/tasks/claim?taskId=task_789`
- **Success Response** (`200 OK`):

  ```json
  {
    "status": "Claimed",
    "taskId": "task_789"
  }
  ```

### Complete a Task

Completes a task currently assigned to the user.

- **Method & URL**: `POST /v1/tasks/complete`
- **Query Parameters**:
  - `taskId` (string, required): The ID of the task to complete.
- **Request Body** (JSON, optional):

  ```json
  {
    "approved": true,
    "comments": "Looks good."
  }
  ```

- **Success Response** (`200 OK`):

  ```json
  {
    "status": "Completed",
    "taskId": "task_789"
  }
  ```

- **Error Response** (`400 Bad Request`):

  ```json
  {
    "status": 400,
    "message": "Task not found: invalid-task-id",
    "path": "/v1/tasks/complete"
  }
  ```

### Fail a Task

Marks a task as FAILED.

- **Method & URL**: `POST /v1/tasks/fail`
- **Query Parameters**:
  - `taskId` (string, required): The ID of the task to fail.
- **Success Response** (`200 OK`):

  ```json
  {
    "status": "Failed",
    "taskId": "task_789"
  }
  ```

### Get User Statistics

Retrieves comprehensive statistics and activity data for the current user.

- **Method & URL**: `GET /v1/tasks/user-stats`
- **Success Response** (`200 OK`):

  ```json
  {
    "quickStats": {
      "activeTasks": 2,
      "completedTasks": 15,
      "runningProcesses": 3,
      "availableTasks": 1
    },
    "recentTasks": [
      {
        "id": "03b905d8-c251-4c40-8bb3-086a29299445",
        "name": "Review Order",
        "taskDefinitionKey": "review-order",
        "status": "COMPLETED",
        "startDate": "2024-01-15T10:30:00Z",
        "processInstanceId": "656f2037-dddd-4c0f-af68-02a8634ff0e4"
      },
      {
        "id": "c1067e06-5910-42cc-b172-c9bcf91b24d8",
        "name": "Approve Payment",
        "taskDefinitionKey": "approve-payment",
        "status": "CLAIMED",
        "startDate": "2024-01-15T09:15:00Z",
        "processInstanceId": "17278ff4-40d0-426c-88bd-c8b24c64c39e"
      }
    ],
    "tasksByStatus": {
      "AVAILABLE": 1,
      "CLAIMED": 2,
      "COMPLETED": 15,
      "FAILED": 0
    },
    "overdueTasks": [
      {
        "id": "overdue-task-123",
        "name": "Urgent Review",
        "taskDefinitionKey": "urgent-review",
        "startDate": "2024-01-08T14:00:00Z",
        "daysOverdue": 7,
        "processInstanceId": "process-instance-456"
      }
    ],
    "processActivity": {
      "recentlyStartedProcesses": [
        {
          "id": "bf395379-cf21-4129-84f1-ac39c68022f7",
          "processDefinitionId": "order-processing",
          "startDate": "2024-01-15T08:00:00Z",
          "currentActivityId": "review-order"
        },
        {
          "id": "656f2037-dddd-4c0f-af68-02a8634ff0e4",
          "processDefinitionId": "payment-approval",
          "startDate": "2024-01-14T16:30:00Z",
          "currentActivityId": null
        }
      ],
      "activeProcessCount": 3,
      "completionRate": 0.75
    }
  }
  ```

**Response Fields Description:**

- **quickStats**: Summary statistics for the user
  - `activeTasks`: Number of tasks currently in CLAIMED status by the user
  - `completedTasks`: Number of tasks completed by the user
  - `runningProcesses`: Number of process instances that have tasks for the user
  - `availableTasks`: Number of tasks the user can claim (AVAILABLE status + eligible)

- **recentTasks**: Array of the 10 most recent tasks assigned to the user, ordered by start date (newest first)
  - `id`: Unique task identifier
  - `name`: Human-readable task name
  - `taskDefinitionKey`: BPMN task definition key
  - `status`: Current task status
  - `startDate`: When the task was created
  - `processInstanceId`: ID of the process instance containing this task

- **tasksByStatus**: Object with task counts grouped by status
  - Keys are task status values (AVAILABLE, CLAIMED, COMPLETED, FAILED, etc.)
  - Values are the count of tasks in that status for the user

- **overdueTasks**: Array of tasks that are overdue (CLAIMED for more than 7 days)
  - `id`: Unique task identifier
  - `name`: Human-readable task name
  - `taskDefinitionKey`: BPMN task definition key
  - `startDate`: When the task was created
  - `daysOverdue`: Number of days the task has been overdue
  - `processInstanceId`: ID of the process instance containing this task

- **processActivity**: Information about processes related to the user
  - `recentlyStartedProcesses`: Array of recently started processes that have tasks for the user
    - `id`: Unique process instance identifier
    - `processDefinitionId`: ID of the process definition
    - `startDate`: When the process instance was started
    - `currentActivityId`: Current activity ID (null if process is completed)
  - `activeProcessCount`: Number of active (RUNNING) process instances with user's tasks
  - `completionRate`: Decimal value (0.0 to 1.0) representing the completion rate for processes with user's tasks

---

## Event Controller

### Correlate a Message Event

- **Method & URL**: `POST /v1/events/messages`
- **Request Body** (JSON, required):

  ```json
  {
    "messageName": "order_shipped",
    "correlationKey": "order_456",
    "variables": {}
  }
  ```

- **Success Response**: `202 Accepted` (No response body)
- **Error Response** (`400 Bad Request`):

  ```json
  {
    "status": 400,
    "message": "No process instance found for correlation key: invalid_key",
    "path": "/v1/events/messages"
  }
  ```

### Broadcast a Signal Event

- **Method & URL**: `POST /v1/events/signals`
- **Request Body** (JSON, required):

  ```json
  {
    "signalName": "system_maintenance",
    "variables": {}
  }
  ```

- **Success Response**: `202 Accepted` (No response body)
