#!/bin/bash
# Stop the development stack

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"

echo "Stopping Abada Engine Development Stack..."
echo "Preparing local TLS certificates..."
"${ROOT_DIR}/scripts/dev/setup-local-tls.sh" "${ROOT_DIR}/docker-compose.dev.yml" || true
docker compose -f "${ROOT_DIR}/docker-compose.yml" -f "${ROOT_DIR}/docker-compose.dev.yml" down

echo "Stack stopped."
