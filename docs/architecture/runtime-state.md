# Runtime State Architecture

This document defines the target runtime architecture for Abada 1.0 and tracks
the transition from the original in-memory engine. It is the authoritative
architecture document for mutable workflow state. The Flyway migrations are
the authority for physical table and column names.

## Target invariants

Abada 1.0 must satisfy all of these invariants:

1. **PostgreSQL is authoritative.** A command reconstructs mutable workflow
   state from committed database rows. It does not use a process-instance or
   task object left in a replica's memory as its input.
2. **Mutable state is command-local.** Tokens, joins, variables, task state,
   subscriptions and jobs are mutable only inside the command handling them.
3. **One command, one transaction.** Loading, authorization and state
   validation, BPMN advancement, persistence of resulting work and history,
   and commit form one database transaction.
4. **Conflicts are explicit.** Row locks serialize operations that must have a
   single winner; optimistic versions detect stale writes elsewhere. A retry or
   deterministic conflict response must never silently lose progress.
5. **Work is durable.** Timers, continuations and external work are database
   jobs with status, due time, lease owner, lease expiry, attempt count and
   retry data. Expired leases can be recovered by another replica.
6. **External publication follows commit.** Lifecycle events and webhooks are
   written to a transactional outbox. No external observer is told about state
   that later rolls back.
7. **Only immutable definitions may be cached.** Parsed BPMN definitions are
   cached by immutable deployment/version identifier. Cache loss changes
   performance, not behavior.

These invariants allow any request or acquired job to run on any engine
replica. Restarting or terminating a replica discards no authoritative
workflow state.

## Command lifecycle

The target command path is:

```mermaid
sequenceDiagram
    participant C as API or worker
    participant E as Engine command
    participant P as PostgreSQL
    participant O as Outbox dispatcher

    C->>E: mutation command + identity/idempotency key
    E->>P: begin transaction
    E->>P: load and lock/version-check required rows
    P-->>E: authoritative state
    E->>E: authorize, validate and advance BPMN
    E->>P: persist state, work, history and outbox
    E->>P: commit
    P-->>E: committed
    E-->>C: deterministic result
    O->>P: lease committed outbox records
    O-->>C: lifecycle delivery (independent retry)
```

If the engine fails before commit, PostgreSQL rolls the command back and
another request or worker can retry it. If it fails after commit, the state is
already durable; an idempotency record makes a duplicate request return the
same logical result. External side effects remain at-least-once unless the
external system participates through an idempotent protocol.

## Implemented slice: tasks and process instances

User tasks and process instances are the first runtime areas migrated to this
model:

1. Task reads query PostgreSQL by task ID, process instance, assignee,
   candidate user or candidate group. They return detached snapshots and do
   not populate a runtime-wide map.
2. `claim`, `completeTask` and `failTask` obtain a PostgreSQL write lock on the
   target task row and materialize one command-local task object.
3. Each command validates the committed task status before changing it. Task
   completion also reconstructs the process instance from its database row,
   advances the BPMN model and persists successor work in the transaction.
4. Task state and activity history commit atomically. Concurrent replicas wait
   for the same row lock and observe the winning status after it commits.
5. Process detail and list reads query PostgreSQL and return detached
   snapshots. Process control, variable updates, event resume and task-driven
   advancement lock the process row before reconstructing tokens, joins and
   variables.
6. Startup does not rehydrate task or process objects. Active gauges are
   restored with grouped database counts instead of loading mutable state.

A concurrent task command on another engine waits for the task lock, then
reads the committed status and is rejected when the transition is no longer
valid. PostgreSQL integration tests verify single winners for claim, failure
and completion across two application contexts, with one corresponding
history event.

Concurrent variable-update tests across two application contexts verify that
the process lock preserves both replicas' changes instead of losing the first
committed update. Mutating a detached process query result also has no effect
on a later read or command.

## Current migration status

| Area | Current behavior | 1.0 target |
|---|---|---|
| Parsed process definitions | Immutable versions are persisted and parsed definitions are cached in each replica | Keep immutable cache keyed only by definition version/deployment ID |
| User-task lifecycle | Claim, completion and failure lock PostgreSQL task rows and mutate command-local snapshots | Add deterministic idempotency to claim/failure and retain this command model |
| Startup | Loads immutable definitions only; active process/task gauges use aggregate queries | Retain this model and extend durable recovery evidence |
| Process control | Instance mutations load and lock PostgreSQL rows and use command-local state | Add deterministic idempotency and transaction-aware metrics |
| Query APIs | Task and instance reads use PostgreSQL and return detached snapshots | Add pagination and purpose-built projections |
| Message/signal correlation | Subscriptions are durable, but all correlation paths are not yet proven command-local and concurrent-safe | Lock/consume subscriptions and advance the instance transactionally |
| Timers/external work | Durable job and lease fields exist | Prove atomic multi-replica acquisition, expiry recovery and idempotent completion |
| Metrics | Some counters are changed before transaction outcome is known | Derive durable facts or update transaction-aware metrics after commit |
| Lifecycle delivery | Activity history is durable; transactional outbox is not implemented | Persist outbox records in the command transaction and deliver with leases |

Consequently, PostgreSQL is the intended production authority, but the whole
runtime does **not yet** satisfy the target invariants. Multi-replica operation
remains experimental until the 0.10 acceptance gate passes.

## Concurrency policy

- Use pessimistic row locking for a singular transition with a natural owner,
  such as completing one user task or consuming one subscription.
- Use optimistic version columns on mutable aggregate records to detect stale
  updates and protect paths that cannot lock every row up front.
- Use atomic lease acquisition, preferably PostgreSQL `FOR UPDATE SKIP LOCKED`,
  for competing workers claiming independent jobs.
- Keep transactions short: never perform remote service calls while holding a
  workflow row lock.
- Require an idempotency key or protocol identity where a client can repeat a
  mutation after losing the response.

Exactly-once refers to committed workflow state transitions. Calls to external
systems are at-least-once unless their own idempotency contract prevents a
duplicate side effect.

## Cache policy

Allowed caches contain immutable parsed process definitions. They may be
discarded and reconstructed from a stored BPMN definition without changing
execution semantics.

Mutable process instances, tasks, subscriptions, jobs and variables are not
runtime-wide cache entries. Command-local maps inside a materialized process
instance hold variables and join state only for the lifetime of that command.

## Completion criteria

The migration is complete only when:

- every mutation command follows the command lifecycle above;
- query APIs read database projections rather than mutable replica maps;
- startup rehydrates only immutable definitions;
- multi-replica tests cover task commands, correlation, timers and external
  work, including replica termination around commit;
- duplicate requests return deterministic results; and
- outbox delivery, lease recovery and supported rolling upgrades pass their
  PostgreSQL acceptance suites.

Progress and test evidence are tracked in the
[Reliable OSS Core roadmap](../development/roadmap.md).
