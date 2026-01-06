# ADR-001: Dual-Mode Authentication (Mini App + Web)

## Status
Accepted

## Context
The Chatkeep Mini App currently only works when opened from Telegram (Mini App mode). Users requested the ability to access it as a regular web application with Telegram authentication via the Telegram Login Widget.

## Questions & Decisions

### Q1: How to detect Mini App vs Web mode?
**Decision**: Check for `window.Telegram.WebApp.initData` presence
- If initData exists → Mini App mode (user opened from Telegram)
- If initData is empty/missing → Web mode (user opened in browser)

**Rationale**: This is the official Telegram detection method. The initData is only populated when opened from Telegram client.

### Q2: What authentication method for Web mode?
**Decision**: Use Telegram Login Widget with frontend callback
- Widget loads from `telegram.org/js/telegram-widget.js`
- User clicks "Log in with Telegram"
- Telegram popup appears for confirmation
- Widget returns user data + hash to frontend callback
- Frontend sends to backend for verification

**Alternatives considered**:
1. ❌ OAuth redirect flow - More complex, requires redirect handling
2. ❌ Deep link to bot - Poor UX, requires user to manually return
3. ✅ Login Widget with callback - Simple, secure, good UX

### Q3: How to handle widget callback authentication?
**Decision**: Create new backend endpoint `/api/v1/auth/telegram-login` that:
1. Receives widget user data + hash
2. Verifies HMAC-SHA256 signature (same algorithm as Mini App)
3. Returns JWT token for subsequent API calls

**Rationale**: Separates widget auth from Mini App auth. Widget returns different data structure than initData.

### Q4: How to unify auth for API calls?
**Decision**: Use JWT tokens for web mode, keep TMA tokens for Mini App mode
- Mini App: `Authorization: tma <initDataRaw>` (unchanged)
- Web mode: `Authorization: Bearer <jwt>`

**Rationale**:
- Mini App initData is already validated per-request (stateless)
- Web mode needs session token (widget only authenticates once)
- Backend already has TelegramAuthFilter, add JwtAuthFilter

### Q5: Where to store JWT in browser?
**Decision**: Use `localStorage` with key `chatkeep_auth_token`
- Simple, persists across tabs
- Cleared on logout

**Security**:
- JWT expires after 24 hours
- HTTPS only in production
- No sensitive data in token payload

### Q6: How to handle login page routing?
**Decision**: Conditional routing in App.tsx
```
if (isMiniApp) → Show HomePage directly
if (isWeb && !authenticated) → Show LoginPage
if (isWeb && authenticated) → Show HomePage
```

### Q7: Should web users see the same UI as Mini App users?
**Decision**: Yes, same UI with minor adaptations
- Use `AppRoot` from telegram-ui (works in both modes)
- Theme: Use Telegram theme if available, fallback to default
- No platform-specific features (haptics, MainButton) in web mode

### Q8: How to handle auth expiry in web mode?
**Decision**: Redirect to login page on 401 response
- API client intercepts 401 errors
- Clears stored token
- Redirects to /login

### Q9: Bot configuration for Login Widget?
**Decision**: Use same bot as Mini App
- Configure domain in @BotFather: `/setdomain`
- Widget uses bot username for authentication

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    User Access                           │
├──────────────────────┬──────────────────────────────────┤
│   Telegram App       │        Web Browser               │
│   (Mini App mode)    │        (Web mode)                │
└──────────┬───────────┴────────────────┬─────────────────┘
           │                            │
     ┌─────▼─────┐                ┌─────▼─────┐
     │ initData  │                │ No initData│
     │ present   │                │           │
     └─────┬─────┘                └─────┬─────┘
           │                            │
     ┌─────▼─────┐                ┌─────▼─────────┐
     │ HomePage  │                │ LoginPage     │
     │ (direct)  │                │ (Login Widget)│
     └─────┬─────┘                └─────┬─────────┘
           │                            │
           │                      ┌─────▼─────────┐
           │                      │ POST /auth/   │
           │                      │ telegram-login│
           │                      └─────┬─────────┘
           │                            │
           │                      ┌─────▼─────────┐
           │                      │ Returns JWT   │
           │                      └─────┬─────────┘
           │                            │
     ┌─────▼─────┐                ┌─────▼─────────┐
     │ API calls │                │ Store JWT     │
     │ with TMA  │                │ in localStorage│
     │ header    │                └─────┬─────────┘
     └─────┬─────┘                      │
           │                      ┌─────▼─────────┐
           │                      │ API calls     │
           │                      │ with Bearer   │
           │                      │ header        │
           │                      └─────┬─────────┘
           │                            │
           └────────────┬───────────────┘
                        │
                  ┌─────▼─────────────────┐
                  │ Backend               │
                  │ - TelegramAuthFilter  │
                  │   (validates TMA)     │
                  │ - JwtAuthFilter       │
                  │   (validates JWT)     │
                  └───────────────────────┘
```

## Files to Create/Modify

### Frontend (New Files)
- `src/pages/LoginPage.tsx` - Login page with Telegram widget
- `src/components/auth/TelegramLoginWidget.tsx` - Widget wrapper
- `src/hooks/auth/useAuthMode.ts` - Detect Mini App vs Web
- `src/hooks/auth/useWebAuth.ts` - Web auth state management
- `src/stores/authStore.ts` - Auth state with Zustand

### Frontend (Modified Files)
- `src/App.tsx` - Add mode detection and conditional routing
- `src/api/client.ts` - Support both TMA and Bearer tokens
- `src/index.html` - Load widget script conditionally

### Backend (New Files)
- `TelegramLoginController.kt` - Widget auth endpoint
- `JwtAuthFilter.kt` - JWT validation filter
- `JwtService.kt` - JWT generation/validation

### Backend (Modified Files)
- `TelegramAuthService.kt` - Extract shared HMAC validation
- `SecurityConfig.kt` - Add JWT filter to chain

## Consequences

### Positive
- Users can access app from any browser
- Shared links work without Telegram app
- Single codebase for both modes
- Existing Mini App users unaffected

### Negative
- Additional backend complexity (JWT handling)
- Two auth flows to maintain
- Web mode lacks Telegram-native features (haptics, etc.)

### Risks
- Widget authentication requires correct domain in @BotFather
- JWT secret must be securely stored
- Token expiry handling needs testing
