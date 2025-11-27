#!/bin/bash
# Stop the development stack

echo "Stopping Abada Engine Development Stack..."
docker compose -f docker-compose.yml -f docker-compose.dev.yml down

echo "Stack stopped."
