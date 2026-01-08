#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_DIR"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
REGISTRY="${REGISTRY:-ghcr.io}"
IMAGE_NAME="${IMAGE_NAME:-$(basename "$PROJECT_DIR")}"
IMAGE_TAG="${IMAGE_TAG:-latest}"

print_help() {
    echo "Chatkeep Deployment Script"
    echo ""
    echo "Usage: ./scripts/deploy.sh [command]"
    echo ""
    echo "Commands:"
    echo "  build     Build Docker image locally"
    echo "  push      Build and push to registry"
    echo "  pull      Pull latest image from registry"
    echo "  up        Start production containers"
    echo "  down      Stop production containers"
    echo "  restart   Pull latest and restart"
    echo "  status    Show container status"
    echo "  logs      Show container logs"
    echo "  help      Show this help message"
    echo ""
    echo "Environment variables:"
    echo "  REGISTRY      Container registry (default: ghcr.io)"
    echo "  IMAGE_NAME    Image name (default: chatkeep)"
    echo "  IMAGE_TAG     Image tag (default: latest)"
    echo ""
    echo "Required for production:"
    echo "  TELEGRAM_BOT_TOKEN  Telegram bot token"
    echo "  DB_PASSWORD         Database password"
}

check_env() {
    local missing=0

    if [ -z "$TELEGRAM_BOT_TOKEN" ]; then
        echo -e "${RED}Error: TELEGRAM_BOT_TOKEN is not set${NC}"
        missing=1
    fi

    if [ -z "$DB_PASSWORD" ]; then
        echo -e "${RED}Error: DB_PASSWORD is not set${NC}"
        missing=1
    fi

    if [ $missing -eq 1 ]; then
        echo ""
        echo "Set required environment variables or create a .env file"
        exit 1
    fi
}

build_image() {
    echo -e "${GREEN}Building Docker image...${NC}"
    docker build -t "$REGISTRY/$IMAGE_NAME:$IMAGE_TAG" .
    echo -e "${GREEN}Image built: $REGISTRY/$IMAGE_NAME:$IMAGE_TAG${NC}"
}

push_image() {
    build_image
    echo -e "${GREEN}Pushing to registry...${NC}"
    docker push "$REGISTRY/$IMAGE_NAME:$IMAGE_TAG"
    echo -e "${GREEN}Image pushed!${NC}"
}

case "${1:-help}" in
    build)
        build_image
        ;;
    push)
        push_image
        ;;
    pull)
        echo -e "${GREEN}Pulling latest image...${NC}"
        docker pull "$REGISTRY/$IMAGE_NAME:$IMAGE_TAG"
        ;;
    up)
        check_env
        echo -e "${GREEN}Starting production containers...${NC}"
        docker compose -f docker-compose.prod.yml up -d
        echo -e "${GREEN}Containers started!${NC}"
        ;;
    down)
        echo -e "${GREEN}Stopping production containers...${NC}"
        docker compose -f docker-compose.prod.yml down
        ;;
    restart)
        check_env
        echo -e "${GREEN}Pulling latest and restarting...${NC}"
        docker compose -f docker-compose.prod.yml pull
        docker compose -f docker-compose.prod.yml up -d

        # Wait for health check
        echo -e "${YELLOW}Waiting for application to become healthy...${NC}"
        sleep 10
        for i in {1..30}; do
            if docker compose -f docker-compose.prod.yml ps | grep -q "healthy"; then
                echo -e "${GREEN}Application is healthy!${NC}"
                break
            fi
            echo -n "."
            sleep 2
        done

        docker image prune -f
        echo -e "${GREEN}Restart complete!${NC}"
        ;;
    status)
        docker compose -f docker-compose.prod.yml ps
        ;;
    logs)
        docker compose -f docker-compose.prod.yml logs -f
        ;;
    help|--help|-h)
        print_help
        ;;
    *)
        echo -e "${RED}Unknown command: $1${NC}"
        print_help
        exit 1
        ;;
esac
