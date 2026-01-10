#!/bin/bash
# Unified Chatkeep Deployment and Development Script
# Supports both development and production workflows

set -e

# Script initialization
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
MINI_APP_DIR="$PROJECT_DIR/mini-app"

cd "$PROJECT_DIR"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Load .env file if exists
load_env() {
    if [ -f "$PROJECT_DIR/.env" ]; then
        set -a
        source "$PROJECT_DIR/.env"
        set +a
    fi
}

load_env

# Default configuration
REGISTRY="${REGISTRY:-ghcr.io}"
IMAGE_NAME="${IMAGE_NAME:-chatkeep}"
IMAGE_TAG="${IMAGE_TAG:-latest}"
DEPLOY_PATH="${DEPLOY_PATH:-/root/chatkeep}"

# =============================================================================
# Common Functions
# =============================================================================

print_header() {
    echo ""
    echo -e "${BLUE}════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}  $1${NC}"
    echo -e "${BLUE}════════════════════════════════════════════════════${NC}"
    echo ""
}

check_docker_daemon() {
    if ! docker info > /dev/null 2>&1; then
        echo -e "${RED}Error: Docker daemon is not running${NC}"
        exit 1
    fi
}

check_mini_app() {
    if [ ! -d "$MINI_APP_DIR" ]; then
        echo -e "${RED}Error: mini-app directory not found at $MINI_APP_DIR${NC}"
        exit 1
    fi
}

wait_for_health() {
    local service=$1
    local timeout=${2:-60}
    local interval=2

    echo -e "${YELLOW}Waiting for $service to become healthy...${NC}"
    for i in $(seq 1 $((timeout / interval))); do
        if docker compose -f docker-compose.prod.yml ps | grep -q "healthy"; then
            echo -e "${GREEN}$service is healthy!${NC}"
            return 0
        fi
        echo -n "."
        sleep $interval
    done

    echo -e "${RED}Timeout waiting for $service to become healthy${NC}"
    return 1
}

wait_for_postgres() {
    echo -e "${YELLOW}Waiting for PostgreSQL to be ready...${NC}"
    for i in {1..30}; do
        if docker exec chatkeep-db pg_isready -U chatkeep -d chatkeep > /dev/null 2>&1; then
            echo -e "${GREEN}PostgreSQL is ready!${NC}"
            return 0
        fi
        sleep 1
    done
    echo -e "${RED}Timeout waiting for PostgreSQL${NC}"
    return 1
}

wait_for_backend() {
    local port=${1:-8080}
    echo -e "${YELLOW}Waiting for backend to start...${NC}"
    for i in {1..60}; do
        if curl -s http://localhost:$port/actuator/health > /dev/null 2>&1; then
            echo -e "${GREEN}Backend is ready!${NC}"
            return 0
        fi
        sleep 1
    done
    echo -e "${RED}Timeout waiting for backend${NC}"
    return 1
}

# =============================================================================
# Development Mode Functions
# =============================================================================

dev_start() {
    print_header "Starting Development Environment"
    check_docker_daemon

    echo -e "${GREEN}Starting PostgreSQL...${NC}"
    docker compose up -d db
    wait_for_postgres

    echo -e "${GREEN}Starting backend...${NC}"
    ./gradlew bootRun
}

dev_db() {
    print_header "Starting PostgreSQL"
    check_docker_daemon

    echo -e "${GREEN}Starting PostgreSQL...${NC}"
    docker compose up -d db
    wait_for_postgres

    echo -e "${GREEN}PostgreSQL is running on localhost:5432${NC}"
    echo -e "${YELLOW}Connection: postgresql://chatkeep:chatkeep@localhost:5432/chatkeep${NC}"
}

dev_app() {
    print_header "Starting Backend Application"

    echo -e "${GREEN}Starting backend...${NC}"
    ./gradlew bootRun
}

dev_backend() {
    dev_app
}

dev_mini_app() {
    print_header "Starting Mini App Dev Server"
    check_mini_app

    # Kill existing instances
    echo -e "${YELLOW}Killing existing Mini App instances...${NC}"
    lsof -ti:5173 | xargs kill -9 2>/dev/null || true
    pkill -9 -f "vite.*mini-app" 2>/dev/null || true

    cd "$MINI_APP_DIR"

    if [ ! -d "node_modules" ]; then
        echo -e "${YELLOW}Installing dependencies...${NC}"
        npm install
    fi

    echo -e "${GREEN}Starting Mini App dev server...${NC}"
    npm run dev
}

dev_frontend() {
    dev_mini_app
}

dev_all() {
    print_header "Starting Full Development Stack"
    check_docker_daemon
    check_mini_app

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

    # Start PostgreSQL
    if ! docker ps --filter name=chatkeep-db --format '{{.Names}}' | grep -q chatkeep-db; then
        echo -e "${GREEN}Starting PostgreSQL...${NC}"
        docker compose up -d db
        wait_for_postgres
    else
        echo -e "${GREEN}PostgreSQL: already running${NC}"
    fi

    # Start backend in background
    echo -e "${GREEN}Starting backend in background...${NC}"
    ./gradlew bootRun > /tmp/chatkeep-backend.log 2>&1 &
    BACKEND_PID=$!
    echo -e "${YELLOW}Backend PID: $BACKEND_PID (logs: /tmp/chatkeep-backend.log)${NC}"

    # Wait for backend
    if ! wait_for_backend; then
        if ! kill -0 $BACKEND_PID 2>/dev/null; then
            echo -e "${RED}Backend failed to start. Check logs: /tmp/chatkeep-backend.log${NC}"
            exit 1
        fi
    fi

    # Kill existing Mini App instances
    echo -e "${YELLOW}Killing existing Mini App instances...${NC}"
    lsof -ti:5173 | xargs kill -9 2>/dev/null || true
    pkill -9 -f "vite.*mini-app" 2>/dev/null || true

    # Start Mini App
    echo -e "${GREEN}Starting Mini App dev server...${NC}"
    cd "$MINI_APP_DIR"
    npm run dev &
    FRONTEND_PID=$!

    # Wait for Mini App
    sleep 3

    echo ""
    echo -e "${GREEN}Full stack started!${NC}"
    echo "  Backend:  http://localhost:8080"
    echo "  Mini App: http://localhost:5173"
    echo "  Swagger:  http://localhost:8080/swagger-ui.html"
    echo ""
    echo -e "${YELLOW}Press Ctrl+C to stop all services${NC}"

    wait
}

dev_full() {
    dev_all
}

dev_build() {
    print_header "Building Backend"

    echo -e "${GREEN}Building project...${NC}"
    ./gradlew build
    echo -e "${GREEN}Build complete!${NC}"
}

dev_build_mini_app() {
    print_header "Building Mini App"
    check_mini_app

    cd "$MINI_APP_DIR"

    if [ ! -d "node_modules" ]; then
        echo -e "${YELLOW}Installing dependencies...${NC}"
        npm install
    fi

    echo -e "${GREEN}Building Mini App for production...${NC}"
    npm run build
    echo -e "${GREEN}Mini App built successfully! Output: mini-app/dist${NC}"
}

dev_docker() {
    print_header "Starting with Docker Compose"
    check_docker_daemon

    if [ -z "$TELEGRAM_BOT_TOKEN" ]; then
        echo -e "${YELLOW}Warning: TELEGRAM_BOT_TOKEN not set. Bot will not work.${NC}"
        echo -e "${YELLOW}Set it with: export TELEGRAM_BOT_TOKEN=your_token${NC}"
    fi

    # Build Mini App first
    echo -e "${GREEN}Building Mini App...${NC}"
    dev_build_mini_app

    echo -e "${GREEN}Building and starting with Docker Compose...${NC}"
    docker compose up --build
}

dev_test() {
    print_header "Running Tests"

    echo -e "${GREEN}Running tests...${NC}"
    ./gradlew test
}

dev_stop() {
    print_header "Stopping Development Services"
    check_docker_daemon

    echo -e "${GREEN}Stopping all containers...${NC}"
    docker compose down

    # Kill any running processes
    lsof -ti:8080 | xargs kill -9 2>/dev/null || true
    lsof -ti:5173 | xargs kill -9 2>/dev/null || true

    echo -e "${GREEN}All services stopped!${NC}"
}

dev_logs() {
    print_header "Development Logs"
    check_docker_daemon

    docker compose logs -f
}

dev_clean() {
    print_header "Cleaning Build Artifacts"
    check_docker_daemon

    echo -e "${YELLOW}Cleaning build artifacts and Docker volumes...${NC}"
    ./gradlew clean

    if [ -d "$MINI_APP_DIR/dist" ]; then
        rm -rf "$MINI_APP_DIR/dist"
    fi

    docker compose down -v
    echo -e "${GREEN}Clean complete!${NC}"
}

dev_tunnel() {
    print_header "Starting Cloudflared Tunnel"

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
}

dev_status() {
    print_header "Development Environment Status"

    echo -e "${GREEN}Service Status:${NC}"
    echo ""

    # PostgreSQL
    if docker ps --filter name=chatkeep-db --format '{{.Names}}' | grep -q chatkeep-db; then
        echo "  PostgreSQL: ${GREEN}✓ running${NC}"
    else
        echo "  PostgreSQL: ${RED}✗ not running${NC}"
    fi

    # Backend
    if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo "  Backend:    ${GREEN}✓ running (http://localhost:8080)${NC}"
    else
        echo "  Backend:    ${RED}✗ not running${NC}"
    fi

    # Mini App dev server
    if curl -s http://localhost:5173 > /dev/null 2>&1; then
        echo "  Mini App:   ${GREEN}✓ running (http://localhost:5173)${NC}"
    else
        echo "  Mini App:   ${RED}✗ not running${NC}"
    fi

    # Mini App build
    if [ -d "$MINI_APP_DIR/dist" ]; then
        echo "  Build:      ${GREEN}✓ exists ($(du -sh "$MINI_APP_DIR/dist" 2>/dev/null | cut -f1))${NC}"
    else
        echo "  Build:      ${YELLOW}✗ not built${NC}"
    fi

    echo ""
}

# =============================================================================
# Production Mode Functions
# =============================================================================

prod_check_env() {
    local missing=0

    if [ -z "$TELEGRAM_BOT_TOKEN" ]; then
        echo -e "${RED}Error: TELEGRAM_BOT_TOKEN is not set${NC}"
        missing=1
    fi

    if [ -z "$DB_PASSWORD" ]; then
        echo -e "${RED}Error: DB_PASSWORD is not set${NC}"
        missing=1
    fi

    if [ -z "$JWT_SECRET" ]; then
        echo -e "${RED}Error: JWT_SECRET is not set${NC}"
        missing=1
    fi

    if [ $missing -eq 1 ]; then
        echo ""
        echo "Set required environment variables or create a .env file"
        exit 1
    fi
}

prod_build() {
    print_header "Building Docker Image"
    check_docker_daemon

    echo -e "${GREEN}Building Docker image: $REGISTRY/$IMAGE_NAME:$IMAGE_TAG${NC}"
    docker build -t "$REGISTRY/$IMAGE_NAME:$IMAGE_TAG" .
    echo -e "${GREEN}Image built successfully!${NC}"
}

prod_push() {
    print_header "Pushing to Registry"
    check_docker_daemon

    prod_build

    echo -e "${GREEN}Pushing to registry: $REGISTRY/$IMAGE_NAME:$IMAGE_TAG${NC}"
    docker push "$REGISTRY/$IMAGE_NAME:$IMAGE_TAG"
    echo -e "${GREEN}Image pushed successfully!${NC}"
}

prod_pull() {
    print_header "Pulling Latest Image"
    check_docker_daemon

    echo -e "${GREEN}Pulling latest image: $REGISTRY/$IMAGE_NAME:$IMAGE_TAG${NC}"
    docker pull "$REGISTRY/$IMAGE_NAME:$IMAGE_TAG"
    echo -e "${GREEN}Image pulled successfully!${NC}"
}

prod_up() {
    print_header "Starting Production Containers"
    check_docker_daemon
    prod_check_env

    echo -e "${GREEN}Starting production containers...${NC}"
    docker compose -f docker-compose.prod.yml up -d
    echo -e "${GREEN}Containers started!${NC}"
}

prod_down() {
    print_header "Stopping Production Containers"
    check_docker_daemon

    echo -e "${GREEN}Stopping production containers...${NC}"
    docker compose -f docker-compose.prod.yml down
    echo -e "${GREEN}Containers stopped!${NC}"
}

prod_restart() {
    print_header "Restarting Production Services"
    check_docker_daemon
    prod_check_env

    echo -e "${GREEN}Pulling latest images...${NC}"
    docker compose -f docker-compose.prod.yml pull

    echo -e "${GREEN}Restarting containers...${NC}"
    docker compose -f docker-compose.prod.yml up -d

    wait_for_health "application" 60

    echo -e "${GREEN}Cleaning up old images...${NC}"
    docker image prune -f

    echo -e "${GREEN}Restart complete!${NC}"
}

prod_status() {
    print_header "Production Status"
    check_docker_daemon

    docker compose -f docker-compose.prod.yml ps
}

prod_logs() {
    print_header "Production Logs"
    check_docker_daemon

    docker compose -f docker-compose.prod.yml logs -f
}

prod_deploy() {
    print_header "Remote Production Deployment"

    # Parse command line arguments
    local host=""
    local user=""
    local password=""
    local key=""

    while [[ $# -gt 0 ]]; do
        case $1 in
            --host|-h)
                host="$2"
                shift 2
                ;;
            --user|-u)
                user="$2"
                shift 2
                ;;
            --password|-p)
                password="$2"
                shift 2
                ;;
            --key|-k)
                key="$2"
                shift 2
                ;;
            *)
                echo -e "${RED}Unknown option: $1${NC}"
                exit 1
                ;;
        esac
    done

    # Validate required parameters
    if [ -z "$host" ]; then
        echo -e "${RED}Error: --host is required${NC}"
        echo "Usage: ./scripts/chatkeep.sh prod deploy --host <host> --user <user> [--password <password> | --key <key>]"
        exit 1
    fi

    if [ -z "$user" ]; then
        echo -e "${RED}Error: --user is required${NC}"
        echo "Usage: ./scripts/chatkeep.sh prod deploy --host <host> --user <user> [--password <password> | --key <key>]"
        exit 1
    fi

    # Construct SSH command
    local ssh_cmd=""
    if [ -n "$password" ]; then
        if ! command -v sshpass &> /dev/null; then
            echo -e "${RED}Error: sshpass not installed (required for password auth)${NC}"
            echo "Install with: brew install sshpass (macOS) or apt-get install sshpass (Ubuntu)"
            exit 1
        fi
        ssh_cmd="sshpass -p '$password' ssh -o StrictHostKeyChecking=no $user@$host"
    elif [ -n "$key" ]; then
        ssh_cmd="ssh -i $key -o StrictHostKeyChecking=no $user@$host"
    else
        ssh_cmd="ssh -o StrictHostKeyChecking=no $user@$host"
    fi

    echo -e "${GREEN}Deploying to: $user@$host${NC}"
    echo -e "${YELLOW}Deployment path: $DEPLOY_PATH${NC}"
    echo ""

    # Remote deployment commands
    local deploy_script="cd $DEPLOY_PATH && \
        echo 'Pulling latest code...' && \
        git pull origin main && \
        echo 'Pulling Docker images...' && \
        docker compose -f docker-compose.prod.yml pull && \
        echo 'Starting containers...' && \
        docker compose -f docker-compose.prod.yml up -d && \
        echo 'Waiting for health checks...' && \
        sleep 15 && \
        echo 'Cleaning up old images...' && \
        docker image prune -f && \
        echo 'Deployment complete!' && \
        docker compose -f docker-compose.prod.yml ps"

    eval "$ssh_cmd '$deploy_script'"

    echo ""
    echo -e "${GREEN}Remote deployment completed successfully!${NC}"
}

# =============================================================================
# Help Functions
# =============================================================================

print_main_help() {
    cat << EOF
Chatkeep Unified Deployment Script

Usage: ./scripts/chatkeep.sh <mode> <command> [options]

Modes:
  dev     Development mode (local development)
  prod    Production mode (deployment)

Examples:
  ./scripts/chatkeep.sh dev start
  ./scripts/chatkeep.sh dev all
  ./scripts/chatkeep.sh prod deploy --host 89.125.243.104 --user root --password XXX
  ./scripts/chatkeep.sh prod restart

For mode-specific help, use:
  ./scripts/chatkeep.sh dev help
  ./scripts/chatkeep.sh prod help

Environment Variables:
  REGISTRY           Container registry (default: ghcr.io)
  IMAGE_NAME         Docker image name (default: chatkeep)
  IMAGE_TAG          Docker image tag (default: latest)
  DEPLOY_PATH        Remote deployment path (default: /root/chatkeep)
  TELEGRAM_BOT_TOKEN Telegram bot token (required for prod)
  DB_PASSWORD        Database password (required for prod)
  JWT_SECRET         JWT secret key (required for prod)

EOF
}

print_dev_help() {
    cat << EOF
Development Mode Commands

Usage: ./scripts/chatkeep.sh dev <command>

Commands:
  start           Start PostgreSQL and backend (default)
  db              Start PostgreSQL only
  app, backend    Start backend only
  mini-app,       Start Mini App dev server (Vite)
    frontend
  all, full       Start full stack (db + backend + frontend)
  build           Build backend JAR
  build-mini-app  Build Mini App for production
  docker          Build and run via Docker Compose
  test            Run backend tests
  stop            Stop all development services
  logs            View Docker container logs
  clean           Clean build artifacts and Docker volumes
  tunnel          Start cloudflared tunnel for Mini App
  status          Show development environment status
  help            Show this help

Examples:
  ./scripts/chatkeep.sh dev start              # Start DB + backend
  ./scripts/chatkeep.sh dev all                # Full stack development
  ./scripts/chatkeep.sh dev mini-app           # Frontend dev server only
  ./scripts/chatkeep.sh dev build-mini-app     # Build frontend for prod
  ./scripts/chatkeep.sh dev tunnel             # Expose Mini App via tunnel

EOF
}

print_prod_help() {
    cat << EOF
Production Mode Commands

Usage: ./scripts/chatkeep.sh prod <command> [options]

Commands:
  build           Build Docker image locally
  push            Build and push image to registry
  pull            Pull latest image from registry
  up              Start production containers
  down            Stop production containers
  restart         Pull latest images and restart
  status          Show production container status
  logs            View production container logs
  deploy          Deploy to remote server via SSH
  help            Show this help

Deploy Options:
  --host, -h      Server hostname or IP (required)
  --user, -u      SSH username (required)
  --password, -p  SSH password (requires sshpass)
  --key, -k       SSH private key path

Examples:
  ./scripts/chatkeep.sh prod build
  ./scripts/chatkeep.sh prod push
  ./scripts/chatkeep.sh prod restart
  ./scripts/chatkeep.sh prod deploy --host 89.125.243.104 --user root --password XXX
  ./scripts/chatkeep.sh prod deploy --host example.com --user deploy --key ~/.ssh/id_rsa
  ./scripts/chatkeep.sh prod logs

Required Environment Variables:
  TELEGRAM_BOT_TOKEN     Telegram bot token
  DB_PASSWORD            Database password
  JWT_SECRET             JWT secret key

Optional Environment Variables:
  REGISTRY               Container registry (default: ghcr.io)
  IMAGE_NAME             Image name (default: chatkeep)
  IMAGE_TAG              Image tag (default: latest)
  DEPLOY_PATH            Remote deployment path (default: /root/chatkeep)

EOF
}

# =============================================================================
# Main Command Router
# =============================================================================

MODE="${1:-help}"
COMMAND="${2:-help}"

case "$MODE" in
    dev)
        shift
        COMMAND="${1:-start}"
        case "$COMMAND" in
            start) dev_start ;;
            db) dev_db ;;
            app|backend) dev_backend ;;
            mini-app|frontend) dev_mini_app ;;
            all|full) dev_all ;;
            build) dev_build ;;
            build-mini-app) dev_build_mini_app ;;
            docker) dev_docker ;;
            test) dev_test ;;
            stop) dev_stop ;;
            logs) dev_logs ;;
            clean) dev_clean ;;
            tunnel) dev_tunnel ;;
            status) dev_status ;;
            help|--help|-h) print_dev_help ;;
            *)
                echo -e "${RED}Unknown dev command: $COMMAND${NC}"
                print_dev_help
                exit 1
                ;;
        esac
        ;;

    prod)
        shift
        COMMAND="${1:-help}"
        case "$COMMAND" in
            build) prod_build ;;
            push) prod_push ;;
            pull) prod_pull ;;
            up) prod_up ;;
            down) prod_down ;;
            restart) prod_restart ;;
            status) prod_status ;;
            logs) prod_logs ;;
            deploy)
                shift
                prod_deploy "$@"
                ;;
            help|--help|-h) print_prod_help ;;
            *)
                echo -e "${RED}Unknown prod command: $COMMAND${NC}"
                print_prod_help
                exit 1
                ;;
        esac
        ;;

    help|--help|-h)
        print_main_help
        ;;

    *)
        echo -e "${RED}Unknown mode: $MODE${NC}"
        print_main_help
        exit 1
        ;;
esac
