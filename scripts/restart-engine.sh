#!/bin/bash
# Restart the Abada Engine container only

echo "Restarting Abada Engine..."
docker compose -f docker-compose.yml -f docker-compose.dev.yml restart abada-engine

echo "Engine restarted."
