#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_DIR"

# Load .env file if exists
if [ -f .env ]; then
    set -a
    source .env
    set +a
fi

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
    echo "  start           Start PostgreSQL and run the app (default)"
    echo "  db              Start only PostgreSQL"
    echo "  app             Run the app (assumes DB is running)"
    echo "  mini-app        Start Mini App dev server (Vite)"
    echo "  build-mini-app  Build Mini App for production"
    echo "  all             Start everything (db + app + mini-app)"
    echo "  test            Run tests"
    echo "  build           Build the project"
    echo "  docker          Build and run with Docker Compose (includes mini-app)"
    echo "  stop            Stop all Docker containers"
    echo "  logs            Show Docker logs"
    echo "  clean           Clean build artifacts and Docker volumes"
    echo "  help            Show this help message"
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

run_mini_app() {
    if [ ! -d "$PROJECT_DIR/mini-app" ]; then
        echo -e "${RED}Error: mini-app directory not found${NC}"
        exit 1
    fi

    echo -e "${GREEN}Starting Mini App dev server...${NC}"
    cd "$PROJECT_DIR/mini-app"

    if [ ! -d "node_modules" ]; then
        echo -e "${YELLOW}Installing dependencies...${NC}"
        npm install
    fi

    npm run dev
}

build_mini_app() {
    if [ ! -d "$PROJECT_DIR/mini-app" ]; then
        echo -e "${RED}Error: mini-app directory not found${NC}"
        exit 1
    fi

    echo -e "${GREEN}Building Mini App for production...${NC}"
    cd "$PROJECT_DIR/mini-app"

    if [ ! -d "node_modules" ]; then
        echo -e "${YELLOW}Installing dependencies...${NC}"
        npm install
    fi

    npm run build
    echo -e "${GREEN}Mini App built successfully! Output: mini-app/dist${NC}"
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
    mini-app)
        run_mini_app
        ;;
    build-mini-app)
        build_mini_app
        ;;
    all)
        echo -e "${GREEN}Starting full development stack...${NC}"
        start_db

        # Start backend in background
        echo -e "${GREEN}Starting backend in background...${NC}"
        ./gradlew bootRun > /tmp/chatkeep-backend.log 2>&1 &
        BACKEND_PID=$!
        echo -e "${YELLOW}Backend PID: $BACKEND_PID (logs: /tmp/chatkeep-backend.log)${NC}"

        # Give backend time to start
        sleep 5

        # Start frontend in foreground
        echo -e "${GREEN}Starting Mini App dev server...${NC}"
        run_mini_app

        # Cleanup on exit
        trap "echo -e '${YELLOW}Stopping backend...${NC}'; kill $BACKEND_PID 2>/dev/null" EXIT
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

        # Build Mini App first
        echo -e "${GREEN}Building Mini App...${NC}"
        build_mini_app

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
