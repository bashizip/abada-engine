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
LocalFile="docker-compose.release.yml"

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
        curl -sSL "$RELEASE_URL" -o "$LocalFile"
    elif command -v wget &> /dev/null; then
        wget -q "$RELEASE_URL" -O "$LocalFile"
    else
        echo -e "${RED}Error: curl or wget is required to download the configuration.${NC}"
        exit 1
    fi
    echo "✓ Configuration downloaded"
}

setup_local_tls() {
    print_step "Preparing local HTTPS certificates..."

    # Prefer local helper when running from a cloned repository.
    if [ -x "./scripts/dev/setup-local-tls.sh" ]; then
        ./scripts/dev/setup-local-tls.sh "$LocalFile" || true
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
    "$tmp_script" "$LocalFile" || true
    rm -f "$tmp_script"
}

start_platform() {
    print_step "Starting Abada Platform..."
    docker compose -f "$LocalFile" up -d

    if [ $? -eq 0 ]; then
        echo ""
        print_step "Platform available at:"
        echo -e "  - ${BLUE}Engine  ${NC}: http://localhost:5601/api/"
        echo -e "  - ${BLUE}Tenda   ${NC}: http://localhost:5602"
        echo -e "  - ${BLUE}Orun    ${NC}: http://localhost:5603"
        echo -e "  - ${BLUE}Grafana ${NC}: http://localhost:3000"
        echo ""
        echo "Run 'docker compose -f $LocalFile down' to stop."
    else
        echo -e "${RED}Error: Failed to start platform.${NC}"
        exit 1
    fi
}

# Main execution
print_header
check_prerequisites
download_compose
setup_local_tls
start_platform
