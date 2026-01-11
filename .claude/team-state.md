# TEAM STATE

## Classification
- Type: BUG_FIX
- Complexity: MEDIUM
- Workflow: STANDARD

## Task
Fix mobile admin app auth flow - nginx doesn't proxy /auth/ to backend

## Server Credentials
- IP: 89.125.243.104
- User: root
- Password: YrB2k5Sc9Y3E7y2
- Deploy path: /root/chatkeep

## Progress
- [x] Phase 1: Discovery - COMPLETED (identified nginx config issue)
- [x] Phase 5: Implementation - COMPLETED
- [x] Phase 6: Review/Testing - COMPLETED (manual-qa passed)
- [x] Phase 7: Summary - COMPLETED

## Root Cause (FIXED)
Nginx config didn't proxy `/auth/` to backend and SSL was not configured.

## Fixes Applied
1. Added `/auth/` proxy to `admin.chatmoderatorbot.ru.conf`
2. Added `/auth/` and `/api/` proxy to `default.conf` for tunnel access
3. Obtained SSL certificate for admin.chatmoderatorbot.ru via Let's Encrypt
4. Enabled HTTPS (port 443) in docker-compose.prod.yml
5. Configured HTTP → HTTPS redirect

## Test Results (manual-qa)
- App launch: PASS
- Auth screen: PASS
- Login button: PASS
- Browser opens Telegram Widget: PASS
- HTTPS connection: PASS
- No errors in logs: PASS

## Phase 4 Output - Architecture Design

### Deeplink OAuth Flow
1. User clicks "Login with Telegram" → app opens browser
2. Browser loads `https://admin.chatmoderatorbot.ru/auth/telegram-login?state=<csrf>`
3. Page shows Telegram Login Widget
4. User authenticates with Telegram
5. Widget callback triggers redirect to `chatkeep://auth/callback?id=...&hash=...&state=...`
6. App receives deeplink, validates state, calls backend login
7. Backend validates Telegram hash, returns JWT
8. App stores token, navigates to main screen

### Implementation Plan

#### Backend Changes
1. Add `/api/v1/admin/**` to CORS config
2. Create Telegram Login Widget HTML page (served at /auth/telegram-login)
3. Page redirects to deeplink after auth

#### Mobile App Changes
1. Update API URL to `admin.chatmoderatorbot.ru` (all platforms)
2. Add deeplink config (Android manifest, iOS Info.plist)
3. Create PlatformBrowser expect/actual (open URLs)
4. Update DefaultAuthComponent for OAuth flow
5. Handle deeplink in MainActivity (Android)

### Files to Create/Modify

#### Backend
- `CorsConfig.kt` - add admin CORS
- `TelegramLoginPageController.kt` - NEW - serve login widget page
- `resources/templates/telegram-login.html` - NEW - login widget HTML

#### Mobile (all platforms)
- `PlatformFactory.android.kt` - API URL
- `PlatformFactory.ios.kt` - API URL
- `PlatformFactory.jvm.kt` - API URL
- `PlatformFactory.wasmJs.kt` - API URL

#### Mobile (Android specific)
- `AndroidManifest.xml` - deeplink intent filter
- `MainActivity.kt` - handle deeplink intent

#### Mobile (common)
- `PlatformBrowser.kt` - NEW expect/actual for browser open
- `DeepLinkData.kt` - NEW parse deeplink params
- `DefaultAuthComponent.kt` - OAuth flow logic
- `AuthComponent.kt` - add deeplink handling

## Recovery
Phase 5: Launch developer + developer-mobile agents in parallel.
