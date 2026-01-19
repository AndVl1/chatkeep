# OAuth Flow Test Report - admin.chatmodtest.ru

**Test Date**: 2026-01-20
**Tester**: Manual QA Agent
**Test URL**: https://admin.chatmodtest.ru/
**Environment**: TEST (chatmodtest)
**Status**: FAILED - CRITICAL CROSS-ORIGIN BUG DETECTED

---

## Executive Summary

The OAuth flow on admin.chatmodtest.ru is **BROKEN** due to a cross-origin issue. The popup window opens on the PRODUCTION domain (`admin.chatmoderatorbot.ru`) instead of the TEST domain (`admin.chatmodtest.ru`), causing `window.opener` to be blocked by browser security policies.

**Result**: Authentication will FAIL because the popup cannot communicate with the main app.

---

## Test Steps Executed

### Step 1: Initial Page Load
- **Action**: Navigate to https://admin.chatmodtest.ru/
- **Result**: PASS
- **Screenshot**: Login page loaded successfully
- **Observations**:
  - Page displays "ChatKeep Admin" header
  - "Login with Telegram" button visible
  - No console errors

### Step 2: Click Login Button
- **Action**: Click "Login with Telegram" button
- **Result**: FAIL - Wrong domain in popup
- **Screenshot**: Telegram login page opened
- **Observations**:
  - Popup opened successfully
  - **CRITICAL**: Popup URL is `https://admin.chatmoderatorbot.ru/auth/telegram-login?state=...`
  - **EXPECTED**: `https://admin.chatmodtest.ru/auth/telegram-login?state=...`
  - Domain mismatch detected

### Step 3: Window.opener Check
- **Action**: Inspect `window.opener` in popup console
- **Result**: FAIL - Cross-origin blocked
- **JavaScript Execution**:
  ```javascript
  {
    "popupUrl": "https://admin.chatmoderatorbot.ru/auth/telegram-login?state=Ld1u8zwy5rePz98PTP5jv0Q8g7xWsUth",
    "popupOrigin": "https://admin.chatmoderatorbot.ru",
    "hasOpener": false,
    "openerAccessible": null
  }
  ```
- **Observations**:
  - `hasOpener: false` - window.opener is NULL
  - Main app origin: `https://admin.chatmodtest.ru`
  - Popup origin: `https://admin.chatmoderatorbot.ru`
  - **Cross-origin security blocks communication**

---

## Test Results Summary

| Test | Expected | Actual | Status |
|------|----------|--------|--------|
| Page Load | admin.chatmodtest.ru loads | Loaded successfully | ✅ PASS |
| Login Button Click | Popup opens | Popup opened | ✅ PASS |
| **Popup URL Domain** | **admin.chatmodtest.ru** | **admin.chatmoderatorbot.ru** | ❌ **FAIL** |
| **window.opener** | **truthy (accessible)** | **false (blocked)** | ❌ **FAIL** |
| PostMessage Flow | Not tested | Cannot test - opener blocked | ⚠️ BLOCKED |
| Popup Auto-Close | Not tested | Cannot test - flow broken | ⚠️ BLOCKED |
| Main App Receives Auth | Not tested | Cannot test - flow broken | ⚠️ BLOCKED |

**Total Tests**: 6
**Passed**: 2
**Failed**: 2
**Blocked**: 2

---

## Root Cause Analysis

### Problem
The deployed WASM app on https://admin.chatmodtest.ru/ is using PRODUCTION backend URL instead of TEST backend URL.

### Investigation
1. Checked current branch: `fix/test-env-bot-nickname-parameterization`
2. Current branch HAS the fix (commit `b007360`)
3. Deployed code runs from `main` branch
4. Checked `main` branch file: `chatkeep-admin/core/common/src/wasmJsMain/kotlin/com/chatkeep/admin/core/common/BuildConfig.wasmJs.kt`

### File Comparison

**Current Branch** (fix/test-env-bot-nickname-parameterization):
```kotlin
actual object BuildConfig {
    actual val isDebug: Boolean = false

    actual val authBackendUrl: String
        get() {
            val hostname = getHostname()
            return when {
                hostname.contains("localhost") -> "http://localhost:8080"
                hostname.contains("chatmodtest") -> "https://admin.chatmodtest.ru"
                else -> "https://admin.chatmoderatorbot.ru"  // production
            }
        }
}
```

**Main Branch** (deployed):
```kotlin
actual object BuildConfig {
    actual val isDebug: Boolean = false
    // authBackendUrl NOT DEFINED - falls back to other platform defaults
}
```

### Conclusion
The `main` branch is missing the `authBackendUrl` property for WASM. The fix exists on the current branch but hasn't been merged to `main` yet.

---

## Impact Assessment

### Severity: CRITICAL

**Why Critical:**
1. **Complete OAuth Flow Failure**: Users cannot authenticate on TEST environment
2. **Cross-Origin Security Violation**: Browser blocks window.opener access
3. **Silent Failure**: No error messages shown to user
4. **Production Contamination**: TEST environment calls PRODUCTION backend

**User Impact:**
- TEST environment is completely unusable for authentication
- Cannot test new features before production deployment
- Risk of mixing TEST and PRODUCTION data

**Security Impact:**
- CSRF state token validation will fail (different origins)
- Cannot verify OAuth callback authenticity
- Potential for session fixation attacks

---

## Recommended Actions

### Immediate Fix (Required for TEST to work)

**Option 1: Merge Current Branch to Main**
```bash
# Current branch has the fix
git checkout main
git merge fix/test-env-bot-nickname-parameterization
git push origin main
```

**Option 2: Cherry-pick the Fix Commit**
```bash
git checkout main
git cherry-pick b007360  # "fix: parameterize auth backend URL"
git push origin main
```

### Verification Steps After Deployment

1. Clear browser cache completely
2. Navigate to https://admin.chatmodtest.ru/
3. Click "Login with Telegram"
4. **Verify** popup URL is `https://admin.chatmodtest.ru/auth/telegram-login?state=...`
5. Check console: `window.opener` should be truthy
6. Complete Telegram auth
7. Click "Open Chatkeep Admin" button
8. **Verify** popup closes automatically
9. **Verify** main app receives auth data

### Long-term Recommendations

1. **Automated Testing**: Add E2E tests for OAuth flow
2. **Environment Detection Tests**: Verify BuildConfig returns correct URLs per environment
3. **Deployment Checklist**: Include OAuth flow testing before marking deployment successful
4. **Branch Protection**: Prevent merging to `main` without required fixes
5. **Staging Environment**: Test all changes on staging before production

---

## Related Commits

- `b007360` - fix: parameterize auth backend URL for environment-aware deployment
- `29be77e` - fix: ensure window.opener access for WASM auth popup
- `ba58ee6` - fix: add BOT_USERNAME validation and improve expression clarity
- `e140f97` - fix: parameterize Mini App bot username based on deployment environment

**Current branch**: `fix/test-env-bot-nickname-parameterization`
**Deployed branch**: `main` (missing the fix)

---

## Appendix: Screenshots

### 1. Main App - Login Page
- URL: https://admin.chatmodtest.ru/
- Status: Loaded successfully
- No errors in console

### 2. OAuth Popup - WRONG DOMAIN
- **URL**: https://admin.chatmoderatorbot.ru/auth/telegram-login?state=...
- **Expected**: https://admin.chatmodtest.ru/auth/telegram-login?state=...
- Status: Cross-origin issue detected
- `window.opener`: null (blocked)

### 3. JavaScript Console Output
```json
{
  "popupUrl": "https://admin.chatmoderatorbot.ru/auth/telegram-login?state=Ld1u8zwy5rePz98PTP5jv0Q8g7xWsUth",
  "popupOrigin": "https://admin.chatmoderatorbot.ru",
  "hasOpener": false,
  "openerAccessible": null
}
```

---

## Conclusion

**The OAuth flow on admin.chatmodtest.ru is currently BROKEN and requires immediate fix.**

The solution exists on the current branch (`fix/test-env-bot-nickname-parameterization`) but needs to be merged to `main` and redeployed for the TEST environment to function properly.

**Recommendation**: MERGE TO MAIN AND REDEPLOY IMMEDIATELY

---

**Test completed by**: Manual QA Agent
**Report generated**: 2026-01-20
**Next action**: Merge fix branch to main and trigger deployment
