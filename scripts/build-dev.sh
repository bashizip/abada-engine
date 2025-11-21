#!/bin/bash
# Build the development stack images

echo "Building Abada Engine Development Stack..."
docker compose -f docker-compose.yml -f docker-compose.dev.yml build

echo "Build complete."
