# QA Test Report: Twitch Toggle on Android (BLOCKED)

**Date**: 2026-01-25
**Platform**: Android (Physical Device - A063)
**Environment**: api.chatmodtest.ru (test environment)
**Tester**: manual-qa agent
**Status**: BLOCKED - Unable to complete authentication

---

## Executive Summary

Test was blocked at the authentication stage. The Telegram Login Widget button in the Chrome Custom Tab is non-responsive, preventing login to the ChatKeep Admin app. Cannot proceed to test Twitch toggle functionality.

---

## Test Attempted

**Feature**: Twitch integration toggle
**Test Scope**:
1. Launch ChatKeep Admin app on Android
2. Complete Telegram authentication
3. Navigate to chat settings
4. Test Twitch toggle functionality
5. Verify toggle responds to taps and state changes persist

---

## Steps Executed

### 1. App Launch
**Status**: PASS
- Launched com.chatkeep.admin successfully
- App displayed login screen correctly
- "Login with Telegram" button visible

**Screenshot Evidence**: Login screen displayed correctly

### 2. Authentication Attempt
**Status**: BLOCKED

**Steps taken**:
1. Tapped "Login with Telegram" button in native app
2. Chrome Custom Tab opened successfully
3. Navigated to admin.chatmodtest.ru
4. Telegram Login Widget loaded
5. Widget displayed "Log in as Андрей" button with user profile picture
6. **BLOCKER**: Button completely non-responsive to taps

**Tap attempts made**:
- UI index tap (index 7)
- Coordinate-based tap (313, 970)
- Coordinate-based tap (540, 1393)
- Telegram icon tap (189, 970)
- ADB shell input tap command
- Multiple retry attempts

**None of the tap methods successfully triggered the login flow.**

### 3. Investigation

**Browser Context**:
- Current Activity: `com.android.chrome/org.chromium.chrome.browser.ChromeTabbedActivity`
- URL: `admin.chatmodtest.ru/a`
- Chrome Custom Tabs (not in-app WebView)

**Logs Checked**:
- No JavaScript errors visible in filtered logs
- No authentication-related errors in app logs
- No crash reports

**Possible Root Causes**:
1. Telegram Login Widget JavaScript not loading properly
2. CORS or security policy blocking widget interaction
3. Test environment configuration issue with Telegram bot credentials
4. Chrome Custom Tab security restrictions preventing deep link activation
5. Network issue preventing widget script from loading

---

## Blocker Details

**Severity**: CRITICAL
**Component**: Authentication
**Blocks**: All feature testing (including Twitch toggle)

**Issue Description**:
The Telegram Login Widget button rendered in the Chrome Custom Tab does not respond to any tap/click events. The button appears visually correct (shows user name "Андрей" and profile picture), indicating the widget loaded successfully, but the click handler does not fire.

**Impact**:
- Cannot authenticate to app
- Cannot access any authenticated features
- Cannot test Twitch toggle or any other settings
- Manual QA testing completely blocked

---

## Recommendations

### Immediate Actions Needed

1. **Verify Test Environment**:
   - Check if admin.chatmodtest.ru Telegram OAuth configuration is correct
   - Verify bot token and allowed domains
   - Test the login widget in desktop browser to isolate Android-specific issues

2. **Alternative Authentication**:
   - Implement development-mode bypass (e.g., mock auth token injection)
   - Add deep link authentication method as fallback
   - Consider native Telegram app OAuth flow instead of web widget

3. **Debugging**:
   - Enable Chrome DevTools remote debugging
   - Inspect console errors in the Custom Tab
   - Check if widget iframe is blocked by security policies

4. **Workaround for QA**:
   - Provide pre-authenticated test build
   - Add debug authentication endpoint
   - Use emulator with root access to inject auth tokens manually

### Future Testing

Once authentication blocker is resolved:
- Re-run Twitch toggle test
- Verify all toggle interactions
- Test persistence of toggle state
- Check network requests for PUT /settings
- Validate error handling

---

## Environment Details

**Device**: Physical Android device (A063)
**Device ID**: P11262002127
**App**: com.chatkeep.admin
**Backend**: api.chatmodtest.ru
**Browser**: Chrome (Custom Tabs)

**Network**: WiFi connected
**Time**: 02:17 - 02:22 (UTC+6)

---

## Next Steps

1. Development team to investigate Telegram Login Widget issue
2. Provide alternative authentication method for QA testing
3. Re-test once blocker is resolved
4. Document authentication flow testing in separate test case

---

## Attachments

Screenshots captured:
1. App login screen (native)
2. Chrome Custom Tab with Telegram widget
3. Unresponsive login button

Logs available in test session artifacts.

---

**Test Result**: BLOCKED
**Can Resume After**: Authentication blocker resolved
**Priority**: HIGH - Blocks all mobile app QA testing
