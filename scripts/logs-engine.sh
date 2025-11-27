#!/bin/bash
# Follow logs for the Abada Engine

docker compose -f docker-compose.yml -f docker-compose.dev.yml logs -f abada-engine
