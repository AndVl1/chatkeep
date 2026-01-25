#!/bin/bash
#
# Twitch Stream Title Updater
# Updates stream title every minute
#
# Requirements:
# - TWITCH_CLIENT_ID: Your Twitch application client ID
# - TWITCH_USER_TOKEN: User access token with 'channel:manage:broadcast' scope
# - TWITCH_BROADCASTER_ID: Your Twitch user ID (numeric)
#
# How to get User Access Token:
# 1. Go to: https://id.twitch.tv/oauth2/authorize?client_id=YOUR_CLIENT_ID&redirect_uri=http://localhost&response_type=token&scope=channel:manage:broadcast
# 2. After authorization, copy the access_token from the URL
#
# How to get Broadcaster ID:
# curl -H "Authorization: Bearer $TWITCH_USER_TOKEN" -H "Client-Id: $TWITCH_CLIENT_ID" https://api.twitch.tv/helix/users
#

set -e

# Configuration
CLIENT_ID="g00zqlyciiu2kpyqazs4dbtgsn4x5j"
USER_TOKEN="fqkxc7nh8p65jb1g6c7emisnnvxu5o"
BROADCASTER_ID="986990068"
INTERVAL_SECONDS="${UPDATE_INTERVAL:-20}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${BLUE}[$(date '+%Y-%m-%d %H:%M:%S')]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[$(date '+%Y-%m-%d %H:%M:%S')]${NC} $1"
}

log_error() {
    echo -e "${RED}[$(date '+%Y-%m-%d %H:%M:%S')]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[$(date '+%Y-%m-%d %H:%M:%S')]${NC} $1"
}

# Validate required environment variables
validate_config() {
    local missing=0

    if [[ -z "$CLIENT_ID" ]]; then
        log_error "TWITCH_CLIENT_ID is not set"
        missing=1
    fi

    if [[ -z "$USER_TOKEN" ]]; then
        log_error "TWITCH_USER_TOKEN is not set"
        echo ""
        echo "To get a user token:"
        echo "1. Replace YOUR_CLIENT_ID in the URL below with your client ID"
        echo "2. Open in browser: https://id.twitch.tv/oauth2/authorize?client_id=YOUR_CLIENT_ID&redirect_uri=http://localhost&response_type=token&scope=channel:manage:broadcast"
        echo "3. Authorize and copy access_token from the redirect URL"
        missing=1
    fi

    if [[ -z "$BROADCASTER_ID" ]]; then
        log_error "TWITCH_BROADCASTER_ID is not set"
        echo ""
        echo "To get your broadcaster ID, run:"
        echo "curl -s -H 'Authorization: Bearer \$TWITCH_USER_TOKEN' -H 'Client-Id: \$TWITCH_CLIENT_ID' https://api.twitch.tv/helix/users | jq '.data[0].id'"
        missing=1
    fi

    if [[ $missing -eq 1 ]]; then
        exit 1
    fi
}

# Generate a title based on current time or custom logic
generate_title() {
    local timestamp=$(date '+%H:%M:%S')
    local date_str=$(date '+%d.%m.%Y')

    # You can customize this function to generate different titles
    # Examples:
    # - Static with timestamp: "Live Stream | $timestamp"
    # - Random from array
    # - Read from file
    # - API call to external service

    # Default: timestamp-based title
    echo "🔴 Some Testing Live Stream | Updated: $timestamp | $date_str"
}

# Update stream title via Twitch API
update_title() {
    local new_title="$1"

    local response=$(curl -s -w "\n%{http_code}" -X PATCH \
        "https://api.twitch.tv/helix/channels?broadcaster_id=$BROADCASTER_ID" \
        -H "Authorization: Bearer $USER_TOKEN" \
        -H "Client-Id: $CLIENT_ID" \
        -H "Content-Type: application/json" \
        -d "{\"title\": \"$new_title\"}")

    local http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | sed '$d')

    if [[ "$http_code" == "204" ]]; then
        log_success "Title updated: $new_title"
        return 0
    elif [[ "$http_code" == "401" ]]; then
        log_error "Authentication failed. Token may be expired or invalid."
        log_error "Response: $body"
        return 1
    elif [[ "$http_code" == "403" ]]; then
        log_error "Permission denied. Make sure token has 'channel:manage:broadcast' scope."
        log_error "Response: $body"
        return 1
    else
        log_error "Failed to update title (HTTP $http_code)"
        log_error "Response: $body"
        return 1
    fi
}

# Get current stream info
get_current_title() {
    local response=$(curl -s \
        "https://api.twitch.tv/helix/channels?broadcaster_id=$BROADCASTER_ID" \
        -H "Authorization: Bearer $USER_TOKEN" \
        -H "Client-Id: $CLIENT_ID")

    echo "$response" | jq -r '.data[0].title // "Unknown"'
}

# Main loop
main() {
    echo "=========================================="
    echo "  Twitch Stream Title Updater"
    echo "=========================================="
    echo ""

    validate_config

    log_info "Configuration:"
    log_info "  Client ID: ${CLIENT_ID:0:8}..."
    log_info "  Broadcaster ID: $BROADCASTER_ID"
    log_info "  Update interval: ${INTERVAL_SECONDS}s"
    echo ""

    # Get and show current title
    local current_title=$(get_current_title)
    log_info "Current title: $current_title"
    echo ""

    log_info "Starting title update loop (Ctrl+C to stop)..."
    echo ""

    local update_count=0

    # Handle graceful shutdown
    trap 'echo ""; log_warn "Stopping..."; exit 0' SIGINT SIGTERM

    while true; do
        update_count=$((update_count + 1))

        local new_title=$(generate_title)

        log_info "Update #$update_count - Setting title..."

        if update_title "$new_title"; then
            log_info "Next update in ${INTERVAL_SECONDS}s"
        else
            log_warn "Will retry in ${INTERVAL_SECONDS}s"
        fi

        echo ""
        sleep "$INTERVAL_SECONDS"
    done
}

# Run
main "$@"
