# Testing Mini App Locally

This guide explains how to test the Chatkeep Mini App in the Telegram environment with minimal setup.

## Prerequisites

Before starting, ensure you have installed:
- **Node.js 18+** and npm
- **Docker** and Docker Compose
- **Java 17+** (for backend)
- **Gradle** (or use `./gradlew` wrapper included in repo)

## Quick Start (Minimal Setup)

For local development without Telegram:

```bash
# Start everything with one command
./scripts/mini-app-helper.sh all
```

This starts:
- PostgreSQL database
- Backend (http://localhost:8080)
- Mini App (http://localhost:5173)

The Mini App has a **mock Telegram environment** built-in, so you can test most features in the browser without Telegram.

### Development Modes

The Mini App supports two authentication modes:

| Mode | URL | Use Case |
|------|-----|----------|
| **Web Mode** | `http://localhost:5173/` | Shows Login Page with Telegram Login Widget |
| **Mock Mini App** | `http://localhost:5173/?mock=true` | Simulates Telegram Mini App environment |

**Web Mode** is useful for testing the Login Widget flow (requires public URL, see below).
**Mock Mini App** is useful for testing Mini App features without Telegram.

---

## Testing Telegram Login Widget

The **Telegram Login Widget** (shown in Web Mode) has special requirements that make localhost testing challenging.

### Why "Bot domain invalid" Error?

Telegram Login Widget requires:
1. A **public domain** configured in @BotFather
2. **HTTPS** connection (HTTP not allowed)
3. Domain must match exactly where the widget is hosted

**localhost does NOT work** because Telegram only trusts explicitly configured public domains.

### Setting Domain in @BotFather

**Important**: When setting domain, use only the domain name without protocol!

```
# WRONG - will show "The message should contain one domain name"
http://localhost:5173/
https://example.com/

# CORRECT
example.com
myapp.ngrok.io
abc123.trycloudflare.com
```

**Steps:**
1. Open @BotFather in Telegram
2. Send `/setdomain`
3. Select your bot
4. Enter domain (e.g., `myapp.ngrok.io`)

### Option A: Cloudflare Tunnel (Recommended)

Persistent URL that doesn't change on restart.

```bash
# 1. Install cloudflared
brew install cloudflared

# 2. Start Mini App
cd mini-app && npm run dev

# 3. In another terminal, start tunnel to frontend
cloudflared tunnel --url http://localhost:5173

# 4. Copy the URL (e.g., https://abc-xyz.trycloudflare.com)

# 5. Start backend tunnel (for API calls)
cloudflared tunnel --url http://localhost:8080
```

Then in @BotFather:
```
/setdomain
[select your bot]
abc-xyz.trycloudflare.com
```

### Option B: ngrok

```bash
# 1. Install ngrok
brew install ngrok

# 2. Start Mini App
cd mini-app && npm run dev

# 3. Start ngrok tunnel
ngrok http 5173

# 4. Copy the HTTPS URL from ngrok output
```

**Note**: Free ngrok URLs change on each restart. You'll need to update @BotFather each time.

### Option C: Create Staging Bot (Best for Long-term)

Create a separate bot for development:

1. In @BotFather: `/newbot`
2. Name: `Chatkeep Dev`
3. Username: `chatkeep_dev_bot` (must be unique)
4. Copy the new token
5. Set domain: `/setdomain` → select dev bot → enter your tunnel URL
6. Add to `.env`:
   ```env
   TELEGRAM_BOT_TOKEN=your_dev_bot_token
   ```
7. Add to `mini-app/.env.development`:
   ```env
   VITE_BOT_USERNAME=chatkeep_dev_bot
   ```

This way your production bot settings remain unchanged.

### Complete Login Widget Testing Flow

```bash
# Terminal 1: Start database
docker compose up -d

# Terminal 2: Start backend
./scripts/mini-app-helper.sh backend

# Terminal 3: Start frontend
cd mini-app && npm run dev

# Terminal 4: Start tunnel (frontend)
cloudflared tunnel --url http://localhost:5173
# Note the URL: https://xxx.trycloudflare.com

# Terminal 5: Start tunnel (backend) - needed for API calls
cloudflared tunnel --url http://localhost:8080
# Note the URL: https://yyy.trycloudflare.com
```

Update `mini-app/.env.development`:
```env
VITE_API_URL=https://yyy.trycloudflare.com/api/v1/miniapp
```

Then:
1. Set domain in @BotFather: `xxx.trycloudflare.com`
2. Open `https://xxx.trycloudflare.com` in browser
3. Click "Log in with Telegram" button
4. Authorize in Telegram
5. You should be redirected back and logged in

### Troubleshooting Login Widget

| Error | Cause | Solution |
|-------|-------|----------|
| "Bot domain invalid" | Domain not set or mismatch | Set domain in @BotFather matching your tunnel URL |
| "The message should contain one domain name" | URL with protocol in @BotFather | Remove `http://` or `https://` prefix |
| Widget doesn't appear | Script not loaded | Check browser console for CSP errors |
| Login succeeds but API fails | CORS or API URL mismatch | Ensure backend is accessible and CORS configured |

---

## Testing in Telegram

### Option 1: Telegram Test Environment (Recommended for Solo Dev)

The Telegram test environment allows **HTTP** and **localhost**, making it the easiest option.

#### Step 1: Access Telegram Test Server

| Platform | How to Access |
|----------|---------------|
| **iOS** | Settings → Tap version 10 times → Accounts → Login to another account → Test |
| **Android** | Settings → Tap version 10 times → Accounts → Login to another account → Test |
| **macOS** | Settings → Tap version 10 times → Cmd+Click "Add Account" |
| **Desktop (Win/Linux)** | Shift+Alt+Right-click "Add Account" → Test Server |

#### Step 2: Create Test Bot

1. In **Telegram Test Server**, message [@BotFather](https://t.me/BotFather)
2. Send `/newbot`
3. Name: `Chatkeep Test`
4. Username: `chatkeep_test_bot` (must be unique)
5. Copy the **bot token**

#### Step 3: Configure Mini App URL

In @BotFather:
1. Send `/setmenubutton`
2. Select your test bot
3. Set URL to: `http://localhost:5173`
4. Set title: `Open Chatkeep`

#### Step 4: Start Development

```bash
# Terminal 1: Start backend
./gradlew bootRun

# Terminal 2: Start Mini App
cd mini-app && npm run dev
```

#### Step 5: Test

1. In Telegram Test Server, open your test bot
2. Tap the menu button (near the message input)
3. Mini App opens!

---

### Option 2: Public URL with Cloudflare Tunnel (For Team/External Testing)

Use this when you need a **public HTTPS URL** (e.g., testing on mobile or sharing with team).

#### Prerequisites

```bash
# Install cloudflared (macOS)
brew install cloudflared
```

#### Step 1: Start Tunnel

```bash
# Terminal 1: Start Mini App
cd mini-app && npm run dev

# Terminal 2: Start tunnel
./scripts/mini-app-helper.sh tunnel
```

The tunnel will show a public URL like:
```
https://something-random.trycloudflare.com
```

#### Step 2: Configure Bot

In @BotFather:
1. Send `/setmenubutton`
2. Select your bot
3. Set URL to: `https://something-random.trycloudflare.com`

#### Step 3: Test

Open the bot in Telegram (regular, not test server) and tap the menu button.

**Note**: The URL changes each time you restart the tunnel. For persistent URL, set up a named Cloudflare tunnel.

---

### Option 3: ngrok (Alternative to Cloudflare)

```bash
# Install ngrok
brew install ngrok

# Start tunnel
./scripts/mini-app-helper.sh ngrok
```

Same process as Cloudflare, but:
- Free tier has connection limits
- URL changes on restart (free tier)
- Requires account for authenticated tunnels

---

## Environment Configuration

### Backend

Create `.env` file in project root:

```env
TELEGRAM_BOT_TOKEN=your_bot_token_here
DB_HOST=localhost
DB_PORT=5432
DB_NAME=chatkeep
DB_USERNAME=chatkeep
DB_PASSWORD=chatkeep
```

**Note**: These values match the defaults in `docker-compose.yml`.

### Mini App

The Mini App uses these env files:
- `.env.development` - Development API URL (default: `/api/v1/miniapp`)
- `.env.production` - Production API URL

No changes needed for local development.

---

## Troubleshooting

### Mini App shows blank page in Telegram

1. Check if dev server is running: `curl http://localhost:5173`
2. Ensure URL is set correctly in @BotFather
3. For test server: Make sure you're using HTTP (not HTTPS)
4. Check browser console for errors (Telegram Desktop: View → Toggle Developer Tools)

### Backend returns 401 Unauthorized

1. Check if `TELEGRAM_BOT_TOKEN` is set correctly
2. Ensure you're using the correct bot token for the environment
3. Test server bots need test server tokens

### Tunnel URL doesn't work

1. Check if Mini App dev server is running first
2. Try accessing the URL directly in browser
3. Ensure no firewall is blocking the connection

### Mock environment not working

The mock environment is now **opt-in**. To enable it:
1. Access via `http://localhost:5173/?mock=true` (note the `?mock=true` param)
2. Without the param, you'll see the Login Page (Web Mode)

If you see "Authentication Required" error in Mock Mode:
1. Make sure you have `?mock=true` in the URL
2. Check browser console for initialization errors
3. The mock uses fake auth data - backend API calls will return 500 (expected)

---

## Development Workflow

### Recommended Flow

1. **Start everything**: `./scripts/mini-app-helper.sh all`
2. **Develop in browser**: Use mock environment at `http://localhost:5173`
3. **Test in Telegram periodically**: Use tunnel when needed
4. **Before PR**: Test in Telegram Test Server

### Helper Commands

```bash
# Check status of all services
./scripts/mini-app-helper.sh status

# Just the Mini App dev server
./scripts/mini-app-helper.sh dev

# Build for production
./scripts/mini-app-helper.sh build

# Clean and reinstall
./scripts/mini-app-helper.sh clean
./scripts/mini-app-helper.sh install
```

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                    Telegram App                          │
│                                                          │
│  ┌──────────────────────────────────────────────────┐   │
│  │              Mini App WebView                      │   │
│  │                                                    │   │
│  │  ┌─────────────────┐    ┌──────────────────────┐ │   │
│  │  │  React App      │────│  Telegram SDK        │ │   │
│  │  │  (localhost:    │    │  (initData, theme,   │ │   │
│  │  │   5173)         │    │   haptics, etc.)     │ │   │
│  │  └────────┬────────┘    └──────────────────────┘ │   │
│  └───────────│──────────────────────────────────────┘   │
│              │ /api/v1/miniapp/*                         │
└──────────────│───────────────────────────────────────────┘
               │
┌──────────────▼───────────────────────────────────────────┐
│            Backend (localhost:8080)                       │
│  ┌────────────────────────────────────────────────────┐  │
│  │  TelegramAuthFilter → validates initData signature  │  │
│  │  Controllers → Settings, Locks, Blocklist, etc.     │  │
│  │  Services → Business logic                          │  │
│  │  PostgreSQL → Data storage                          │  │
│  └────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────┘
```

---

## Resources

- [Telegram Mini Apps Docs](https://core.telegram.org/bots/webapps)
- [Test Environment Guide](https://docs.telegram-mini-apps.com/platform/test-environment)
- [@telegram-apps/sdk Documentation](https://docs.telegram-mini-apps.com/)
- [Cloudflare Tunnel](https://developers.cloudflare.com/cloudflare-one/connections/connect-applications/)
