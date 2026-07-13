# Persistence and Recovery

Abada persists production workflow state in PostgreSQL. Flyway owns the schema
and Hibernate validates it at startup. H2 remains available for local
convenience, but PostgreSQL Testcontainers tests are the persistence and
concurrency reference.

## Stored state

- Immutable, versioned BPMN process definitions and their raw XML
- Process instances, variables, active tokens and gateway join state
- User tasks and candidate users/groups
- Message and signal subscriptions
- Timer jobs and external tasks with lease and retry state
- Append-only activity history and idempotency records

## Command-local runtime state

User tasks are not rehydrated into an engine-wide memory map. Task list and
detail reads query PostgreSQL and return detached snapshots. Claim, completion
and failure commands lock the target task row, validate its committed status,
apply the transition to a command-local object and persist it in the command
transaction.

This makes concurrent replicas deterministic: after one replica commits a
transition, another waiting replica reads the new status and cannot repeat an
invalid transition. Indexes cover process-instance, assignee, candidate-user
and candidate-group task lookups.

Process-instance detail and list queries likewise materialize detached
snapshots directly from PostgreSQL. Completion, cancellation, failure,
suspension, variable updates and event resumption take a write lock on the
instance row before reconstructing tokens, joins and variables. Concurrent
commands therefore see the previous command's committed version instead of a
replica-local object.

## Startup recovery

Startup reloads only immutable parsed process definitions. It does not reload
user-task or process-instance objects. Active process and task metrics are
reconstructed with grouped count queries rather than materializing workflow
state.

## Guarantees and remaining work

PostgreSQL tests currently cover restart recovery and concurrent task claim,
completion and failure across two application contexts. Full multi-replica
certification still requires database-authoritative process commands, atomic
event correlation, timer/external-work acquisition and failure testing around
transaction commits.

See [Runtime State Architecture](../architecture/runtime-state.md) and the
[Reliable OSS Core Roadmap](../development/roadmap.md) for the exact migration
boundary and acceptance gates.
