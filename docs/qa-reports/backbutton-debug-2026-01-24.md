# BackButton Debug Session Report

**Date**: 2026-01-24
**Platform**: Web (Telegram Mini App)
**Environment**: chatmodtest.ru
**Tester**: manual-qa agent

---

## Test Objective

Debug why the Telegram native BackButton is not appearing in the Mini App by checking console logs for `[BackButton]` and `[AppLayout]` messages.

---

## Test Environment Setup

1. Navigated to https://web.telegram.org/a/?account=2
2. Opened AdminTestBot chat
3. Attempted to open Mini App within Telegram Web iframe

---

## Key Findings

### Issue 1: Console Logs Not Accessible from Iframe

**Problem**: The Mini App runs inside an iframe within Telegram Web, and browser security policies prevent reading console logs from the iframe context using the MCP Chrome tools.

**Evidence**:
- Console tracking on the Telegram Web tab (2029639774) only showed Telegram's own logs
- No `[BackButton]` or `[AppLayout]` logs were captured from the iframe

### Issue 2: Direct URL Access Shows BackButton.isAvailable = false

When accessing chatmodtest.ru directly (not via Telegram Mini App), the following console output was captured:

**Home Page** (`/`):
```
[AppLayout] isMiniApp: false isHomePage: true path: /
[BackButton] mount.isAvailable: false
[BackButton] show.isAvailable: false
[BackButton] hide.isAvailable: false
[BackButton] visible: false
[BackButton] mount not available, skipping
```

**Settings Page** (`/chat/-1003591184161/settings`):
```
[AppLayout] isMiniApp: false isHomePage: false path: /chat/-1003591184161/settings
[BackButton] mount.isAvailable: false
[BackButton] show.isAvailable: false
[BackButton] hide.isAvailable: false
[BackButton] visible: false
[BackButton] mount not available, skipping
```

---

## Analysis

### Root Cause Identified

The BackButton is not showing because **`isAvailable` is `false`** for all BackButton methods:
- `mount.isAvailable: false`
- `show.isAvailable: false`
- `hide.isAvailable: false`

This happens when:
1. The app detects `isMiniApp: false` (not running in Telegram WebApp environment)
2. The Telegram WebApp SDK's `BackButton` object is not available or not supported

### Expected Behavior

In a proper Telegram Mini App environment (inside telegram://), the logs should show:
```
[AppLayout] isMiniApp: true isHomePage: false path: /chat/XXX/settings
[BackButton] mount.isAvailable: true
[BackButton] show.isAvailable: true
[BackButton] visible: true
[BackButton] mounted
[BackButton] show() called
```

---

## Limitations Encountered

1. **Iframe Security Boundary**: Cannot read console logs from Mini App iframe within Telegram Web
2. **Test Environment**: Testing via web.telegram.org/a/ doesn't provide access to iframe console
3. **Alternative Required**: Need to test using:
   - Mobile Telegram app (real device or emulator)
   - Telegram Desktop app
   - Or inject logging into the Mini App itself to send logs to an external service

---

## Recommendations

### Immediate Actions

1. **Add Remote Logging**: Implement a remote logging service in the Mini App to send console logs to an external endpoint (e.g., Sentry, LogRocket, or custom endpoint)

2. **Test on Real Device**: Open the Mini App on a mobile device via Telegram mobile app to verify if BackButton appears natively

3. **Check SDK Version**: Verify that `@telegram-apps/sdk` is the latest version and BackButton is supported in the environment

4. **Add Debug UI**: Temporarily add a debug overlay in the Mini App UI that shows:
   ```jsx
   {import.meta.env.DEV && (
     <div style={{position: 'fixed', top: 0, left: 0, background: 'red', color: 'white', zIndex: 9999}}>
       isMiniApp: {String(isMiniApp)}<br/>
       backButton.isAvailable: {String(backButton?.isAvailable)}<br/>
       path: {location.pathname}
     </div>
   )}
   ```

### Code Review Points

Review `mini-app/src/components/layout/AppLayout.tsx`:

1. Check how `isMiniApp` is determined
2. Verify BackButton initialization logic
3. Confirm that `backButton.mount()` and `backButton.show()` are called when conditions are met
4. Check if there's a version mismatch between SDK and Telegram WebApp API

---

## Test Summary

**Total Tests**: 2
- Direct URL access test (home page): COMPLETED
- Direct URL access test (settings page): COMPLETED
- Telegram Web iframe test: BLOCKED (cannot access iframe console)

**Issues Found**: 1
- BackButton.isAvailable = false when not in Telegram Mini App environment (expected behavior)

**Console Logs Captured**:
- Home page: 6 messages
- Settings page: 6 messages
- Telegram Web iframe: 0 messages (blocked by security policy)

---

## Next Steps

1. Test in actual Telegram mobile app or desktop client
2. Add remote logging or debug overlay
3. Verify SDK configuration and version
4. Check if BackButton is supported in current Telegram client version

---

## Environment Details

- **Browser**: Chrome (via MCP tools)
- **Test URLs**:
  - https://web.telegram.org/a/?account=2#8342819049 (Telegram Web)
  - https://chatmodtest.ru/ (direct access)
  - https://chatmodtest.ru/chat/-1003591184161/settings (direct access)
- **Tab IDs**: 2029639774, 2029639340, 2029639635
