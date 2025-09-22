Feature Request: Add Task Status Field with Extended Lifecycle

Please extend the Task entity, persistence layer, and REST API to include an explicit status field of type TaskStatus (Java enum).

Core statuses (current implicit ones):

AVAILABLE (UI: Pending) → task created, no assignee yet.

CLAIMED (UI: In Progress) → task claimed by a user.

COMPLETED (UI: Done) → task finished, process advanced.

Extended statuses (future-proofing):

SUSPENDED (UI: On Hold) → paused temporarily.

CANCELLED (UI: Cancelled) → explicitly terminated.

DELEGATED (UI: Delegated) → reassigned to another user.

ESCALATED (UI: Escalated) → breached SLA/deadline, raised to another actor.

EXPIRED (UI: Timed Out) → deadline passed without action.

FAILED (UI: Failed) → task ended unsuccessfully.

Acceptance Criteria:

Add TaskStatus enum with the above values.

Add a status column in Task JPA entity and database schema.

Populate status appropriately on task creation, claim, completion, and cancellation.

Update TaskDetailsDto to expose status.

Update Task REST APIs to allow filtering by status.

Ensure persistence and recovery reload the correct status after restart.

Keep API backward-compatible: for now only core statuses will be actively set by engine; extended ones can be set via admin APIs later.

This will allow frontends (Tenda & Orun) to display clear, business-friendly statuses (Pending, In Progress, Done, On Hold, Cancelled, etc.), while the engine remains technically precise.