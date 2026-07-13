# Deployment Support Matrix

| Mode | Intended use | Authentication | Runtime guarantee |
|---|---|---|---|
| H2, one engine | Local development and tests | Disabled or trusted local proxy | Restart recovery; not a production topology |
| PostgreSQL, one engine | Self-hosted production candidate | Direct OIDC JWT validation | Durable definitions, tokens, joins, tasks, subscriptions and timers; command migration is incomplete |
| PostgreSQL, multiple engines | Pre-production/experimental | Direct OIDC JWT validation | Database-authoritative task and process-instance reads/mutations plus partial subscription and lease protection; full HA certification remains a 0.10 release gate |
| Trusted proxy | Controlled private network only | OAuth2 Proxy headers | Engine must not be reachable except through the proxy |

Production defaults to `ABADA_SECURITY_MODE=oidc` and requires
`OIDC_ISSUER_URI` and `ABADA_ALLOWED_ORIGINS`. `proxy` mode trusts
`X-Auth-Request-*` headers and is unsafe when clients can reach the engine
directly. `disabled` is only for local development and automated tests.

PostgreSQL is the intended production source of truth. Flyway owns the schema
and Hibernate validates it at startup. H2 is not used as evidence of
PostgreSQL concurrency correctness. Mutable task and process-instance state is
command-local, while correlation and work-acquisition correctness still need
the 0.10 acceptance evidence; see the
[runtime state architecture](../architecture/runtime-state.md) for the exact
boundary.
