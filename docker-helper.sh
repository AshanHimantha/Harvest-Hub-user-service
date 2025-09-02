#!/bin/bash

# Harvest Hub User Service Docker Helper Script
# This script helps build and run the user service with Docker

set -e

echo "🚀 Harvest Hub User Service Docker Helper"
echo "=========================================="

# Function to show usage
show_usage() {
    echo "Usage: $0 [build|run|compose-up|compose-down|clean]"
    echo ""
    echo "Commands:"
    echo "  build        - Build the Maven project and Docker image"
    echo "  run          - Run the Docker container (requires external database)"
    echo "  compose-up   - Start with docker-compose (includes database)"
    echo "  compose-down - Stop docker-compose services"
    echo "  clean        - Clean up Docker images and containers"
    echo ""
}

# Function to build the application
build_app() {
    echo "📦 Building Maven project..."
    mvn clean package -DskipTests
    
    echo "🐳 Building Docker image..."
    docker build -t harvest-hub-user-service:latest .
    
    echo "✅ Build completed successfully!"
    echo "   Image: harvest-hub-user-service:latest"
}

# Function to run standalone container
run_container() {
    echo "🐳 Running Docker container..."
    echo "⚠️  Note: This requires an external database to be configured"
    echo ""
    
    docker run -d \
        --name harvest-hub-user-service \
        -p 8081:8081 \
        harvest-hub-user-service:latest
    
    echo "✅ Container started!"
    echo "   Name: harvest-hub-user-service"
    echo "   Port: http://localhost:8081"
    echo ""
    echo "💡 To view logs: docker logs -f harvest-hub-user-service"
    echo "💡 To stop: docker stop harvest-hub-user-service"
}

# Function to start with docker-compose
compose_up() {
    echo "🐳 Starting services with Docker Compose..."
    echo "📋 This will start:"
    echo "   - PostgreSQL database"
    echo "   - User service application"
    echo ""
    
    # Check if user needs to configure environment variables
    if grep -q "your-user-pool-id" docker-compose.yml; then
        echo "⚠️  WARNING: Please update docker-compose.yml with your AWS Cognito configuration:"
        echo "   - AWS_COGNITO_USERPOOLID"
        echo "   - AWS_REGION"
        echo "   - SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI"
        echo ""
        read -p "   Continue anyway? (y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            echo "❌ Cancelled. Please update docker-compose.yml first."
            exit 1
        fi
    fi
    
    docker compose up --build -d
    
    echo "✅ Services started!"
    echo "   Database: localhost:5432"
    echo "   User Service: http://localhost:8081"
    echo ""
    echo "💡 To view logs: docker compose logs -f"
    echo "💡 To stop: docker compose down"
}

# Function to stop docker-compose
compose_down() {
    echo "🛑 Stopping Docker Compose services..."
    docker compose down
    echo "✅ Services stopped!"
}

# Function to clean up
clean_up() {
    echo "🧹 Cleaning up Docker resources..."
    
    # Stop and remove container if it exists
    if docker ps -a --format '{{.Names}}' | grep -q "harvest-hub-user-service"; then
        echo "   Removing harvest-hub-user-service container..."
        docker stop harvest-hub-user-service 2>/dev/null || true
        docker rm harvest-hub-user-service 2>/dev/null || true
    fi
    
    # Remove image if it exists
    if docker images --format '{{.Repository}}:{{.Tag}}' | grep -q "harvest-hub-user-service:latest"; then
        echo "   Removing harvest-hub-user-service:latest image..."
        docker rmi harvest-hub-user-service:latest 2>/dev/null || true
    fi
    
    echo "✅ Cleanup completed!"
}

# Main script logic
case "${1:-}" in
    "build")
        build_app
        ;;
    "run")
        run_container
        ;;
    "compose-up")
        compose_up
        ;;
    "compose-down")
        compose_down
        ;;
    "clean")
        clean_up
        ;;
    *)
        show_usage
        exit 1
        ;;
esac