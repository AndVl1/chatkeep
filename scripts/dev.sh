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

print_help() {
    echo "Chatkeep Development Helper"
    echo ""
    echo "Usage: ./scripts/dev.sh [command]"
    echo ""
    echo "Commands:"
    echo "  start     Start PostgreSQL and run the app (default)"
    echo "  db        Start only PostgreSQL"
    echo "  app       Run the app (assumes DB is running)"
    echo "  test      Run tests"
    echo "  build     Build the project"
    echo "  docker    Build and run with Docker Compose"
    echo "  stop      Stop all Docker containers"
    echo "  logs      Show Docker logs"
    echo "  clean     Clean build artifacts and Docker volumes"
    echo "  help      Show this help message"
}

start_db() {
    echo -e "${GREEN}Starting PostgreSQL...${NC}"
    docker compose up -d db
    echo -e "${GREEN}Waiting for database to be ready...${NC}"
    sleep 3
}

run_app() {
    echo -e "${GREEN}Starting application...${NC}"
    ./gradlew bootRun
}

case "${1:-start}" in
    start)
        start_db
        run_app
        ;;
    db)
        start_db
        echo -e "${GREEN}PostgreSQL is running on localhost:5432${NC}"
        ;;
    app)
        run_app
        ;;
    test)
        echo -e "${GREEN}Running tests...${NC}"
        ./gradlew test
        ;;
    build)
        echo -e "${GREEN}Building project...${NC}"
        ./gradlew build
        ;;
    docker)
        if [ -z "$TELEGRAM_BOT_TOKEN" ]; then
            echo -e "${YELLOW}Warning: TELEGRAM_BOT_TOKEN not set. Bot will not work.${NC}"
            echo -e "${YELLOW}Set it with: export TELEGRAM_BOT_TOKEN=your_token${NC}"
        fi
        echo -e "${GREEN}Building and starting with Docker Compose...${NC}"
        docker compose up --build
        ;;
    stop)
        echo -e "${GREEN}Stopping all containers...${NC}"
        docker compose down
        ;;
    logs)
        docker compose logs -f
        ;;
    clean)
        echo -e "${YELLOW}Cleaning build artifacts and Docker volumes...${NC}"
        ./gradlew clean
        docker compose down -v
        echo -e "${GREEN}Done!${NC}"
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
