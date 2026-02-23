#!/bin/bash

# Helper script for building and running the FULL dev environment (Engine + Observability + DB)

set -e

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== Abada Engine Full Stack Build & Run ===${NC}\n"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"

# Check for Java/Maven
if ! command -v java &> /dev/null; then
    echo -e "${YELLOW}Warning: Java not found. Local build will likely fail.${NC}"
    echo -e "${YELLOW}Ensure you have Java 21+ installed to use the fast local build.${NC}\n"
fi

# Build the jar locally for faster Docker builds
echo -e "${YELLOW}Step 1: Building JAR locally...${NC}"
cd "${ROOT_DIR}/engine" && ./mvnw clean package spring-boot:repackage -DskipTests

echo -e "\n${YELLOW}Step 1.5: Checking Sibling Projects...${NC}"

# Build Abada Tenda if available (just the image, not the container)
if [ -d "${ROOT_DIR}/tenda" ]; then
    echo -e "${BLUE}Found ./tenda. Building abada-tenda:dev image...${NC}"
    (cd "${ROOT_DIR}/tenda" && docker build -t abada-tenda:dev .)
else
    echo -e "${YELLOW}./tenda not found. Skipping build.${NC}"
fi

# Build Abada Orun if available (just the image, not the container)
if [ -d "${ROOT_DIR}/orun" ]; then
    echo -e "${BLUE}Found ./orun. Building abada-orun:dev image...${NC}"
    (cd "${ROOT_DIR}/orun" && docker build -t abada-orun:dev .)
else
    echo -e "${YELLOW}./orun not found. Skipping build.${NC}"
fi


echo -e "\n${YELLOW}Step 2: Cleaning up existing environment...${NC}"
# We clean up to ensure fresh state, but we could make this optional
docker stop abada-engine 2>/dev/null || true
# We don't remove the image here to allow cache usage, but we force rebuild next

echo -e "\n${YELLOW}Step 3: Preparing local TLS certificates...${NC}"
"${ROOT_DIR}/scripts/dev/setup-local-tls.sh" "${ROOT_DIR}/docker-compose.dev.yml" || true

echo -e "\n${YELLOW}Step 4: Building and starting ALL services...${NC}"
# We build abada-engine specifically to ensure the local jar is picked up, then up everything
docker-compose -f "${ROOT_DIR}/docker-compose.yml" -f "${ROOT_DIR}/docker-compose.dev.yml" up -d --build

echo -e "\n${YELLOW}Step 5: Waiting for services to stabilize...${NC}"
sleep 10

echo -e "\n${GREEN}✓ Stack is up!${NC}"
echo -e "${BLUE}Services:${NC}"
echo -e "- Abada Engine:    https://localhost/api/info"
echo -e "- Abada Tenda:     https://tenda.localhost"
echo -e "- Abada Orun:      https://orun.localhost"
echo -e "- Grafana:         http://localhost:3000 (admin/admin123)"
echo -e "- Jaeger:          http://localhost:16686"
echo -e "- Traefik:         http://localhost:8080"
echo -e "\n${YELLOW}To view logs:${NC} docker-compose -f ${ROOT_DIR}/docker-compose.yml -f ${ROOT_DIR}/docker-compose.dev.yml logs -f"
