#!/bin/bash

# Script to rebuild the abada-tenda:dev Docker image and refresh the running container
# This script will:
# 1. Build the new Docker image
# 2. Stop and remove the old container
# 3. Start a new container with the updated image

set -e  # Exit on any error

CONTAINER_NAME="abada-tenda"
IMAGE_NAME="abada-tenda:dev"
# Get the project root directory (parent of scripts/)
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "=========================================="
echo "Refreshing Tenda Container"
echo "=========================================="
echo ""

# Step 1: Build the Docker image
echo "📦 Building Docker image: $IMAGE_NAME"
echo "------------------------------------------"
docker build -t "$IMAGE_NAME" "$PROJECT_DIR"
echo ""

# Step 2: Stop the running container (if it exists)
echo "🛑 Stopping container: $CONTAINER_NAME"
echo "------------------------------------------"
if docker ps -q -f name="$CONTAINER_NAME" | grep -q .; then
    docker stop "$CONTAINER_NAME"
    echo "Container stopped successfully"
else
    echo "Container is not running"
fi
echo ""

# Step 3: Remove the old container (if it exists)
echo "🗑️  Removing old container: $CONTAINER_NAME"
echo "------------------------------------------"
if docker ps -aq -f name="$CONTAINER_NAME" | grep -q .; then
    docker rm "$CONTAINER_NAME"
    echo "Container removed successfully"
else
    echo "No container to remove"
fi
echo ""

# Step 4: Start the new container
echo "🚀 Starting new container: $CONTAINER_NAME"
echo "------------------------------------------"
docker run -d \
    --name "$CONTAINER_NAME" \
    -p 5602:5602 \
    --restart unless-stopped \
    "$IMAGE_NAME"

echo "Container started successfully"
echo ""

# Step 5: Show container status
echo "✅ Container Status"
echo "------------------------------------------"
docker ps --filter "name=$CONTAINER_NAME" --format "table {{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}"
echo ""

echo "=========================================="
echo "✨ Refresh Complete!"
echo "=========================================="
echo "The Tenda application should be available at: http://localhost:5602"
echo ""
