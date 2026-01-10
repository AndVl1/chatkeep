#!/bin/bash
# Legacy wrapper for mini-app-helper.sh - forwards to chatkeep.sh
# This file is kept for backward compatibility

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Map mini-app-helper commands to chatkeep.sh dev commands
case "${1:-help}" in
    install)
        echo "Use: cd mini-app && npm install"
        exit 0
        ;;
    dev|mini-app|frontend)
        exec "$SCRIPT_DIR/chatkeep.sh" dev mini-app
        ;;
    build|build-mini-app)
        exec "$SCRIPT_DIR/chatkeep.sh" dev build-mini-app
        ;;
    preview|lint|clean)
        # These commands are not migrated yet - show deprecation message
        echo "This command is deprecated. Use 'cd mini-app && npm run $1' instead"
        exit 1
        ;;
    tunnel)
        exec "$SCRIPT_DIR/chatkeep.sh" dev tunnel
        ;;
    ngrok)
        echo "ngrok tunnel support - run: ngrok http 5173"
        exit 0
        ;;
    all|full)
        exec "$SCRIPT_DIR/chatkeep.sh" dev all
        ;;
    status)
        exec "$SCRIPT_DIR/chatkeep.sh" dev status
        ;;
    help|--help|-h)
        echo "This script is deprecated. Use ./scripts/chatkeep.sh dev instead"
        echo ""
        exec "$SCRIPT_DIR/chatkeep.sh" dev help
        ;;
    *)
        echo "Unknown command: $1"
        echo "This script is deprecated. Use ./scripts/chatkeep.sh dev instead"
        exec "$SCRIPT_DIR/chatkeep.sh" dev help
        ;;
esac
