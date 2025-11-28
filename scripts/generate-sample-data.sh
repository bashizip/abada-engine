#!/bin/bash

# Sample Data Generator Script
# This script runs the Abada Engine with sample data generation enabled

set -e

echo "========================================="
echo "Abada Engine - Sample Data Generator"
echo "========================================="
echo ""

# Check if we're in the project root
if [ ! -f "pom.xml" ]; then
    echo "Error: This script must be run from the project root directory"
    exit 1
fi

# Set the property to enable sample data generation
export ABADA_GENERATE_SAMPLE_DATA=true

echo "Starting Abada Engine with sample data generation..."
echo ""

# Run the application with the sample data generator enabled
./mvnw spring-boot:run -Dspring-boot.run.arguments="--abada.generate-sample-data=true"

echo ""
echo "========================================="
echo "Sample data generation complete!"
echo "========================================="
