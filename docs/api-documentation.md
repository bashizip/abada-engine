# Abada Engine API Documentation

This document provides a detailed overview of the Abada Engine REST API endpoints. All request and response bodies are in JSON format.

---

## Authentication

All API endpoints require the following headers to be sent with each request to establish the user's identity and permissions:

- `X-User`: The unique identifier for the user (e.g., `alice`).
- `X-Groups`: A comma-separated list of groups the user belongs to (e.g., `customers,managers`).

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

### Start a Process Instance

Starts a new instance of a deployed process.

- **Method & URL**: `POST /v1/processes/start`
- **Query Parameters**:
  - `processId` (string, required): The ID of the process to start. Example: `/v1/processes/start?processId=recipe-cook`
- **Success Response** (`200 OK`):
  ```json
  {
    "processInstanceId": "a1b2c3d4-e5f6-7890-1234-567890abcdef"
  }
  ```

### List All Process Instances

Retrieves a list of all process instances, both active and completed.

- **Method & URL**: `GET /v1/processes/instances`
- **Success Response** (`200 OK`):
  ```json
  [
    {
      "id": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
      "currentActivityId": "user_task_1",
      "variables": {
        "orderId": "order_456"
      },
      "isCompleted": false,
      "startDate": "2024-01-01T12:00:00Z",
      "endDate": null
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
    "currentActivityId": "user_task_1",
    "variables": {
      "orderId": "order_456"
    },
    "isCompleted": false,
    "startDate": "2024-01-01T12:00:00Z",
    "endDate": null
  }
  ```

### Fail a Process Instance

Marks a running process instance as FAILED. This is a terminal status that stops all execution of the instance.

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
- **Error Response** (`400 Bad Request`):
  ```json
  {
    "error": "Cannot fail process instance",
    "processInstanceId": "a1b2c3d4-e5f6-7890-1234-567890abcdef"
  }
  ```

### Get a Process Definition by ID

Retrieves a specific process definition by its ID, including its documentation and the full BPMN XML.

- **Method & URL**: `GET /v1/processes/{id}`
- **Path Parameters**:
  - `{id}` (string, required): The ID of the process definition (e.g., `recipe-cook`). **This must be part of the URL path.**
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
  - `status` (string, optional): Filters tasks by their current status. Valid values: `AVAILABLE`, `CLAIMED`, `COMPLETED`, etc.
- **Success Response** (`200 OK`):
  ```json
  [
    {
      "id": "task_789",
      "name": "Review Order",
      "assignee": "patrick",
      "status": "CLAIMED",
      "startDate": "2024-01-01T12:00:00Z",
      "endDate": null,
      "candidateGroups": ["managers"],
      "processInstanceId": "instance_123",
      "variables": {
        "orderId": "order_456"
      }
    }
  ]
  ```

### Get Task by ID

Retrieves the details of a specific task by its ID, including all process variables.

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
    "candidateGroups": ["managers"],
    "processInstanceId": "instance_123",
    "variables": {
      "orderId": "order_456",
      "amount": 100.0
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
- **Error Response** (`400 Bad Request`):
  ```json
  {
    "error": "Cannot claim task",
    "taskId": "task_789"
  }
  ```

### Complete a Task

Completes a task currently assigned to the user.

- **Method & URL**: `POST /v1/tasks/complete`
- **Query Parameters**:
  - `taskId` (string, required): The ID of the task to complete. Example: `/v1/tasks/complete?taskId=task_789`
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
    "error": "Cannot complete task",
    "taskId": "task_789"
  }
  ```

### Fail a Task

Marks a task as FAILED. This is a terminal status and does not advance the process.

- **Method & URL**: `POST /v1/tasks/fail`
- **Query Parameters**:
  - `taskId` (string, required): The ID of the task to fail. Example: `/v1/tasks/fail?taskId=task_789`
- **Success Response** (`200 OK`):
  ```json
  {
    "status": "Failed",
    "taskId": "task_789"
  }
  ```
- **Error Response** (`400 Bad Request`):
  ```json
  {
    "error": "Cannot fail task",
    "taskId": "task_789"
  }
  ```

---

## Event Controller

### Correlate a Message Event

- **Method & URL**: `POST /v1/events/messages`
- **Request Body** (JSON, required):
  ```json
  {
    "messageName": "order_shipped",
    "correlationKey": "order_456",
    "variables": {
      "shippingDate": "2024-01-01T12:00:00Z"
    }
  }
  ```
- **Success Response**: `202 Accepted` (No response body)

### Broadcast a Signal Event

- **Method & URL**: `POST /v1/events/signals`
- **Request Body** (JSON, required):
  ```json
  {
    "signalName": "system_maintenance",
    "variables": {
      "maintenanceWindow": "2 hours"
    }
  }
  ```
- **Success Response**: `202 Accepted` (No response body)
