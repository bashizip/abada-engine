# Abada Reliable OSS Core Roadmap

This is the authoritative public roadmap. SaaS, DMN/CMMN and public agentic
runtime work are deferred until the core reaches the corresponding release
gates.

## 0.9 — Durable runtime

- Flyway-owned PostgreSQL schema and immutable definition versions
- Complete token and gateway-join persistence
- Optimistic concurrency on mutable runtime records
- Durable event subscriptions, leased timer jobs and activity history
- Explicit BPMN support validation

## 0.10 — Cluster safety

- Atomic acquisition for every engine and external-worker work type
- Idempotency records for public mutation commands
- Replica failover and concurrent-correlation Testcontainers suites
- Transactional outbox publication and expired-lease recovery

## 0.11 — Stable contracts

- Frozen `/api/v1` and external-worker protocol
- Direct OIDC validation, permission enforcement and security tests
- Java worker SDK and aligned Tenda/Orun clients

## 1.0 RC and 1.0

- Published BPMN conformance results and reproducible PostgreSQL benchmarks
- Upgrade, backup/restore and operational evidence
- No duplicate or lost workflow progress during replica termination, duplicate
  requests, concurrent correlation or supported rolling upgrades

See [BPMN support](../reference/bpmn-support.md) and
[deployment support](../reference/deployment-support.md) for current guarantees.
