#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

BASE_URL="http://localhost:5601/abada/api"
ITERATIONS=20

echo -e "${BLUE}Starting traffic generation for Jaeger troubleshooting...${NC}"
echo -e "${BLUE}Target URL: ${BASE_URL}${NC}"
echo -e "${BLUE}Iterations: ${ITERATIONS}${NC}"

for ((i=1; i<=ITERATIONS; i++)); do
    echo -e "\n${GREEN}Iteration $i/$ITERATIONS${NC}"
    
    # Hit Health Endpoint
    echo -n "  GET /actuator/health ... "
    curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/actuator/health"
    echo ""

    # Hit Root Endpoint
    echo -n "  GET /v1 ... "
    curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/v1"
    echo ""

    # Hit Tasks Endpoint
    echo -n "  GET /v1/tasks ... "
    curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/v1/tasks"
    echo ""

    sleep 0.5
done

echo -e "\n${BLUE}Traffic generation complete.${NC}"
