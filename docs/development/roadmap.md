# Abada Reliable OSS Core Roadmap

This is the authoritative checklist for the Abada 1.0 reliable open-source
core. The current milestone is **0.9 — Durable runtime**.

Last reviewed: 2026-07-13.

## How to use this roadmap

- `[x]` means the implementation is present and its current automated checks
  pass.
- `[ ]` means the work or its required acceptance evidence is incomplete. An
  item remains unchecked when only a foundation or partial implementation
  exists.
- Close a release gate only when every required item in that gate is complete
  and the linked evidence is reproducible in CI.
- Update this file in the same pull request that completes or reopens an item.

## 0.9 — Durable runtime

### Schema and process definitions

- [x] Use Flyway migrations for the production schema and validate rather than
  generate the schema with Hibernate.
- [x] Store immutable process-definition versions instead of overwriting a
  deployed process key.
- [x] Persist definition checksums and deployment identifiers.
- [x] Prove fresh-schema Flyway migrations against PostgreSQL with
  Testcontainers.
- [ ] Prove upgrades from every supported schema version against PostgreSQL
  with Testcontainers.

### Authoritative execution state

- [x] Persist variables, execution tokens and parallel-gateway join state.
- [x] Add optimistic version columns to mutable runtime records.
- [x] Persist message and signal subscriptions.
- [x] Persist timer jobs with due time, lease data, attempts and retry data.
- [x] Persist external tasks with locking, completion and failure state.
- [x] Record append-only activity history.
- [x] Make user-task completion load locked task and process state from
  PostgreSQL and mutate only command-local objects. Evidence:
  [`PostgresRestartRecoveryTest`](../../engine/src/test/java/com/abada/engine/persistence/PostgresRestartRecoveryTest.java).
- [x] Remove runtime-wide mutable instance, task, subscription and scheduling
  maps; load authoritative state from PostgreSQL for each command.
  - [x] Remove the user-task map; query visible tasks from PostgreSQL and lock
    task rows for claim, completion and failure. Evidence:
    [`PostgresRestartRecoveryTest`](../../engine/src/test/java/com/abada/engine/persistence/PostgresRestartRecoveryTest.java).
  - [x] Remove the process-instance compatibility map and startup rehydration;
    lock process rows for mutations and return detached database snapshots for
    queries. Evidence:
    [`PostgresRestartRecoveryTest`](../../engine/src/test/java/com/abada/engine/persistence/PostgresRestartRecoveryTest.java).
- [ ] Make every engine command a single atomic load, validate, advance,
  persist and commit transaction.
- [ ] Retain only immutable parsed definition caches in memory, keyed by
  definition version.
- [ ] Publish lifecycle events and webhooks through a transactional outbox.

### Supported BPMN behavior

- [x] Publish the current BPMN support matrix.
- [x] Reject known unsupported BPMN constructs during deployment.
- [x] Support the documented core constructs, including script tasks.
- [ ] Back every row in the support matrix with executable conformance tests.
- [ ] Publish precise tested semantics for variables, retries, cancellation,
  suspension, tasks, gateways and catch events.

### Recovery evidence

- [x] Exercise basic restart recovery in the current persistence test suite.
- [x] Run a full application-context restart against PostgreSQL and recover a
  deployed definition, active token, user task, variables and history before
  completing the workflow. Evidence:
  [`PostgresRestartRecoveryTest`](../../engine/src/test/java/com/abada/engine/persistence/PostgresRestartRecoveryTest.java).
- [ ] Use PostgreSQL as the correctness reference for all persistence and
  concurrency tests; keep H2 as a convenience profile only.
- [ ] Test failure immediately before and after transaction commit.
- [x] Prove that a persisted user task, active token and variables recover
  without lost workflow progress.
- [ ] Prove that durable subscriptions, timer jobs and external tasks recover
  without lost workflow progress.

- [ ] **0.9 release gate:** durable PostgreSQL execution and restart recovery
  pass all acceptance tests without relying on mutable in-memory state.

## 0.10 — Cluster safety

### Work acquisition and recovery

- [x] Store lease owner, lease expiry, attempt count and retry data for durable
  jobs.
- [x] Recover expired job leases for reprocessing.
- [x] Support external-task fetch-and-lock, lock extension, completion and
  technical failure.
- [ ] Atomically claim timers, continuations and external work across two or
  more replicas without duplicate state transitions.
- [ ] Use a PostgreSQL work-acquisition strategy whose contention behavior is
  covered by concurrent tests.
- [ ] Recover work when a worker or engine replica dies while holding a lease.
- [ ] Deliver transactional-outbox records reliably and idempotently.

### Idempotency and concurrent commands

- [x] Support idempotency keys for process starts and task completion.
- [x] Serialize concurrent completion of the same user task across two engine
  application contexts, with one committed transition and one completion
  history event. Evidence:
  [`PostgresRestartRecoveryTest`](../../engine/src/test/java/com/abada/engine/persistence/PostgresRestartRecoveryTest.java).
- [ ] Define and implement idempotency for every public mutation command,
  message correlation and external-task completion.
- [ ] Return deterministic results for duplicate and concurrently repeated
  requests.
- [ ] Correlate durable message and signal subscriptions transactionally.
- [ ] Prevent lost updates during concurrent completion, cancellation,
  correlation and timer firing.

### Multi-replica acceptance

- [ ] Run two or more engine replicas against one PostgreSQL database in CI.
- [ ] Concurrently claim timers, messages, user tasks and external work.
- [ ] Kill replicas before and after commits and verify recovery.
- [ ] Demonstrate exactly-once workflow state transitions while documenting
  at-least-once external side-effect semantics.
- [ ] Pass replica failover and concurrent-correlation Testcontainers suites.

- [ ] **0.10 release gate:** multi-replica acquisition, leases, idempotency,
  failover and concurrent-command correctness pass on PostgreSQL.

## 0.11 — Stable contracts and security

### Public API and worker protocol

- [ ] Freeze a consistent `/api/v1` contract with pagination, filtering,
  stable DTOs and typed errors.
- [ ] Validate generated OpenAPI and API compatibility in CI.
- [ ] Add optional `Idempotency-Key` support to all applicable mutation
  endpoints.
- [ ] Freeze a versioned worker protocol for fetch-and-lock, heartbeat, lock
  extension, completion, BPMN error, technical failure, retry and trace
  propagation.

### Authentication, authorization and audit

- [x] Validate OIDC JWTs with Spring Security resource-server support.
- [x] Keep trusted proxy-header authentication as an explicit deployment mode.
- [x] Restrict CORS and avoid logging authorization headers and full sensitive
  payloads.
- [x] Record actor, action, timestamps, workflow identifiers and trace data in
  activity history.
- [ ] Enforce and test permissions for deployment, process control, task
  actions, operations access and external workers.
- [ ] Test forged proxy headers, invalid and expired JWTs, role boundaries,
  CORS, sensitive logging and unauthorized cross-user task actions.

### Clients and product integration

- [ ] Publish a Java external-worker SDK against the frozen protocol.
- [ ] Align Tenda with the guaranteed task APIs.
- [ ] Align Orun with durable history, incidents, jobs and instance state.

- [ ] **0.11 release gate:** REST and worker contracts are frozen; OIDC, RBAC,
  security tests, the Java SDK and frontends are aligned.

## 1.0 RC — Evidence and operations

### Conformance and quality

- [ ] Publish executable conformance results for every supported BPMN
  construct and semantic guarantee.
- [ ] Run parser, engine unit, PostgreSQL integration, frontend lint/build and
  API compatibility checks on every pull request.
- [x] Enforce zero known npm dependency vulnerabilities for Tenda and Orun in
  CI with a low-severity audit gate.
- [ ] Complete an independent security review with no unresolved critical
  findings.
- [ ] Reach zero unresolved critical correctness defects.

### Upgrades and operations

- [ ] Test migrations from every supported minor version.
- [ ] Test a supported rolling upgrade across multiple replicas.
- [ ] Publish and verify backup and restore instructions.
- [ ] Publish incident, recovery and operational runbooks.
- [ ] Provide one reproducible quickstart and one production reference
  deployment.
- [ ] Provide runnable sample workflows for the supported BPMN subset.

### Reproducible performance

- [ ] Publish throughput and latency benchmarks.
- [ ] Document benchmark hardware, PostgreSQL configuration, workflow model
  shape and concurrency.
- [ ] Define and pass release performance thresholds.

- [ ] **1.0 RC release gate:** conformance, upgrades, security, benchmarks and
  operational evidence are published and reproducible.

## 1.0 — Release acceptance

- [ ] Terminate engine replicas without duplicate or lost workflow progress.
- [ ] Repeat API requests and worker completions with deterministic outcomes.
- [ ] Correlate messages concurrently without duplicate or lost progress.
- [ ] Perform supported rolling upgrades without duplicate or lost progress.
- [ ] Verify that the same PostgreSQL database passes the complete acceptance
  suite for all scenarios above.
- [ ] **1.0 release gate:** all acceptance evidence is green and no critical
  correctness defect remains unresolved.

## Deferred until after 1.0

These items do not count toward the 1.0 completion percentage: hosted SaaS,
multi-tenancy, billing, Kubernetes packaging, DMN, CMMN, TypeScript and Python
worker SDKs, public agent runtime, AI memory and policy-engine work. Future
agentic features should consume the durable worker, event, variable, policy and
telemetry contracts without bypassing the BPMN state machine.

See [BPMN support](../reference/bpmn-support.md) and
[deployment support](../reference/deployment-support.md) for the current
guarantees.
