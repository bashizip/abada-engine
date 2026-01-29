#!/bin/bash

# Build and Push Abada Platform Images to Docker Hub
# This script builds production-ready images and pushes them to Docker Hub

set -e  # Exit on error

# Configuration
DOCKER_USERNAME="${DOCKER_USERNAME:-}"
VERSION="${VERSION:-latest}"
PLATFORM_DIR="$(dirname "$(dirname "$(dirname "$(realpath "$0")")")")"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored messages
print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if Docker is running
check_docker() {
    if ! docker info > /dev/null 2>&1; then
        print_error "Docker is not running. Please start Docker and try again."
        exit 1
    fi
    print_info "Docker is running"
}

# Function to login to Docker Hub
docker_login() {
    if [ -z "$DOCKER_USERNAME" ]; then
        print_error "DOCKER_USERNAME environment variable is not set."
        echo "Please run: export DOCKER_USERNAME=your_dockerhub_username"
        exit 1
    fi
    
    print_info "Logging into Docker Hub as $DOCKER_USERNAME..."
    if ! docker login -u "$DOCKER_USERNAME"; then
        print_error "Docker Hub login failed"
        exit 1
    fi
    print_info "Successfully logged into Docker Hub"
}

# Function to build and push an image
build_and_push() {
    local component=$1
    local context_dir=$2
    local dockerfile=$3
    local image_name="${DOCKER_USERNAME}/${component}"
    local full_tag="${image_name}:${VERSION}"
    local latest_tag="${image_name}:latest"
    
    print_info "Building $component..."
    print_info "Context: $context_dir"
    print_info "Dockerfile: $dockerfile"
    
    # Build the image
    if docker build -f "$dockerfile" -t "$full_tag" -t "$latest_tag" "$context_dir"; then
        print_info "Successfully built $component"
    else
        print_error "Failed to build $component"
        return 1
    fi
    
    # Push the versioned tag
    print_info "Pushing $full_tag..."
    if docker push "$full_tag"; then
        print_info "Successfully pushed $full_tag"
    else
        print_error "Failed to push $full_tag"
        return 1
    fi
    
    # Push the latest tag (only if VERSION is not 'latest')
    if [ "$VERSION" != "latest" ]; then
        print_info "Pushing $latest_tag..."
        if docker push "$latest_tag"; then
            print_info "Successfully pushed $latest_tag"
        else
            print_error "Failed to push $latest_tag"
            return 1
        fi
    fi
    
    print_info "✓ Completed $component"
}

# Main execution
main() {
    print_info "=== Abada Platform Docker Hub Push Script ==="
    print_info "Platform directory: $PLATFORM_DIR"
    print_info "Version: $VERSION"
    echo ""
    
    # Check prerequisites
    check_docker
    docker_login
    echo ""
    
    # Build and push each component
    print_info "=== Building and Pushing Images ==="
    echo ""
    
    # 1. Abada Engine
    print_info "--- Building Abada Engine ---"
    build_and_push \
        "abada-engine" \
        "$PLATFORM_DIR/abada-engine" \
        "$PLATFORM_DIR/abada-engine/Dockerfile.prod"
    echo ""
    
    # 2. Abada Tenda
    print_info "--- Building Abada Tenda ---"
    build_and_push \
        "abada-tenda" \
        "$PLATFORM_DIR/abada-tenda" \
        "$PLATFORM_DIR/abada-tenda/Dockerfile.prod"
    echo ""
    
    # 3. Abada Orun
    print_info "--- Building Abada Orun ---"
    build_and_push \
        "abada-orun" \
        "$PLATFORM_DIR/abada-orun" \
        "$PLATFORM_DIR/abada-orun/Dockerfile.prod"
    echo ""
    
    # Summary
    print_info "=== Build and Push Complete ==="
    echo ""
    print_info "Successfully built and pushed the following images:"
    echo "  - ${DOCKER_USERNAME}/abada-engine:${VERSION}"
    echo "  - ${DOCKER_USERNAME}/abada-tenda:${VERSION}"
    echo "  - ${DOCKER_USERNAME}/abada-orun:${VERSION}"
    
    if [ "$VERSION" != "latest" ]; then
        echo ""
        print_info "Also tagged as :latest"
    fi
    
    echo ""
    print_info "Users can now run the platform with:"
    echo "  docker-compose -f docker-compose.hub.yml up -d"
}

# Run main function
main
