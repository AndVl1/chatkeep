# Mini App Subdomain Setup

## Overview

This document describes the setup of `miniapp.chatmoderatorbot.ru` subdomain for Telegram Mini App exclusive access.

## Implementation Complete

### 1. Nginx Configuration ✓
- Created `/docker/nginx/sites/miniapp.chatmoderatorbot.ru.conf`
- HTTP server with ACME challenge support
- HTTPS server with browser detection (User-Agent based)
- Redirects browser access to `chatmoderatorbot.ru`
- Serves Mini App for Telegram WebView

### 2. Frontend Changes ✓
- Updated `useAuthMode.ts` to enforce strict Telegram requirement for `miniapp.*` subdomain
- Updated `App.tsx` with subdomain-specific error messages
- Main domain `chatmoderatorbot.ru` retains dual-mode support (Telegram + browser)

### 3. SSL Certificate Script ✓
- Updated `scripts/setup-ssl.sh` to include `miniapp.chatmoderatorbot.ru` in DOMAINS array
- Created GitHub Actions workflow `setup-ssl.yml` for automated SSL setup

### 4. Deployment ✓
- All code changes deployed to production branch
- GitHub Actions deploy workflow successfully completed
- Nginx configs deployed to server

## Pending: SSL Certificate

**Status**: The SSL certificate does not yet include `miniapp.chatmoderatorbot.ru`.

**Issue**: Let's Encrypt HTTP-01 challenge fails with 404 when trying to verify domain ownership.

**Root Cause**: The nginx configuration for `miniapp.chatmoderatorbot.ru` is not being loaded by nginx on the server, despite the file being present.

### Debugging Steps Taken

1. ✓ Verified DNS: `miniapp.chatmoderatorbot.ru` resolves to `89.125.243.104`
2. ✓ Verified nginx config syntax is correct
3. ✓ Deployed code multiple times via GitHub Actions
4. ✗ HTTP access to `miniapp.chatmoderatorbot.ru` returns 404

### Manual Fix Required

SSH into the production server and run:

```bash
# 1. Navigate to project directory
cd /root/chatkeep

# 2. Verify miniapp config file exists
ls -la docker/nginx/sites/miniapp.chatmoderatorbot.ru.conf

# 3. Check if nginx container sees the config
docker exec chatkeep-nginx ls -la /etc/nginx/sites-enabled/

# 4. If config is missing in container, restart nginx to reload volume mounts
docker restart chatkeep-nginx

# 5. Wait for nginx to start
sleep 5

# 6. Test nginx configuration
docker exec chatkeep-nginx nginx -t

# 7. Test HTTP access to miniapp subdomain
curl -I http://miniapp.chatmoderatorbot.ru/.well-known/acme-challenge/test

# 8. If HTTP returns 404, check nginx error logs
docker logs chatkeep-nginx 2>&1 | tail -50

# 9. Once HTTP access works, run SSL certificate setup
sudo /root/chatkeep/scripts/setup-ssl.sh

# OR use the GitHub Actions workflow:
# Go to https://github.com/AndVl1/chatkeep/actions/workflows/setup-ssl.yml
# Click "Run workflow" on production branch
```

### Expected Result After SSL Setup

- `https://miniapp.chatmoderatorbot.ru` - Accessible from Telegram WebView only
- `https://chatmoderatorbot.ru` - Accessible from both Telegram and browser

### Testing

Once SSL is configured:

1. **Browser test**: Open `https://miniapp.chatmoderatorbot.ru` in browser
   - Expected: HTTP 301 redirect to `https://chatmoderatorbot.ru`

2. **Telegram test**: Open Mini App from Telegram
   - Bot: @chatAutoModerBot
   - Expected: Mini App loads normally from `miniapp.chatmoderatorbot.ru`

## Architecture

### Request Flow

```
User Request → DNS → Nginx → Decision Point

Decision Point:
├─ miniapp.chatmoderatorbot.ru
│  ├─ User-Agent contains "Telegram"? → Serve Mini App
│  └─ User-Agent is browser? → Redirect to chatmoderatorbot.ru
│
└─ chatmoderatorbot.ru
   ├─ window.Telegram.WebApp.initData present? → Mini App mode
   └─ No initData? → Show LoginPage (browser mode)
```

### Security Layers

1. **Nginx Layer** (Primary): User-Agent detection + redirect
2. **Frontend Layer** (Secondary): Hostname check + strict initData validation

## Files Modified

- `docker/nginx/sites/miniapp.chatmoderatorbot.ru.conf` (created)
- `scripts/setup-ssl.sh` (updated)
- `mini-app/src/hooks/auth/useAuthMode.ts` (updated)
- `mini-app/src/App.tsx` (updated)
- `.github/workflows/setup-ssl.yml` (created)
- `.github/workflows/update-ssl-cert.yml` (created)
- `.github/workflows/debug-nginx.yml` (created)

## Deployment

All changes are in the `production` branch and have been deployed via GitHub Actions.

To trigger a new deployment:
```bash
gh workflow run deploy.yml --ref production
```

## Rollback

If issues occur, the main domain `chatmoderatorbot.ru` continues to work normally as before.

To remove miniapp subdomain:
1. Delete `/docker/nginx/sites/miniapp.chatmoderatorbot.ru.conf`
2. Deploy changes
3. Certificate will continue to work for other subdomains
