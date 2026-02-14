#!/usr/bin/env bash

set -euo pipefail

COMPOSE_FILE="${1:-docker-compose.dev.yml}"
CERT_DIR="docker/traefik/certs"
CERT_FILE="${CERT_DIR}/localhost.pem"
KEY_FILE="${CERT_DIR}/localhost-key.pem"
CERT_NAMES=(
  "localhost"
  "*.localhost"
  "tenda.localhost"
  "orun.localhost"
  "keycloak.localhost"
  "traefik.localhost"
)

BLUE='\033[0;34m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
NC='\033[0m'

print_step() {
  echo -e "${GREEN}==>${NC} $1"
}

print_info() {
  echo -e "${BLUE}i${NC} $1"
}

print_warn() {
  echo -e "${YELLOW}Warning:${NC} $1"
}

if [ ! -f "${COMPOSE_FILE}" ]; then
  print_warn "Compose file '${COMPOSE_FILE}' not found. Skipping local TLS setup."
  exit 0
fi

# Only set up local TLS when compose is clearly using localhost hostnames and HTTPS.
# We intentionally avoid matching healthcheck URLs like http://localhost by requiring Host(...) or .localhost.
if ! grep -Eq 'Host\(`[^`]*localhost`\)|\.localhost' "${COMPOSE_FILE}"; then
  print_info "No localhost host rules detected in ${COMPOSE_FILE}; skipping mkcert setup."
  exit 0
fi

if ! grep -Eq "443:443|published:[[:space:]]*\"?443\"?|--entrypoints\\.websecure\\.address=:443|entrypoints=web,websecure|\\.tls=true" "${COMPOSE_FILE}"; then
  print_info "No HTTPS entrypoint detected in ${COMPOSE_FILE}; skipping mkcert setup."
  exit 0
fi

if ! command -v mkcert >/dev/null 2>&1; then
  print_warn "mkcert is not installed. Install mkcert to avoid browser SSL warnings for https://*.localhost."
  print_info "macOS: brew install mkcert nss"
  print_info "Linux/Windows: https://github.com/FiloSottile/mkcert"
  exit 0
fi

mkdir -p "${CERT_DIR}"

if [ -f "${CERT_FILE}" ] && [ -f "${KEY_FILE}" ]; then
  print_info "Local TLS certs already exist at ${CERT_DIR}."
  exit 0
fi

print_step "Installing mkcert local CA (if not already installed)..."
mkcert -install

print_step "Generating TLS certificate for localhost domains..."
mkcert -cert-file "${CERT_FILE}" -key-file "${KEY_FILE}" "${CERT_NAMES[@]}"

print_step "Local TLS certs ready at ${CERT_DIR}"
