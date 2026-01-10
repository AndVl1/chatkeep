# ChatKeep Admin App - Technical Specification

## Overview

A Kotlin Multiplatform Compose admin application for managing the ChatKeep Telegram bot.

**Domain**: `admin.chatmoderatorbot.ru`
**Platforms**: Android, iOS, Desktop (JVM), Web (WASM)
**Priority**: Android > Desktop > WASM > iOS

---

## Features

### 1. Authentication

**Method**: Telegram Login Widget + JWT

**Admin Allowlist**:
- Storage: Backend config (`application.yml`)
- Environment variable: `ADMIN_USER_IDS` (comma-separated)
- Example: `ADMIN_USER_IDS=123456789,987654321`

**Flow**:
1. User opens app → Login screen with "Login with Telegram" button
2. Telegram Login Widget validates user identity
3. Backend checks if user ID is in allowlist
4. If allowed → JWT token issued, stored locally
5. If not allowed → "Access denied" error

**Token Management**:
- JWT expiration: 24 hours
- Auto-refresh when token near expiry
- Logout clears local token

---

### 2. Dashboard (Home Screen)

**Purpose**: Overview of system status at a glance

**Components**:

| Section | Data |
|---------|------|
| **Service Status** | Running/Stopped indicator, uptime |
| **Deploy Info** | Last commit SHA, deploy timestamp, Docker image version |
| **Quick Stats** | Total chats, messages today vs yesterday (with trend) |
| **Quick Actions** | Restart Bot button |

**Refresh**: Pull-to-refresh + auto-refresh every 30s

---

### 3. Chat Statistics

**Purpose**: View connected chats and their activity

**List View**:
- Chat name
- Messages today (count)
- Messages yesterday (count)
- Trend indicator (up/down/same)

**Sorting**: By messages today (descending)

**Period**: Today + Yesterday with trend comparison

---

### 4. Deployment Management

**GitHub Integration**:
- Authentication: Personal Access Token (stored on backend)
- Repository: `AndVl1/chatkeep`

**Available Workflows**:
1. `deploy.yml` - Deploy to Production
2. `ci.yml` - Run CI checks

**UI**:
- List of available workflows
- "Run" button for each
- Confirmation dialog before execution
- Status indicator (queued/running/completed/failed)
- Link to GitHub Actions page

**Workflow Status**:
- Last run status
- Last run timestamp
- Triggered by (username)

---

### 5. Bot Logs

**Source**: Docker container logs via SSH

**Display**:
- Last 100 lines
- Scrollable log view
- Monospace font
- Basic syntax highlighting (ERROR/WARN/INFO)

**Actions**:
- Refresh button
- Copy to clipboard
- Filter by log level (optional, nice-to-have)

---

### 6. Quick Actions

**Restart Bot**:
- SSH command: `docker restart chatkeep-app`
- Confirmation dialog: "Are you sure you want to restart the bot?"
- Shows progress indicator
- Success/failure notification

---

### 7. Settings

**Options**:
- Theme: Light / Dark / System (switch in app)
- About: App version, build info
- Logout

---

## UI/UX Design

### Theme
- **Style**: Material Design 3
- **Colors**: Dynamic color scheme
- **Dark Mode**: Manual toggle in settings (Light/Dark/System)

### Navigation

**Adaptive Navigation**:
- **Mobile** (< 600dp): Bottom Navigation Bar
  - Dashboard | Chats | Deploy | Settings
- **Desktop/Tablet** (>= 600dp): Navigation Rail (side)
  - Same 4 destinations with icons and labels

### States

Every screen handles:
1. **Loading**: Centered circular progress
2. **Error**: Error message with retry button
3. **Empty**: Appropriate empty state illustration
4. **Offline**: Banner "No connection" at top

---

## Technical Architecture

### KMP Project Structure

```
chatkeep-admin/
├── core/
│   ├── common/      # Utilities, Result types
│   ├── network/     # Ktor HTTP client, API service
│   ├── ui/          # Theme, shared components
│   ├── data/        # DataStore preferences
│   └── database/    # Room (if needed for caching)
├── feature/
│   ├── auth/
│   │   ├── api/     # AuthComponent interface
│   │   └── impl/    # Login screen, Telegram widget
│   ├── dashboard/
│   │   ├── api/     # DashboardComponent interface
│   │   └── impl/    # Dashboard screen
│   ├── chats/
│   │   ├── api/     # ChatsComponent interface
│   │   └── impl/    # Chat list screen
│   ├── deploy/
│   │   ├── api/     # DeployComponent interface
│   │   └── impl/    # Deploy management screen
│   ├── logs/
│   │   ├── api/     # LogsComponent interface
│   │   └── impl/    # Logs viewer screen
│   └── settings/
│       ├── api/     # SettingsComponent interface
│       └── impl/    # Settings screen
└── composeApp/      # Platform entry points
```

### Technology Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin 2.1.0 |
| UI | Compose Multiplatform 1.7.3 |
| Navigation | Decompose 3.2.0 |
| HTTP | Ktor Client 3.1.1 |
| Serialization | kotlinx.serialization 1.7.3 |
| Preferences | DataStore 1.1.1 |
| DI | Manual / Metro (optional) |

### API Endpoints (Backend)

**New endpoints to implement**:

```
# Admin Auth
POST /api/v1/admin/auth/login
  Body: { telegramData: TelegramLoginData }
  Response: { token: String, user: AdminUser }

GET /api/v1/admin/auth/me
  Header: Authorization: Bearer <token>
  Response: { user: AdminUser }

# Dashboard
GET /api/v1/admin/dashboard
  Response: {
    serviceStatus: { running: Boolean, uptime: Long },
    deployInfo: { commitSha: String, deployedAt: Instant, imageVersion: String },
    quickStats: { totalChats: Int, messagesToday: Int, messagesYesterday: Int }
  }

# Chats
GET /api/v1/admin/chats
  Response: [{
    chatId: Long,
    chatTitle: String,
    messagesToday: Int,
    messagesYesterday: Int
  }]

# GitHub Workflows
GET /api/v1/admin/workflows
  Response: [{
    id: String,
    name: String,
    lastRun: { status: String, timestamp: Instant, triggeredBy: String }?
  }]

POST /api/v1/admin/workflows/{workflowId}/trigger
  Response: { runId: Long, url: String }

# Logs
GET /api/v1/admin/logs?lines=100
  Response: { lines: [String], timestamp: Instant }

# Quick Actions
POST /api/v1/admin/actions/restart
  Response: { success: Boolean, message: String }
```

---

## Deployment

### WASM Build
- Output: Static files in `composeApp/build/dist/wasmJs/productionExecutable/`
- Host: Nginx on VPS at `admin.chatmoderatorbot.ru`

### Nginx Config (admin subdomain)
```nginx
server {
    listen 443 ssl http2;
    server_name admin.chatmoderatorbot.ru;

    ssl_certificate /etc/letsencrypt/live/chatmoderatorbot.ru/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/chatmoderatorbot.ru/privkey.pem;

    root /var/www/chatkeep-admin;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

### Mobile Apps
- Android: APK distribution (manual or via internal testing)
- iOS: TestFlight (requires Apple Developer account)
- Desktop: JAR distribution

---

## Security Considerations

1. **Admin Allowlist**: Only pre-approved Telegram user IDs can access
2. **JWT Tokens**: Short-lived (24h), stored securely (DataStore encrypted)
3. **GitHub PAT**: Stored only on backend, never exposed to clients
4. **SSH Commands**: Executed through backend API, not directly from client
5. **HTTPS Only**: All API calls over TLS

---

## Implementation Priority

### Phase 1: Core (MVP)
1. Auth (Login/Logout)
2. Dashboard (basic status)
3. Chats list

### Phase 2: Deployment
4. GitHub workflow integration
5. Workflow trigger + status

### Phase 3: Operations
6. Bot logs viewer
7. Quick actions (restart)

### Phase 4: Polish
8. Settings (theme switch)
9. Adaptive navigation
10. Error handling refinement

---

## Environment Variables

### Backend
```
ADMIN_USER_IDS=123456789,987654321
GITHUB_PAT=ghp_xxxxxxxxxxxx
GITHUB_REPO=AndVl1/chatkeep
```

### CI/CD
- `DEPLOY_HOST` - VPS hostname
- `DEPLOY_USER` - SSH user
- `DEPLOY_SSH_KEY` - SSH private key

---

## Acceptance Criteria

1. [ ] User can login with Telegram, non-admins are rejected
2. [ ] Dashboard shows service status and deploy info
3. [ ] Chat list displays with today/yesterday message counts
4. [ ] Can trigger deploy workflow with confirmation
5. [ ] Can view last 100 lines of bot logs
6. [ ] Can restart bot with confirmation
7. [ ] Theme switch works (Light/Dark/System)
8. [ ] All 4 platforms build and run
9. [ ] WASM version deployed to admin.chatmoderatorbot.ru
10. [ ] Android app tested on real device
