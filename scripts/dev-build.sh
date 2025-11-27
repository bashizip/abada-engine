#!/bin/bash

# Helper script for building and running the dev environment

set -e

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== Abada Engine Dev Build ===${NC}\n"

# Build the jar locally for faster Docker builds
echo -e "${YELLOW}Step 1: Building JAR locally...${NC}"
./mvnw clean package spring-boot:repackage -DskipTests

echo -e "\n${YELLOW}Step 2: Building and starting Docker containers...${NC}"
docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d --build abada-engine

echo -e "\n${YELLOW}Step 3: Waiting for application to start...${NC}"
sleep 10

echo -e "\n${GREEN}✓ Build complete!${NC}"
echo -e "${BLUE}Application is starting at http://localhost:5601/abada/api${NC}"
echo -e "${BLUE}Jaeger UI: http://localhost:16686${NC}"
echo -e "${BLUE}Grafana: http://localhost:3000${NC}"
echo -e "\n${YELLOW}To view logs:${NC} docker logs -f abada-engine"
echo -e "${YELLOW}To generate test traffic:${NC} ./scripts/generate_traffic.sh"
