#!/bin/bash
# Start the development stack (Infrastructure + Engine)

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"

echo "Starting Abada Engine Development Stack..."
echo "Preparing local TLS certificates..."
"${ROOT_DIR}/scripts/dev/setup-local-tls.sh" "${ROOT_DIR}/docker-compose.dev.yml" || true
docker compose -f "${ROOT_DIR}/docker-compose.yml" -f "${ROOT_DIR}/docker-compose.dev.yml" up -d

echo "Stack started. Access endpoints:"
echo "- Abada Engine: http://localhost:5601"
echo "- Abada Tenda:  http://localhost:5602"
echo "- Abada Orun:   http://localhost:5603"
echo "- Grafana:      http://localhost:3000"
echo "- Jaeger:       http://localhost:16686"
echo "- Traefik:      http://localhost:8080"
