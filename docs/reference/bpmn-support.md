# BPMN Support Contract

This matrix defines the BPMN semantics guaranteed by Abada. Deployment rejects
unsupported flow nodes instead of silently treating them as pass-through nodes.

| Element | Status | Guaranteed semantics |
|---|---|---|
| None start/end events | Supported | One none start event; completion after all active tokens reach an end |
| User task | Supported | Assignee/candidate authorization, claim, complete and fail |
| Service task (`camunda:class`) | Supported | Synchronous Java delegate execution inside the engine transaction |
| Service task (`camunda:topic`) | Supported | Durable external task with fetch/lock, completion and failure |
| Script task | Supported | Server-side JavaScript/ECMAScript with process variables as bindings |
| Exclusive gateway | Supported | First matching conditional flow, then configured default flow |
| Inclusive gateway | Supported | All matching flows and matching-token join behavior |
| Parallel gateway | Supported | Fork all outgoing flows and wait for all expected join tokens |
| Message catch event | Supported | Durable subscription by message name and `correlationKey` variable |
| Signal catch event | Supported | Durable broadcast subscription by signal name |
| Duration timer catch event | Supported | Durable scheduled job for ISO-8601 durations |
| Event-based gateway | Limited | A single outgoing catch event only |

Not supported in the 1.0 contract: subprocesses, call activities, boundary
events, event subprocesses, compensation, transactions, multi-instance
activities, complex gateways, conditional events, time-date/time-cycle timers,
message/signal start events, throwing events, receive/send/manual/business-rule
tasks, DMN and CMMN.

Embedded Java and script tasks can execute external side effects. Database state
transitions are protected against duplicate engine advancement, but applications
must make those side effects idempotent. External tasks are the recommended
boundary for remote or retryable work.
