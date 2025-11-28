#!/bin/bash
# Build the development stack images

echo "Building Abada Engine Development Stack..."

# Build the jar locally first (required for dev build)
echo "Step 1: Building JAR locally..."
./mvnw clean package spring-boot:repackage -DskipTests

echo "Step 2: Building Docker images..."
docker compose -f docker-compose.yml -f docker-compose.dev.yml build

echo "Build complete."
