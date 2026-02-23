# Scripts

Scripts are now grouped by environment:

- `scripts/dev/`: local development lifecycle scripts
- `scripts/prod/`: production build/release/deploy scripts
- `scripts/test/`: health/auth/traffic validation scripts

## Quick Start

Development:

```bash
./scripts/dev/build-and-run-dev.sh
```

Production:

```bash
./scripts/prod/build-prod.sh
```

Testing:

```bash
./scripts/test/test-health.sh
```

## Dev Scripts

- `scripts/dev/build-and-run-dev.sh`
- `scripts/dev/start-dev.sh`
- `scripts/dev/stop-dev.sh`
- `scripts/dev/logs-engine.sh`
- `scripts/dev/clean-all.sh`

## Prod Scripts

- `scripts/prod/build-prod.sh`
- `scripts/prod/start-prod.sh`
- `scripts/prod/release-compose.sh`
- `scripts/prod/push-to-dockerhub.sh`

## Test Scripts

- `scripts/test/test-health.sh`
- `scripts/test/test-auth-flow.sh`
- `scripts/test/generate_traffic.sh`
