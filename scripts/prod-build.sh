#!/bin/bash

# Production build script for Abada Engine
# This builds the Docker image from source (no local JAR required)

set -e

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== Abada Engine Production Build ===${NC}\n"

# Parse command line arguments
BUILD_ONLY=false
if [ "$1" = "--build-only" ]; then
    BUILD_ONLY=true
fi

echo -e "${YELLOW}Building Docker image from source...${NC}"
echo -e "${BLUE}This will download Maven dependencies and build inside Docker${NC}\n"

# Build the image (USE_LOCAL_JAR defaults to false)
docker-compose -f docker-compose.yml -f docker-compose.prod.yml build abada-engine

echo -e "\n${GREEN}✓ Docker image built successfully!${NC}"

if [ "$BUILD_ONLY" = "false" ]; then
    echo -e "\n${YELLOW}Starting production stack...${NC}"
    docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
    
    echo -e "\n${GREEN}✓ Production stack started!${NC}"
    echo -e "${BLUE}Application: http://localhost/abada/api (via Traefik)${NC}"
    echo -e "${BLUE}Jaeger UI: http://localhost:16686${NC}"
    echo -e "${BLUE}Grafana: http://localhost:3000${NC}"
    echo -e "\n${YELLOW}To view logs:${NC} docker-compose -f docker-compose.yml -f docker-compose.prod.yml logs -f"
else
    echo -e "\n${BLUE}Image built but not started (--build-only flag)${NC}"
    echo -e "${YELLOW}To start:${NC} docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d"
fi
