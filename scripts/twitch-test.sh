#!/bin/bash
# Twitch Local Testing Script
# Uses Twitch CLI to test EventSub webhook handling locally

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Default configuration
WEBHOOK_URL="${TWITCH_WEBHOOK_URL:-http://localhost:8080/webhooks/twitch}"
WEBHOOK_SECRET="${TWITCH_WEBHOOK_SECRET:-test_webhook_secret_12345}"
BROADCASTER_ID="${TWITCH_BROADCASTER_ID:-123456789}"
MOCK_API_PORT="${TWITCH_MOCK_API_PORT:-8085}"

print_header() {
    echo ""
    echo -e "${BLUE}════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}  $1${NC}"
    echo -e "${BLUE}════════════════════════════════════════════════════${NC}"
    echo ""
}

check_twitch_cli() {
    if ! command -v twitch &> /dev/null; then
        echo -e "${RED}Error: Twitch CLI not installed${NC}"
        echo "Install with: brew install twitchdev/twitch/twitch-cli"
        exit 1
    fi
    echo -e "${GREEN}Twitch CLI: $(twitch version)${NC}"
}

check_backend() {
    if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo -e "${GREEN}Backend is running on http://localhost:8080${NC}"
        return 0
    else
        echo -e "${RED}Backend is not running!${NC}"
        echo "Start with: ./scripts/chatkeep.sh dev start"
        return 1
    fi
}

configure() {
    print_header "Configuring Twitch CLI for Local Testing"

    echo -e "${YELLOW}Configuration:${NC}"
    echo "  Webhook URL: $WEBHOOK_URL"
    echo "  Webhook Secret: $WEBHOOK_SECRET"
    echo ""

    # Configure default forwarding address
    echo -e "${GREEN}Setting up event forwarding...${NC}"
    twitch event configure -F "$WEBHOOK_URL" -s "$WEBHOOK_SECRET"

    echo -e "${GREEN}Configuration complete!${NC}"
}

verify() {
    print_header "Verifying Webhook Handler"

    check_backend || exit 1

    echo -e "${GREEN}Sending verification request...${NC}"
    twitch event verify-subscription stream.online \
        -F "$WEBHOOK_URL" \
        -s "$WEBHOOK_SECRET"

    echo ""
    echo -e "${GREEN}Verification complete!${NC}"
}

trigger_online() {
    print_header "Triggering stream.online Event"

    local broadcaster="${1:-$BROADCASTER_ID}"

    echo -e "${YELLOW}Sending stream.online event for broadcaster: $broadcaster${NC}"

    twitch event trigger stream.online \
        -F "$WEBHOOK_URL" \
        -s "$WEBHOOK_SECRET" \
        -t "$broadcaster"

    echo -e "${GREEN}Event sent!${NC}"
}

trigger_offline() {
    print_header "Triggering stream.offline Event"

    local broadcaster="${1:-$BROADCASTER_ID}"

    echo -e "${YELLOW}Sending stream.offline event for broadcaster: $broadcaster${NC}"

    twitch event trigger stream.offline \
        -F "$WEBHOOK_URL" \
        -s "$WEBHOOK_SECRET" \
        -t "$broadcaster"

    echo -e "${GREEN}Event sent!${NC}"
}

test_flow() {
    print_header "Testing Full Flow (online -> wait -> offline)"

    check_backend || exit 1

    local broadcaster="${1:-$BROADCASTER_ID}"
    local wait_time="${2:-5}"

    echo -e "${YELLOW}Testing stream lifecycle for broadcaster: $broadcaster${NC}"
    echo ""

    echo -e "${GREEN}Step 1: Triggering stream.online...${NC}"
    trigger_online "$broadcaster"

    echo ""
    echo -e "${YELLOW}Waiting $wait_time seconds...${NC}"
    sleep "$wait_time"

    echo ""
    echo -e "${GREEN}Step 2: Triggering stream.offline...${NC}"
    trigger_offline "$broadcaster"

    echo ""
    echo -e "${GREEN}Full flow test complete!${NC}"
    echo -e "${YELLOW}Check backend logs for event processing${NC}"
}

mock_api_start() {
    print_header "Starting Twitch Mock API Server"

    echo -e "${GREEN}Starting mock API on port $MOCK_API_PORT...${NC}"
    twitch mock-api start -p "$MOCK_API_PORT"
}

mock_api_generate() {
    print_header "Generating Mock Data"

    echo -e "${GREEN}Generating mock users and streams...${NC}"
    twitch mock-api generate -c "${1:-10}"

    echo -e "${GREEN}Mock data generated!${NC}"
}

status() {
    print_header "Twitch Testing Status"

    check_twitch_cli
    echo ""

    # Check backend
    if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo -e "Backend:     ${GREEN}✓ running${NC}"
    else
        echo -e "Backend:     ${RED}✗ not running${NC}"
    fi

    # Check mock API
    if curl -s "http://localhost:$MOCK_API_PORT/units/users" > /dev/null 2>&1; then
        echo -e "Mock API:    ${GREEN}✓ running (port $MOCK_API_PORT)${NC}"
    else
        echo -e "Mock API:    ${YELLOW}✗ not running${NC}"
    fi

    echo ""
    echo -e "${YELLOW}Configuration:${NC}"
    echo "  WEBHOOK_URL: $WEBHOOK_URL"
    echo "  WEBHOOK_SECRET: ${WEBHOOK_SECRET:0:10}..."
    echo "  BROADCASTER_ID: $BROADCASTER_ID"
    echo ""
}

print_help() {
    cat << EOF
Twitch Local Testing Script

Usage: ./scripts/twitch-test.sh <command> [options]

Commands:
  configure         Configure Twitch CLI for local testing
  verify            Verify webhook handler responds correctly
  online [id]       Trigger stream.online event
  offline [id]      Trigger stream.offline event
  test [id] [wait]  Test full flow (online -> wait -> offline)
  mock-start        Start mock API server
  mock-generate [n] Generate mock data (default: 10 users)
  status            Show testing environment status
  help              Show this help

Environment Variables:
  TWITCH_WEBHOOK_URL     Webhook URL (default: http://localhost:8080/webhooks/twitch)
  TWITCH_WEBHOOK_SECRET  Webhook secret (default: test_webhook_secret_12345)
  TWITCH_BROADCASTER_ID  Default broadcaster ID (default: 123456789)
  TWITCH_MOCK_API_PORT   Mock API port (default: 8085)

Examples:
  ./scripts/twitch-test.sh configure           # Setup CLI
  ./scripts/twitch-test.sh verify              # Test webhook verification
  ./scripts/twitch-test.sh online 12345        # Send online event
  ./scripts/twitch-test.sh test 12345 10       # Full test with 10s wait
  ./scripts/twitch-test.sh status              # Check status

Quick Start:
  1. Start backend: ./scripts/chatkeep.sh dev start
  2. Configure CLI: ./scripts/twitch-test.sh configure
  3. Verify webhook: ./scripts/twitch-test.sh verify
  4. Test flow:     ./scripts/twitch-test.sh test

EOF
}

# Main command router
COMMAND="${1:-help}"

case "$COMMAND" in
    configure|config)
        configure
        ;;
    verify|check)
        verify
        ;;
    online|stream-online)
        shift
        trigger_online "$@"
        ;;
    offline|stream-offline)
        shift
        trigger_offline "$@"
        ;;
    test|flow)
        shift
        test_flow "$@"
        ;;
    mock-start|mock)
        mock_api_start
        ;;
    mock-generate|generate)
        shift
        mock_api_generate "$@"
        ;;
    status)
        status
        ;;
    help|--help|-h)
        print_help
        ;;
    *)
        echo -e "${RED}Unknown command: $COMMAND${NC}"
        print_help
        exit 1
        ;;
esac
