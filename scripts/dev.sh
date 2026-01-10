#!/bin/bash
# Legacy wrapper for dev.sh - forwards to chatkeep.sh
# This file is kept for backward compatibility

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Forward all commands to chatkeep.sh dev mode
exec "$SCRIPT_DIR/chatkeep.sh" dev "$@"
