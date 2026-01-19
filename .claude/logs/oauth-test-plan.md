# OAuth Flow Test Plan - admin.chatmodtest.ru

**Test Date**: 2026-01-20
**URL**: https://admin.chatmodtest.ru/
**Test Type**: Final OAuth Flow Verification

---

## Test Steps

### Step 1: Clear Cache and Reload
- Navigate to https://admin.chatmodtest.ru/
- Open DevTools > Application > Clear site data (clear everything)
- Hard refresh (Cmd+Shift+R)
- **Verify**: Fresh WASM files loaded

### Step 2: OAuth Flow Test
1. Click "Login with Telegram" button
2. **CHECK POPUP URL**: Should be `https://admin.chatmodtest.ru/auth/telegram-login?state=...`
3. Complete Telegram authentication
4. After auth success, click "Open Chatkeep Admin" button
5. Check console for "[Chatkeep Auth]" debug messages
6. **Verify**: Popup closes automatically
7. **Verify**: Main app receives auth data and shows authenticated state

---

## Expected Results

### Popup URL
- **Domain**: admin.chatmodtest.ru (same-origin)
- **Path**: /auth/telegram-login
- **Query**: ?state=... (OAuth state parameter)

### Console Messages
- `[Chatkeep Auth] Sending auth data via postMessage...`
- `[Chatkeep Auth] postMessage sent successfully`

### Popup Behavior
- `window.opener` should be truthy (hasOpener: true)
- Popup closes within 300ms after auth
- Main app receives message and updates state

### Authentication Result
- Main app redirects or shows authenticated state
- User data displayed correctly
- No errors in console

---

## Critical Checks

### ✅ Same-Origin Policy
- Popup URL must be on same domain (admin.chatmodtest.ru)
- Cross-origin would block window.opener access

### ✅ PostMessage Flow
- Popup sends auth data via postMessage
- Main app receives and processes message
- Popup closes automatically

### ✅ Error Handling
- No console errors
- No network errors
- Proper error messages if auth fails

---

## Issues to Report

If any of these occur, document:
- ❌ Popup URL on wrong domain
- ❌ window.opener is null
- ❌ Popup doesn't close
- ❌ Main app doesn't receive auth data
- ❌ Console errors
- ❌ Network errors

---

## Test Execution

**Assigned to**: manual-qa agent
**Status**: Pending
**Tools**: Chrome MCP (tabs_context, navigate, screenshot, computer, read_console_messages)
