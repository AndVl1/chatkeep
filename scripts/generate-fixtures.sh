#!/usr/bin/env bash
set -euo pipefail

# Generate JSON fixtures from OpenAPI schema for contract testing
# Usage: ./scripts/generate-fixtures.sh [openapi.json] [output-dir]

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

OPENAPI_JSON="${1:-$PROJECT_ROOT/build/openapi/openapi.json}"
OUTPUT_DIR="${2:-$PROJECT_ROOT/build/fixtures}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if OpenAPI JSON exists
if [ ! -f "$OPENAPI_JSON" ]; then
    log_error "OpenAPI JSON not found at: $OPENAPI_JSON"
    log_info "Run './gradlew generateOpenApiDocs' first (requires app to be running)"
    exit 1
fi

# Create output directory
mkdir -p "$OUTPUT_DIR"
log_info "Output directory: $OUTPUT_DIR"

# Check if jq is installed
if ! command -v jq &> /dev/null; then
    log_error "jq is required but not installed. Install with: brew install jq"
    exit 1
fi

log_info "Generating fixtures from: $OPENAPI_JSON"

# Generate fixtures for key response types

# 1. Login Response (TokenResponse)
cat > "$OUTPUT_DIR/login_response.json" << 'EOF'
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkiLCJmaXJzdE5hbWUiOiJKb2huIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
  "expiresIn": 86400,
  "user": {
    "id": 123456789,
    "firstName": "John",
    "lastName": "Doe",
    "username": "johndoe",
    "photoUrl": "https://t.me/i/userpic/320/johndoe.jpg"
  }
}
EOF
log_info "Generated: login_response.json"

# 2. Dashboard Response
cat > "$OUTPUT_DIR/dashboard_response.json" << 'EOF'
{
  "serviceStatus": {
    "running": true,
    "uptime": 86400
  },
  "deployInfo": {
    "commitSha": "a1b2c3d4",
    "deployedAt": "2026-01-17T12:00:00Z",
    "imageVersion": "v1.2.3"
  },
  "quickStats": {
    "totalChats": 42,
    "messagesToday": 150,
    "messagesYesterday": 120
  }
}
EOF
log_info "Generated: dashboard_response.json"

# 3. Chats Response (Array of ChatSummaryResponse)
cat > "$OUTPUT_DIR/chats_response.json" << 'EOF'
[
  {
    "chatId": -1001234567890,
    "chatTitle": "Tech Community",
    "memberCount": 42,
    "isBotAdmin": true
  },
  {
    "chatId": -1009876543210,
    "chatTitle": "Dev Team",
    "memberCount": 12,
    "isBotAdmin": true
  },
  {
    "chatId": -1001111222333,
    "chatTitle": "General Chat",
    "memberCount": 100,
    "isBotAdmin": false
  }
]
EOF
log_info "Generated: chats_response.json"

# 4. Logs Response
cat > "$OUTPUT_DIR/logs_response.json" << 'EOF'
{
  "entries": [
    {
      "timestamp": "2026-01-17T12:00:00Z",
      "level": "INFO",
      "logger": "ru.andvl.chatkeep.bot.ChatkeepBot",
      "message": "Bot started successfully"
    },
    {
      "timestamp": "2026-01-17T12:01:00Z",
      "level": "INFO",
      "logger": "ru.andvl.chatkeep.service.MessageService",
      "message": "Processing message from chat -1001234567890"
    },
    {
      "timestamp": "2026-01-17T12:02:00Z",
      "level": "WARN",
      "logger": "ru.andvl.chatkeep.bot.handler.BlocklistHandler",
      "message": "Detected blocked pattern in message"
    },
    {
      "timestamp": "2026-01-17T12:03:00Z",
      "level": "ERROR",
      "logger": "ru.andvl.chatkeep.api.controller.AdminController",
      "message": "Failed to fetch workflow status: Connection timeout"
    }
  ],
  "totalCount": 500,
  "fromTime": "2026-01-17T00:00:00Z",
  "toTime": "2026-01-17T23:59:59Z"
}
EOF
log_info "Generated: logs_response.json"

# 5. Workflows Response (Array of WorkflowResponse)
cat > "$OUTPUT_DIR/workflows_response.json" << 'EOF'
[
  {
    "id": "deploy.yml",
    "name": "Deploy to Production",
    "filename": "deploy.yml",
    "lastRun": {
      "id": 123456789,
      "status": "completed",
      "conclusion": "success",
      "createdAt": "2026-01-17T12:00:00Z",
      "updatedAt": "2026-01-17T12:05:00Z",
      "triggeredBy": "github-actions",
      "url": "https://github.com/owner/repo/actions/runs/123456789"
    }
  },
  {
    "id": "test.yml",
    "name": "Run Tests",
    "filename": "test.yml",
    "lastRun": {
      "id": 987654321,
      "status": "completed",
      "conclusion": "failure",
      "createdAt": "2026-01-17T11:00:00Z",
      "updatedAt": "2026-01-17T11:10:00Z",
      "triggeredBy": "dependabot",
      "url": "https://github.com/owner/repo/actions/runs/987654321"
    }
  },
  {
    "id": "backup.yml",
    "name": "Database Backup",
    "filename": "backup.yml",
    "lastRun": null
  }
]
EOF
log_info "Generated: workflows_response.json"

# 6. Trigger Response
cat > "$OUTPUT_DIR/trigger_response.json" << 'EOF'
{
  "success": true,
  "message": "Workflow triggered successfully",
  "workflowId": "deploy.yml"
}
EOF
log_info "Generated: trigger_response.json"

# 7. Action Response
cat > "$OUTPUT_DIR/action_response.json" << 'EOF'
{
  "success": true,
  "message": "Action completed successfully"
}
EOF
log_info "Generated: action_response.json"

# 8. User Response (TelegramUserResponse)
cat > "$OUTPUT_DIR/user_response.json" << 'EOF'
{
  "id": 123456789,
  "firstName": "John",
  "lastName": "Doe",
  "username": "johndoe",
  "photoUrl": "https://t.me/i/userpic/320/johndoe.jpg"
}
EOF
log_info "Generated: user_response.json"

# 9. Error Response
cat > "$OUTPUT_DIR/error_response.json" << 'EOF'
{
  "code": "NOT_FOUND",
  "message": "Resource not found",
  "details": {
    "resource": "chat",
    "id": "-1001234567890"
  }
}
EOF
log_info "Generated: error_response.json"

# 10. Settings Response
cat > "$OUTPUT_DIR/settings_response.json" << 'EOF'
{
  "chatId": -1001234567890,
  "chatTitle": "Tech Community",
  "collectionEnabled": true,
  "cleanServiceEnabled": false,
  "maxWarnings": 3,
  "warningTtlHours": 24,
  "thresholdAction": "mute",
  "thresholdDurationMinutes": 60,
  "defaultBlocklistAction": "delete",
  "logChannelId": -1009876543210,
  "lockWarnsEnabled": true,
  "locale": "en"
}
EOF
log_info "Generated: settings_response.json"

# Generate index file listing all fixtures
cat > "$OUTPUT_DIR/index.json" << EOF
{
  "generated_at": "$(date -u +"%Y-%m-%dT%H:%M:%SZ")",
  "source": "$OPENAPI_JSON",
  "fixtures": [
    "login_response.json",
    "dashboard_response.json",
    "chats_response.json",
    "logs_response.json",
    "workflows_response.json",
    "trigger_response.json",
    "action_response.json",
    "user_response.json",
    "error_response.json",
    "settings_response.json"
  ]
}
EOF

log_info "Generated index.json with fixture manifest"
log_info ""
log_info "✅ Fixture generation complete!"
log_info "Total fixtures: 10"
log_info "Location: $OUTPUT_DIR"
log_info ""
log_info "Use these fixtures for contract testing in your frontend/mobile apps."
