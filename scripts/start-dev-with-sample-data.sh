#!/bin/bash

# Start Dev Environment with Sample Data
# This script sets the environment variable to enable sample data generation
# and then calls the standard start-dev.sh script.

set -e

echo "========================================="
echo "Abada Engine - Dev Start with Sample Data"
echo "========================================="
echo ""

# Set the property to enable sample data generation
export ABADA_GENERATE_SAMPLE_DATA=true

echo "Enabling sample data generation (ABADA_GENERATE_SAMPLE_DATA=true)..."
echo "Starting development environment..."
echo ""

# Execute the standard start-dev script
./scripts/start-dev.sh
