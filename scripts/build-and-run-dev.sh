#!/bin/bash

# Helper script for building and running the FULL dev environment (Engine + Observability + DB)

set -e

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== Abada Engine Full Stack Build & Run ===${NC}\n"

# Check for Java/Maven
if ! command -v java &> /dev/null; then
    echo -e "${YELLOW}Warning: Java not found. Local build will likely fail.${NC}"
    echo -e "${YELLOW}Ensure you have Java 21+ installed to use the fast local build.${NC}\n"
fi

# Build the jar locally for faster Docker builds
echo -e "${YELLOW}Step 1: Building JAR locally...${NC}"
./mvnw clean package spring-boot:repackage -DskipTests

echo -e "\n${YELLOW}Step 2: Cleaning up existing environment...${NC}"
# We clean up to ensure fresh state, but we could make this optional
docker stop abada-engine 2>/dev/null || true
# We don't remove the image here to allow cache usage, but we force rebuild next

echo -e "\n${YELLOW}Step 3: Building and starting ALL services...${NC}"
# We build abada-engine specifically to ensure the local jar is picked up, then up everything
docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d --build

echo -e "\n${YELLOW}Step 4: Waiting for services to stabilize...${NC}"
sleep 10

echo -e "\n${GREEN}✓ Stack is up!${NC}"
echo -e "${BLUE}Services:${NC}"
echo -e "- Abada Engine:    http://localhost:5601/abada/api"
echo -e "- Abada Tenda:     http://localhost:5602"
echo -e "- Abada Orun:      http://localhost:5603"
echo -e "- Grafana:         http://localhost:3000 (admin/admin123)"
echo -e "- Jaeger:          http://localhost:16686"
echo -e "- Traefik:         http://localhost:8080"
echo -e "\n${YELLOW}To view logs:${NC} docker-compose -f docker-compose.yml -f docker-compose.dev.yml logs -f"
