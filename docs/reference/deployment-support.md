# Deployment Support Matrix

| Mode | Intended use | Authentication | Runtime guarantee |
|---|---|---|---|
| H2, one engine | Local development and tests | Disabled or trusted local proxy | Restart recovery; not a production topology |
| PostgreSQL, one engine | Self-hosted production | Direct OIDC JWT validation | 0.10 durable runtime: atomic commands, restart recovery, versioned definitions and transactional outbox |
| PostgreSQL, multiple engines | Self-hosted production | Direct OIDC JWT validation | 0.10 cluster-safe acquisition, correlation, idempotency, lease recovery and outbox delivery |
| Trusted proxy | Controlled private network only | OAuth2 Proxy headers | Engine must not be reachable except through the proxy |

Production defaults to `ABADA_SECURITY_MODE=oidc` and requires
`OIDC_ISSUER_URI` and `ABADA_ALLOWED_ORIGINS`. `proxy` mode trusts
`X-Auth-Request-*` headers and is unsafe when clients can reach the engine
directly. `disabled` is only for local development and automated tests.

PostgreSQL is the production source of truth. Flyway owns the schema
and Hibernate validates it at startup. H2 is not used as evidence of
PostgreSQL concurrency correctness. Mutable workflow state is command-local;
two-replica correlation, work acquisition, duplicate-request and failover
behavior is covered by PostgreSQL Testcontainers acceptance tests. See the
[runtime state architecture](../architecture/runtime-state.md) for the exact
boundary.
