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
    if [ -f "$LocalFile" ]; then
        print_step "Using existing $LocalFile"
    else
        print_step "Downloading configuration..."
        if command -v curl &> /dev/null; then
            curl -sSL "$RELEASE_URL" -o "$LocalFile"
        elif command -v wget &> /dev/null; then
            wget -q "$RELEASE_URL" -O "$LocalFile"
        else
            echo -e "${RED}Error: curl or wget is required to download the configuration.${NC}"
            exit 1
        fi
        echo "✓ Configuration downloaded"
    fi
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
start_platform
