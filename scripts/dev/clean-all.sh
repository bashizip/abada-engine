#!/bin/bash
# WARNING: This script deletes all containers and volumes for the dev stack.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"

read -p "Are you sure you want to delete all containers and volumes? (y/N) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]
then
    echo "Cancelled."
    exit 1
fi

echo "Removing all containers and volumes..."
docker compose -f "${ROOT_DIR}/docker-compose.yml" -f "${ROOT_DIR}/docker-compose.dev.yml" down -v --remove-orphans

echo "Cleanup complete."
