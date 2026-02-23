#!/bin/bash
# Follow logs for the Abada Engine

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"

echo "Preparing local TLS certificates..."
"${ROOT_DIR}/scripts/dev/setup-local-tls.sh" "${ROOT_DIR}/docker-compose.dev.yml" || true
docker compose -f "${ROOT_DIR}/docker-compose.yml" -f "${ROOT_DIR}/docker-compose.dev.yml" logs -f abada-engine
