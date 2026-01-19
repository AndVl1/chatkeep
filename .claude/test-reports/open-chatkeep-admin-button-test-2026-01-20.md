# Test Session Report

**Feature Tested**: "Open ChatKeep Admin" Button Fix (postMessage Communication)
**Platform**: Web (WASM)
**Environment**: https://admin.chatmodtest.ru/
**Date**: 2026-01-20
**Tester**: manual-qa agent

---

## Tests Executed

### Test 1: Telegram OAuth Login Flow
**Status**: PASS

**Steps**:
1. Navigated to https://admin.chatmodtest.ru/
2. Clicked "Login with Telegram" button
3. Popup window opened successfully
4. Authenticated using existing Telegram account (Андрей)
5. Authentication completed successfully

**Verified**:
- Popup opens with named window (`chatkeep-auth`) - fix IS deployed in WASM
- Authentication completes successfully
- "Authenticated successfully!" message shown

**Screenshots**: ss_8813y7xko, ss_7826tm2z8, ss_4461rwsae

**Issues**: None

---

### Test 2: "Open ChatKeep Admin" Button Functionality
**Status**: FAIL

**Steps**:
1. After successful authentication, "Open ChatKeep Admin" button appears
2. Clicked the button (ref_5 link element)
3. Observed behavior

**Expected**:
- postMessage sent to parent window with auth data
- Console logs show "[Chatkeep Auth]" debug messages
- Popup closes automatically
- Main app receives authentication and redirects

**Actual**:
- Button does nothing (popup stays open)
- No console messages logged
- No postMessage communication occurs
- Main app remains on login screen

**Screenshots**: ss_7285s50mb, ss_6752tmqc8

**Errors**:
- Console (popup): No error messages, no "[Chatkeep Auth]" debug logs
- Console (main app): No messages received

**Root Cause Analysis**:
1. JavaScript environment check shows:
   - `isMobile = false` (correct)
   - `hasOpener = false` (BUG!)
   - `window.opener = null` (blocked by browser)

2. Code path: Since `hasOpener = false`, the condition `if (!isMobile && hasOpener)` fails, causing code to fall through to mobile deeplink path

3. Button href: `chatkeep://auth/callback?...` (deep link, not postMessage)

**Why `window.opener` is null**:
Cross-origin security issue - popup opened from different domain:
- Main app: `https://admin.chatmodtest.ru/`
- OAuth popup: `https://admin.chatmoderatorbot.ru/auth/telegram-login?...`

Browser blocks `window.opener` access due to cross-origin policy.

---

### Test 3: Verify Fix Deployment Status
**Status**: PARTIALLY DEPLOYED

**WASM Fix (DEPLOYED)**:
- File: `chatkeep-admin/core/common/src/wasmJsMain/kotlin/com/chatkeep/admin/core/common/PlatformBrowser.wasmJs.kt`
- Change: `window.open(url, 'chatkeep-auth', 'width=600,height=700,popup=yes')`
- Status: ✅ DEPLOYED (confirmed via JavaScript inspection)

**Backend Template Fix (DEPLOYED)**:
- File: `src/main/resources/templates/telegram-login.html`
- Changes: targetOrigin fix, console logging, error handling
- Status: ✅ DEPLOYED (confirmed via code review)

**Configuration Issue (NOT FIXED)**:
- File: `chatkeep-admin/feature/auth/impl/src/commonMain/kotlin/com/chatkeep/admin/feature/auth/DefaultAuthComponent.kt`
- Line 102: `val oauthUrl = "https://admin.chatmoderatorbot.ru/auth/telegram-login?state=$stateToken"`
- Issue: Hardcoded production URL causes cross-origin issue in test environment
- Status: ❌ NOT FIXED

---

## Technical Details

### JavaScript Environment Check (Popup Window)
```json
{
  "isMobile": false,
  "hasOpener": false,
  "openerOrigin": "no opener",
  "userAgent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36"
}
```

### Page Structure (Popup)
```
heading "ChatKeep Admin"
generic "Authenticated successfully!" ✓
link "Open ChatKeep Admin" href="chatkeep://auth/callback?..."
```

### Cross-Origin Issue
- Main app origin: `https://admin.chatmodtest.ru`
- Popup origin: `https://admin.chatmoderatorbot.ru`
- Result: Browser blocks `window.opener` due to different origins

### Code Path Taken
```javascript
// Line 187 in telegram-login.html
if (!isMobile && hasOpener) {
    // ❌ This code path NOT executed (hasOpener = false)
    // Should use postMessage
} else {
    // ✅ This code path executed instead
    // Uses deep links (mobile behavior)
    const deeplinkUrl = `chatkeep://auth/callback?${params.toString()}`;
}
```

---

## Summary

**Total Tests**: 3
**Passed**: 1
**Failed**: 2

**Issues Found**:

### Issue #1: Cross-Origin OAuth Breaks window.opener - CRITICAL
**Severity**: CRITICAL
**Category**: Configuration Bug

**Description**:
The WASM app deployed at `admin.chatmodtest.ru` hardcodes the OAuth URL to `admin.chatmoderatorbot.ru`, causing a cross-origin popup. Browser security blocks `window.opener` access, preventing postMessage communication.

**Steps to Reproduce**:
1. Deploy WASM app to test environment (`admin.chatmodtest.ru`)
2. Click "Login with Telegram"
3. Popup opens to production backend (`admin.chatmoderatorbot.ru`)
4. After auth, click "Open ChatKeep Admin"
5. Button does nothing

**Expected**: Same-origin popup allows `window.opener` access and postMessage works

**Actual**: Cross-origin popup blocks `window.opener`, code falls back to mobile deeplink behavior

**Fix Required**:
Parameterize OAuth URL in `DefaultAuthComponent.kt` to match deployment environment:
- Production: `https://admin.chatmoderatorbot.ru/auth/telegram-login`
- Test: `https://admin.chatmodtest.ru/auth/telegram-login` (if test backend exists)
OR
- Make both apps same-origin (deploy WASM and backend on same domain)

**Affected Files**:
- `chatkeep-admin/feature/auth/impl/src/commonMain/kotlin/com/chatkeep/admin/feature/auth/DefaultAuthComponent.kt` (line 102)

**Related Commit**: 29be77e (window.opener fix) - fix is correct but needs same-origin deployment

---

### Issue #2: No "[Chatkeep Auth]" Debug Logs - MEDIUM
**Severity**: MEDIUM
**Category**: Logging/Debugging

**Description**:
The postMessage code path includes console.log statements for debugging, but since that code path isn't executed (due to Issue #1), there are no debug logs. This makes it harder to diagnose auth issues in production.

**Recommendation**: Add debug logging to the environment detection logic and deeplink fallback path as well.

---

## Recommendation

**NEEDS FIXES** - Cannot release until Issue #1 is resolved.

### Required Actions:
1. **Parameterize OAuth URL**: Make auth endpoint URL environment-aware
2. **Deploy to test environment**: Ensure test backend at `admin.chatmodtest.ru` handles OAuth
3. **Verify same-origin**: Confirm WASM app and OAuth backend are same-origin
4. **Re-test**: Run this test scenario again after fixes

### Optional Improvements:
1. Add debug logging to environment detection
2. Add fallback error message when opener is blocked
3. Consider using OAuth redirect flow instead of popup (more reliable)

---

## Environment Details

**Browser**: Chrome 143.0.0.0
**OS**: macOS 10.15.7
**Main App**: https://admin.chatmodtest.ru/ (WASM)
**OAuth Backend**: https://admin.chatmoderatorbot.ru/ (Production)
**User Agent**: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36

---

## Screenshots

1. **ss_8813y7xko**: Initial login page
2. **ss_7826tm2z8**: Telegram OAuth screen
3. **ss_4461rwsae**: Authentication success screen
4. **ss_7285s50mb**: Button after click (no change)
5. **ss_6752tmqc8**: Main app still showing login (not authenticated)
