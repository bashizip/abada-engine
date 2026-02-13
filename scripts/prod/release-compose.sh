#!/bin/bash

# Script to merge docker-compose files for release
# Generates a single docker-compose.release.yml file

set -e

# Configuration
VERSION="${VERSION:-latest}"
PLATFORM_DIR="$(dirname "$(dirname "$(dirname "$(realpath "$0")")")")"
OUTPUT_FILE="$PLATFORM_DIR/release/docker-compose.release.yml"

# Colors
GREEN='\033[0;32m'
NC='\033[0m'

echo -e "${GREEN}[INFO]${NC} Generating release compose file..."

# Use docker compose config to merge files
# We merge: base + prod + hub
# This gives us:
# - All services from base
# - Prod configurations (postgres, multiple replicas, etc)
# - Hub images (instead of local build)

if ! docker compose \
    -f "$PLATFORM_DIR/docker-compose.yml" \
    -f "$PLATFORM_DIR/docker-compose.prod.yml" \
    -f "$PLATFORM_DIR/docker-compose.hub.yml" \
    config > "$OUTPUT_FILE.tmp"; then
    echo "Error: Failed to generate compose config"
    exit 1
fi

# Sanitize absolute paths to relative ones to make the file portable
# We replace the absolute project root with '.'
sed "s|$PLATFORM_DIR|.|g" "$OUTPUT_FILE.tmp" > "$OUTPUT_FILE"
rm "$OUTPUT_FILE.tmp"

echo -e "${GREEN}[INFO]${NC} Successfully generated: $OUTPUT_FILE"
echo "You can test it with:"
echo "cd release && docker compose -f docker-compose.release.yml up -d"
