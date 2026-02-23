#!/bin/bash

# Abada Platform Quickstart Script
# Downloads and starts the Abada Platform

set -e

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Configuration
RELEASE_URL="https://raw.githubusercontent.com/bashizip/abada-engine/main/release/docker-compose.release.yml"
TLS_SETUP_URL="https://raw.githubusercontent.com/bashizip/abada-engine/main/scripts/dev/setup-local-tls.sh"
LOCAL_FILE="docker-compose.release.yml"

print_header() {
    echo -e "${BLUE}"
    echo "    _    _               _        "
    echo "   / \  | |__   __ _  __| | __ _  "
    echo "  / _ \ | '_ \ / _\` |/ _\` |/ _\` | "
    echo " / ___ \| |_) | (_| | (_| | (_| | "
    echo "/_/   \_\_.__/ \__,_|\__,_|\__,_| "
    echo "                                  "
    echo "   Quickstart Launcher            "
    echo -e "${NC}"
}

print_step() {
    echo -e "${GREEN}==>${NC} $1"
}

check_prerequisites() {
    print_step "Checking prerequisites..."
    if ! command -v docker &> /dev/null; then
        echo -e "${RED}Error: docker is not installed.${NC}"
        echo "Please install Docker Desktop from https://www.docker.com/products/docker-desktop"
        exit 1
    fi

    # Check for docker compose (v2)
    if ! docker compose version &> /dev/null; then
        echo -e "${RED}Error: docker compose (v2) is not available.${NC}"
        echo "Please make sure you have a recent version of Docker Desktop installed."
        exit 1
    fi
    echo "✓ Docker and Docker Compose found"
}

download_compose() {
    print_step "Downloading latest configuration..."
    if command -v curl &> /dev/null; then
        curl -sSL "$RELEASE_URL" -o "$LOCAL_FILE"
    elif command -v wget &> /dev/null; then
        wget -q "$RELEASE_URL" -O "$LOCAL_FILE"
    else
        echo -e "${RED}Error: curl or wget is required to download the configuration.${NC}"
        exit 1
    fi
    echo "✓ Configuration downloaded"
}


check_release_assets() {
    local required_paths=(
        "./docker/grafana/provisioning"
        "./docker/grafana/dashboards"
        "./docker/loki-config-prod.yaml"
        "./docker/otel-collector-config.yaml"
        "./docker/prometheus.yml"
        "./docker/promtail-config.yaml"
        "./docker/traefik/dynamic.yml"
        "./docker/traefik/traefik.yml"
    )

    local missing=()
    for path in "${required_paths[@]}"; do
        if [ ! -e "$path" ]; then
            missing+=("$path")
        fi
    done

    if [ ${#missing[@]} -gt 0 ]; then
        echo -e "${RED}Error: release assets are missing for docker-compose.release.yml.${NC}"
        echo "Run this quickstart from the repository root, or use a release bundle that includes the docker/ directory."
        echo "Missing paths:"
        for path in "${missing[@]}"; do
            echo "  - $path"
        done
        exit 1
    fi

    mkdir -p ./logs
}
setup_local_tls() {
    print_step "Preparing local HTTPS certificates..."

    # Prefer local helper when running from a cloned repository.
    if [ -x "./scripts/dev/setup-local-tls.sh" ]; then
        ./scripts/dev/setup-local-tls.sh "$LOCAL_FILE"
        return
    fi

    # Fallback for curl|bash usage where repo files are not present locally.
    local tmp_script
    tmp_script="$(mktemp)"

    if command -v curl &> /dev/null; then
        if ! curl -sSL "$TLS_SETUP_URL" -o "$tmp_script"; then
            echo "i Could not download TLS helper via curl. Skipping."
            rm -f "$tmp_script"
            return
        fi
    elif command -v wget &> /dev/null; then
        if ! wget -q "$TLS_SETUP_URL" -O "$tmp_script"; then
            echo "i Could not download TLS helper via wget. Skipping."
            rm -f "$tmp_script"
            return
        fi
    else
        echo "i Could not download TLS helper (curl/wget not found). Skipping."
        rm -f "$tmp_script"
        return
    fi

    chmod +x "$tmp_script"
    "$tmp_script" "$LOCAL_FILE"
    rm -f "$tmp_script"
}

verify_local_tls() {
    print_step "Verifying local TLS certificate and trust..."

    local cert_file="./docker/traefik/certs/localhost.pem"
    local key_file="./docker/traefik/certs/localhost-key.pem"

    if ! command -v mkcert &> /dev/null; then
        echo -e "${RED}Error: mkcert is required to generate trusted local HTTPS certificates.${NC}"
        echo "Install mkcert, then rerun quickstart."
        exit 1
    fi

    local caroot
    caroot="$(mkcert -CAROOT 2>/dev/null || true)"
    if [ -z "$caroot" ] || [ ! -f "$caroot/rootCA.pem" ]; then
        echo -e "${RED}Error: mkcert root CA is not installed correctly on this machine.${NC}"
        echo "Run 'mkcert -install' and rerun quickstart."
        exit 1
    fi

    if [ ! -s "$cert_file" ] || [ ! -s "$key_file" ]; then
        echo -e "${RED}Error: local TLS cert files are missing.${NC}"
        echo "Expected:"
        echo "  - $cert_file"
        echo "  - $key_file"
        exit 1
    fi

    if command -v openssl &> /dev/null; then
        local required_names=(
            "localhost"
            "tenda.localhost"
            "orun.localhost"
            "grafana.localhost"
            "traefik.localhost"
        )

        local cert_text
        cert_text="$(openssl x509 -in "$cert_file" -text -noout 2>/dev/null || true)"
        if [ -z "$cert_text" ]; then
            echo -e "${RED}Error: unable to read TLS certificate at $cert_file.${NC}"
            exit 1
        fi

        local name
        for name in "${required_names[@]}"; do
            if ! printf '%s\n' "$cert_text" | grep -Fq "DNS:${name}"; then
                echo -e "${RED}Error: TLS certificate is missing SAN '${name}'.${NC}"
                exit 1
            fi
        done

        if ! openssl verify -CAfile "$caroot/rootCA.pem" "$cert_file" >/dev/null 2>&1; then
            echo -e "${RED}Error: certificate verification failed with mkcert root CA.${NC}"
            exit 1
        fi
    fi

    echo "✓ Local HTTPS certificate is installed and valid for *.localhost"
}

start_platform() {
    print_step "Starting Abada Platform..."
    docker compose -f "$LOCAL_FILE" up -d

    if [ $? -eq 0 ]; then
        echo ""
        print_step "Platform available at:"
        echo -e "  - ${BLUE}Engine API      ${NC}: https://localhost/api/"
        echo -e "  - ${BLUE}Tenda UI        ${NC}: https://tenda.localhost"
        echo -e "  - ${BLUE}Orun UI         ${NC}: https://orun.localhost"
        echo -e "  - ${BLUE}Grafana         ${NC}: https://grafana.localhost"
        echo -e "  - ${BLUE}Traefik         ${NC}: https://traefik.localhost"
        echo ""
        echo "Run 'docker compose -f $LOCAL_FILE down' to stop."
    else
        echo -e "${RED}Error: Failed to start platform.${NC}"
        exit 1
    fi
}

# Main execution
print_header
check_prerequisites
download_compose
check_release_assets
setup_local_tls
verify_local_tls
start_platform
