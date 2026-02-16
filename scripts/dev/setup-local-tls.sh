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
  "grafana.localhost"
  "jaeger.localhost"
  "prometheus.localhost"
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

cert_has_all_names() {
  local cert_file="$1"

  if ! command -v openssl >/dev/null 2>&1; then
    # If openssl is not available, fall back to keeping existing certs.
    return 0
  fi

  local cert_text
  cert_text="$(openssl x509 -in "${cert_file}" -text -noout 2>/dev/null || true)"
  if [ -z "${cert_text}" ]; then
    return 1
  fi

  local name
  for name in "${CERT_NAMES[@]}"; do
    if ! printf '%s\n' "${cert_text}" | grep -Fq "DNS:${name}"; then
      return 1
    fi
  done

  return 0
}

if [ -f "${CERT_FILE}" ] && [ -f "${KEY_FILE}" ]; then
  if cert_has_all_names "${CERT_FILE}"; then
    print_info "Local TLS certs already exist at ${CERT_DIR} and match required hostnames."
    exit 0
  fi

  print_warn "Existing TLS cert is missing one or more required hostnames. Regenerating..."
  rm -f "${CERT_FILE}" "${KEY_FILE}"
fi

print_step "Installing mkcert local CA (if not already installed)..."
mkcert -install

print_step "Generating TLS certificate for localhost domains..."
mkcert -cert-file "${CERT_FILE}" -key-file "${KEY_FILE}" "${CERT_NAMES[@]}"

print_step "Local TLS certs ready at ${CERT_DIR}"
