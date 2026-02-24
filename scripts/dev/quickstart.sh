#!/usr/bin/env bash

set -euo pipefail

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"

print_banner() {
    echo -e "${BLUE}"
    cat << 'EOF'
 _    _  _____  __   __ _____  _____  _______  _____   _____   _____  _______
| |  | ||_   _| \ \ / /|_   _||  _  ||__   __||  _  | /  ___||  _  ||_   _  |
| |  | |  | |    \ V /   | |  | | | |   | |   | | | | \ `--. | | | |  | |  |
| |/\| |  | |     \ /    | |  | | | |   | |   | | | |  `--. \| | | |  | |  |
\  /\  / _| |_    | |    | |  \ \_/ /   | |   \ \_/ / /\__/ /| \_/ / _| |_  |
 \/  \/ |_____|   \_/    \_/   \___/    \_/    \___/  \____/  \___/ |_____| |
EOF
    echo -e "${NC}"
    echo -e "${BLUE}=== Abada Platform - Development Quick Start ===${NC}\n"
}

check_prerequisites() {
    echo -e "${YELLOW}Checking prerequisites...${NC}"

    # Check Docker
    if ! command -v docker &> /dev/null; then
        echo -e "${RED}❌ Docker is not installed.${NC}"
        echo "Please install Docker Desktop: https://www.docker.com/products/docker-desktop"
        exit 1
    fi
    echo -e "${GREEN}✓${NC} Docker is installed ($(docker --version | cut -d' ' -f3))"

    # Check Docker Compose
    if ! docker compose version &> /dev/null; then
        echo -e "${RED}❌ Docker Compose is not installed.${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓${NC} Docker Compose is installed ($(docker compose version --short))"

    # Check if ports are available
    for port in 80 443 3000 8080 9090 16686; do
        if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null 2>&1 ; then
            echo -e "${YELLOW}⚠${NC} Port $port is already in use. This may cause issues."
        fi
    done

    echo ""
}

setup_tls() {
    echo -e "${YELLOW}Setting up local TLS certificates...${NC}"

    # Check if mkcert is installed
    if command -v mkcert &> /dev/null; then
        echo -e "${GREEN}✓${NC} mkcert found, generating trusted certificates..."
        "${ROOT_DIR}/scripts/dev/setup-local-tls.sh" "${ROOT_DIR}/docker-compose.dev.yml" || {
            echo -e "${YELLOW}⚠${NC} Certificate generation failed. Continuing with self-signed certs."
        }
    else
        echo -e "${YELLOW}⚠${NC} mkcert not found. Using self-signed certificates."
        echo "To avoid browser SSL warnings, install mkcert: https://github.com/FiloSottile/mkcert"
    fi
    echo ""
}

start_stack() {
    echo -e "${YELLOW}Starting Abada Platform stack...${NC}"
    echo -e "${BLUE}This may take a few minutes on first run...${NC}\n"

    cd "${ROOT_DIR}"

    # Start all services
    docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d

    echo ""
    echo -e "${GREEN}✓${NC} Containers started!"
    echo ""
}

wait_for_services() {
    echo -e "${YELLOW}Waiting for services to be healthy...${NC}"

    # Wait for engine to be healthy
    max_attempts=30
    attempt=1

    while [ $attempt -le $max_attempts ]; do
        if docker compose -f docker-compose.yml -f docker-compose.dev.yml ps abada-engine | grep -q "healthy"; then
            echo -e "${GREEN}✓${NC} Engine is healthy"
            break
        fi
        echo -ne "${BLUE}Waiting for engine...${NC} (attempt $attempt/$max_attempts)\r"
        sleep 2
        ((attempt++))
    done

    if [ $attempt -gt $max_attempts ]; then
        echo -e "${YELLOW}⚠${NC} Engine took longer than expected to start. Check logs with:"
        echo "  docker compose -f docker-compose.yml -f docker-compose.dev.yml logs abada-engine"
    fi

    echo ""
}

show_status() {
    echo -e "${BLUE}=== Service Status ===${NC}"
    docker compose -f docker-compose.yml -f docker-compose.dev.yml ps
    echo ""
}

show_access_info() {
    echo -e "${GREEN}============================================${NC}"
    echo -e "${GREEN}  🎉 Abada Platform is running!${NC}"
    echo -e "${GREEN}============================================${NC}"
    echo ""
    echo -e "${BLUE}Service URLs:${NC}"
    echo -e "  🔌 Engine API:     ${YELLOW}https://localhost/api/${NC}"
    echo -e "  📚 Swagger UI:    ${YELLOW}https://localhost/api/swagger-ui.html${NC}"
    echo -e "  📋 Tenda UI:      ${YELLOW}https://tenda.localhost${NC}"
    echo -e "  📊 Orun Dashboard:${YELLOW}https://orun.localhost${NC}"
    echo -e "  🔐 Keycloak:      ${YELLOW}https://keycloak.localhost${NC}"
    echo -e "  📈 Grafana:       ${YELLOW}https://grafana.localhost${NC} (admin/admin123)"
    echo -e "  🔍 Jaeger:        ${YELLOW}https://jaeger.localhost${NC}"
    echo -e "  🚦 Traefik:       ${YELLOW}https://traefik.localhost${NC}"
    echo ""
    echo -e "${BLUE}Test Credentials:${NC}"
    echo -e "  👤 Admin:  ${YELLOW}admin / admin${NC} (Platform Administrator)"
    echo -e "  👤 Alice:  ${YELLOW}alice / alice${NC} (Customer)"
    echo -e "  👤 Bob:    ${YELLOW}bob / bob${NC} (Custos)"
    echo ""
    echo -e "${BLUE}Useful Commands:${NC}"
    echo -e "  📋 View logs:     ${YELLOW}docker compose -f docker-compose.yml -f docker-compose.dev.yml logs -f${NC}"
    echo -e "  🛑 Stop stack:    ${YELLOW}docker compose -f docker-compose.yml -f docker-compose.dev.yml down${NC}"
    echo -e "  🔄 Restart:       ${YELLOW}docker compose -f docker-compose.yml -f docker-compose.dev.yml restart${NC}"
    echo -e "  📊 Status:        ${YELLOW}docker compose -f docker-compose.yml -f docker-compose.dev.yml ps${NC}"
    echo ""
    echo -e "${YELLOW}⚠${NC} Note: This is a ${BLUE}development/demo${NC} environment."
    echo -e "${YELLOW}⚠${NC} For production deployment, see docs/operations/docker-deployment.md"
    echo ""
}

# Main execution
print_banner
check_prerequisites
setup_tls
start_stack
wait_for_services
show_status
show_access_info

echo -e "${GREEN}Done!${NC}\n"
