#!/bin/bash
# Helper script for Mini App operations

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
MINI_APP_DIR="$PROJECT_DIR/mini-app"

# Load .env file if exists
load_env() {
    if [ -f "$PROJECT_DIR/.env" ]; then
        echo -e "${GREEN}Loading environment from .env...${NC}"
        set -a
        source "$PROJECT_DIR/.env"
        set +a
    fi
}

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

print_help() {
    echo "Mini App Helper Script"
    echo ""
    echo "Usage: ./scripts/mini-app-helper.sh [command]"
    echo ""
    echo "Commands:"
    echo "  install      Install Mini App dependencies"
    echo "  dev          Start dev server (localhost:5173)"
    echo "  build        Build for production"
    echo "  preview      Preview production build"
    echo "  lint         Run linter"
    echo "  clean        Clean build artifacts and node_modules"
    echo "  tunnel       Start cloudflared tunnel (requires cloudflared)"
    echo "  ngrok        Start ngrok tunnel (requires ngrok)"
    echo "  all          Start backend + Mini App dev server (full stack)"
    echo "  status       Show Mini App status"
    echo "  help         Show this help"
    echo ""
    echo "Quick start for local testing:"
    echo "  ./scripts/mini-app-helper.sh all    # Start everything"
    echo ""
    echo "For Telegram testing, see: docs/TESTING.md"
}

check_mini_app() {
    if [ ! -d "$MINI_APP_DIR" ]; then
        echo -e "${RED}Error: mini-app directory not found at $MINI_APP_DIR${NC}"
        exit 1
    fi
}

case "${1:-help}" in
    install)
        check_mini_app
        echo -e "${GREEN}Installing Mini App dependencies...${NC}"
        cd "$MINI_APP_DIR"
        npm install
        echo -e "${GREEN}Dependencies installed!${NC}"
        ;;

    dev)
        check_mini_app
        # Kill existing instances
        echo -e "${YELLOW}Killing existing Mini App instances...${NC}"
        lsof -ti:5173 | xargs kill -9 2>/dev/null || true
        pkill -9 -f "vite.*mini-app" 2>/dev/null || true
        echo -e "${GREEN}Starting Mini App dev server...${NC}"
        cd "$MINI_APP_DIR"
        npm run dev
        ;;

    build)
        check_mini_app
        echo -e "${GREEN}Building Mini App for production...${NC}"
        cd "$MINI_APP_DIR"
        npm run build
        echo -e "${GREEN}Build complete! Output: $MINI_APP_DIR/dist${NC}"
        ;;

    preview)
        check_mini_app
        echo -e "${GREEN}Previewing production build...${NC}"
        cd "$MINI_APP_DIR"
        if [ ! -d "dist" ]; then
            echo -e "${YELLOW}No build found. Building first...${NC}"
            npm run build
        fi
        npm run preview
        ;;

    lint)
        check_mini_app
        echo -e "${GREEN}Running linter...${NC}"
        cd "$MINI_APP_DIR"
        npm run lint
        ;;

    clean)
        check_mini_app
        echo -e "${YELLOW}Cleaning Mini App build artifacts...${NC}"
        cd "$MINI_APP_DIR"
        rm -rf dist node_modules
        echo -e "${GREEN}Clean complete!${NC}"
        ;;

    tunnel)
        if ! command -v cloudflared &> /dev/null; then
            echo -e "${RED}Error: cloudflared not installed${NC}"
            echo "Install with: brew install cloudflared"
            exit 1
        fi

        echo -e "${GREEN}Starting cloudflared tunnel...${NC}"
        echo -e "${YELLOW}Make sure Mini App dev server is running on http://localhost:5173${NC}"
        echo ""
        echo -e "${YELLOW}Copy the URL and set it in @BotFather -> /setmenubutton${NC}"
        echo ""
        cloudflared tunnel --url http://localhost:5173
        ;;

    ngrok)
        if ! command -v ngrok &> /dev/null; then
            echo -e "${RED}Error: ngrok not installed${NC}"
            echo "Install with: brew install ngrok"
            echo "Or download from: https://ngrok.com/download"
            exit 1
        fi

        echo -e "${GREEN}Starting ngrok tunnel...${NC}"
        echo -e "${YELLOW}Make sure Mini App dev server is running on http://localhost:5173${NC}"
        echo ""
        echo -e "${YELLOW}Copy the URL and set it in @BotFather -> /setmenubutton${NC}"
        echo ""
        ngrok http 5173
        ;;

    all)
        check_mini_app
        echo -e "${GREEN}Starting full development stack...${NC}"
        echo ""
        echo -e "${YELLOW}This will start:${NC}"
        echo "  1. PostgreSQL (docker-compose)"
        echo "  2. Backend (./gradlew bootRun)"
        echo "  3. Mini App dev server (npm run dev)"
        echo ""

        # Cleanup function
        cleanup() {
            echo ""
            echo -e "${YELLOW}Shutting down services...${NC}"
            kill $BACKEND_PID 2>/dev/null && wait $BACKEND_PID 2>/dev/null || true
            kill $FRONTEND_PID 2>/dev/null && wait $FRONTEND_PID 2>/dev/null || true
            echo -e "${GREEN}Shutdown complete${NC}"
            exit 0
        }
        trap cleanup INT TERM

        # Check if PostgreSQL is running
        if ! docker ps --filter name=chatkeep-db --format '{{.Names}}' | grep -q chatkeep-db; then
            echo -e "${GREEN}Starting PostgreSQL...${NC}"
            docker compose up -d db
            echo "Waiting for PostgreSQL to be ready..."
            for i in {1..30}; do
                if docker exec chatkeep-db pg_isready -U chatkeep -d chatkeep > /dev/null 2>&1; then
                    echo -e "${GREEN}PostgreSQL is ready!${NC}"
                    break
                fi
                sleep 1
            done
        else
            echo "  PostgreSQL: ✓ already running"
        fi

        # Load environment variables from .env
        load_env

        # Start backend in background
        echo -e "${GREEN}Starting backend...${NC}"
        cd "$PROJECT_DIR"
        ./gradlew bootRun &
        BACKEND_PID=$!

        # Wait for backend to be ready
        echo "Waiting for backend to start..."
        for i in {1..60}; do
            if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
                echo -e "${GREEN}Backend is ready!${NC}"
                break
            fi
            if ! kill -0 $BACKEND_PID 2>/dev/null; then
                echo -e "${RED}Backend failed to start${NC}"
                exit 1
            fi
            sleep 1
        done

        # Kill existing Mini App instances
        echo -e "${YELLOW}Killing existing Mini App instances...${NC}"
        lsof -ti:5173 | xargs kill -9 2>/dev/null || true
        pkill -9 -f "vite.*mini-app" 2>/dev/null || true

        # Start Mini App
        echo -e "${GREEN}Starting Mini App dev server...${NC}"
        cd "$MINI_APP_DIR"
        npm run dev &
        FRONTEND_PID=$!

        # Wait for Mini App to be ready
        echo "Waiting for Mini App to start..."
        for i in {1..30}; do
            if curl -s http://localhost:5173 > /dev/null 2>&1; then
                echo -e "${GREEN}Mini App is ready!${NC}"
                break
            fi
            sleep 1
        done

        echo ""
        echo -e "${GREEN}Full stack started!${NC}"
        echo "  Backend: http://localhost:8080"
        echo "  Mini App: http://localhost:5173"
        echo "  Swagger: http://localhost:8080/swagger-ui.html"
        echo ""
        echo -e "${YELLOW}Press Ctrl+C to stop all services${NC}"

        wait
        ;;

    status)
        check_mini_app
        echo -e "${GREEN}Mini App Status:${NC}"
        echo ""

        # Check if built
        if [ -d "$MINI_APP_DIR/dist" ]; then
            echo "  Build: ✓ exists ($(du -sh "$MINI_APP_DIR/dist" | cut -f1))"
        else
            echo "  Build: ✗ not found"
        fi

        # Check if node_modules installed
        if [ -d "$MINI_APP_DIR/node_modules" ]; then
            echo "  Dependencies: ✓ installed"
        else
            echo "  Dependencies: ✗ not installed (run: ./scripts/mini-app-helper.sh install)"
        fi

        # Check if dev server is running
        if curl -s http://localhost:5173 > /dev/null 2>&1; then
            echo "  Dev Server: ✓ running (http://localhost:5173)"
        else
            echo "  Dev Server: ✗ not running"
        fi

        # Check if nginx container is running
        if docker ps --filter name=chatkeep-mini-app --format '{{.Names}}' | grep -q chatkeep-mini-app; then
            echo "  Production (nginx): ✓ running (http://localhost:3000)"
        else
            echo "  Production (nginx): ✗ not running"
        fi

        echo ""
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
