#!/bin/bash

# Start production stack (assumes image is already built)

set -e

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== Starting Abada Engine Production Stack ===${NC}\n"

# Check if image exists
if ! docker images | grep -q "abada-engine.*latest"; then
    echo -e "${YELLOW}Warning: abada-engine:latest image not found${NC}"
    echo -e "${YELLOW}Building image first...${NC}\n"
    ./scripts/prod-build.sh --build-only
    echo ""
fi

echo -e "${YELLOW}Starting production services...${NC}"
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d

echo -e "\n${YELLOW}Waiting for services to be healthy...${NC}"
sleep 5

# Check service status
echo -e "\n${BLUE}Service Status:${NC}"
docker-compose -f docker-compose.yml -f docker-compose.prod.yml ps

echo -e "\n${GREEN}✓ Production stack started!${NC}"
echo -e "\n${BLUE}Service URLs:${NC}"
echo -e "  Application: ${YELLOW}http://localhost/api${NC} (via Traefik load balancer)"
echo -e "  Jaeger UI:   ${YELLOW}http://localhost:16686${NC}"
echo -e "  Grafana:     ${YELLOW}http://localhost:3000${NC} (admin/admin)"
echo -e "  Prometheus:  ${YELLOW}http://localhost:9090${NC}"
echo -e "  Traefik:     ${YELLOW}http://localhost:8080${NC}"
echo -e "  Consul:      ${YELLOW}http://localhost:8500${NC}"

echo -e "\n${BLUE}Useful Commands:${NC}"
echo -e "  View logs:        ${YELLOW}docker-compose -f docker-compose.yml -f docker-compose.prod.yml logs -f${NC}"
echo -e "  View app logs:    ${YELLOW}docker-compose -f docker-compose.yml -f docker-compose.prod.yml logs -f abada-engine${NC}"
echo -e "  Stop stack:       ${YELLOW}docker-compose -f docker-compose.yml -f docker-compose.prod.yml down${NC}"
echo -e "  Restart service:  ${YELLOW}docker-compose -f docker-compose.yml -f docker-compose.prod.yml restart abada-engine${NC}"
echo -e "  Scale replicas:   ${YELLOW}docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d --scale abada-engine=5${NC}"
