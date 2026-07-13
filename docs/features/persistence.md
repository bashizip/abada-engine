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

## User-task lifecycle

User tasks are not rehydrated into an engine-wide memory map. Task list and
detail reads query PostgreSQL and return detached snapshots. Claim, completion
and failure commands lock the target task row, validate its committed status,
apply the transition to a command-local object and persist it in the command
transaction.

This makes concurrent replicas deterministic: after one replica commits a
transition, another waiting replica reads the new status and cannot repeat an
invalid transition. Indexes cover process-instance, assignee, candidate-user
and candidate-group task lookups.

## Startup recovery

Startup currently reloads immutable definitions and the remaining
process-instance compatibility state. It does not reload user-task objects.
Active-task metrics are reconstructed with a grouped count query rather than
materializing all tasks.

Process-instance startup rehydration is transitional and is the next runtime
cache scheduled for removal. The final architecture will load mutable instance
state only for the command handling it.

## Guarantees and remaining work

PostgreSQL tests currently cover restart recovery and concurrent task claim,
completion and failure across two application contexts. Full multi-replica
certification still requires database-authoritative process commands, atomic
event correlation, timer/external-work acquisition and failure testing around
transaction commits.

See [Runtime State Architecture](../architecture/runtime-state.md) and the
[Reliable OSS Core Roadmap](../development/roadmap.md) for the exact migration
boundary and acceptance gates.
