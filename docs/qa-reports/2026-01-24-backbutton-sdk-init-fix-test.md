# BackButton SDK Init Fix Test Report

**Date**: 2026-01-24
**Feature Tested**: BackButton SDK initialization fix
**Platform**: Web (Telegram Mini App via web.telegram.org)
**Environment**: https://chatmodtest.ru/ (AdminTestBot)
**Tester**: manual-qa agent

---

## Test Objective

Verify that the BackButton SDK initialization fix resolves the issue where `mount.isAvailable` and `show.isAvailable` were showing `false` on all pages.

**Expected Behavior**:
- `mount.isAvailable: true` (after SDK init)
- `show.isAvailable: true` (after SDK init)
- Native Telegram back arrow (←) appears in Mini App header on settings page

---

## Test Execution

### Test 1: Home Page Debug Bar Check

**Steps**:
1. Opened Telegram Web (web.telegram.org/a/?account=2)
2. Navigated to AdminTestBot
3. Opened Mini App via app menu (☰ button)
4. Inspected debug bar at bottom of Mini App

**Result**: FAIL ❌

**Debug Bar Values (Home Page)**:
```
isMiniApp: true | isHomePage: true | path: / | mount.isAvailable: false | show.isAvailable: false | isMounted: false | isVisible: false
```

**Issues Found**:
- `mount.isAvailable: false` (expected `true`)
- `show.isAvailable: false` (expected `true`)

**Screenshot**: ss_8983nsnne (Home page with debug bar)

---

### Test 2: Settings Page Debug Bar Check

**Steps**:
1. From home page, clicked on "ChatBot Test" chat
2. Navigated to settings page
3. Inspected debug bar at bottom
4. Checked for native Telegram back arrow in header

**Result**: FAIL ❌

**Debug Bar Values (Settings Page)**:
```
isMiniApp: true | isHomePage: false | path: /chat/-1003591184161/settings | mount.isAvailable: false | show.isAvailable: false | isMounted: false | isVisible: false
```

**Issues Found**:
- `mount.isAvailable: false` (expected `true`)
- `show.isAvailable: false` (expected `true`)
- `isMounted: false`
- `isVisible: false`
- **No native Telegram back arrow** in Mini App header

**Observations**:
- Custom "← Back" button is visible in the app's UI (purple color)
- Native back arrow from Telegram is **absent** from the header

**Screenshot**: ss_4791s7umg (Settings page with debug bar)

---

## Summary

**Total Tests**: 2
**Passed**: 0
**Failed**: 2

---

## Issues Found

### Issue #1: BackButton SDK Not Available

**Severity**: HIGH

**Description**:
The BackButton SDK is still reporting `mount.isAvailable: false` and `show.isAvailable: false` on both home page and settings page. This indicates that the SDK initialization fix did **not work**.

**Expected**:
- `mount.isAvailable: true`
- `show.isAvailable: true`

**Actual**:
- `mount.isAvailable: false`
- `show.isAvailable: false`

**Root Cause** (Hypothesis):
The SDK initialization timing may still be incorrect, or the Telegram WebApp API is not recognizing the BackButton component as available in this environment.

**Impact**:
- Native Telegram back arrow does not appear
- BackButton SDK features cannot be used
- Users must rely on custom in-app back button

---

### Issue #2: Native Back Arrow Missing

**Severity**: MEDIUM

**Description**:
When navigating to the settings page, the native Telegram back arrow (←) that should appear in the Mini App header is **missing**.

**Expected**:
- Native back arrow in Telegram header (similar to BotFather)
- Controlled by BackButton.mount() and BackButton.show()

**Actual**:
- No native back arrow in header
- Only custom "← Back" button in app UI

**Workaround**:
The custom "← Back" button in the app UI is functional, but this is not the native Telegram UX.

---

## Recommendation

**Status**: NEEDS INVESTIGATION & FIX

The BackButton SDK initialization fix **did not resolve the issue**. Further investigation is required to:

1. **Review SDK initialization code** - Check if BackButton is being initialized at the correct time in the component lifecycle
2. **Verify Telegram WebApp API version** - Ensure the environment supports BackButton API
3. **Check console errors** - Look for any SDK initialization errors in browser console
4. **Compare with working implementation** - Test against BotFather or other known-working Mini Apps
5. **Consider alternative approach** - May need to use a different initialization pattern or check for platform-specific limitations

---

## Next Steps

1. Investigate why `mount.isAvailable` remains `false` despite initialization
2. Check browser console for SDK errors during initialization
3. Review @telegram-apps/sdk-react BackButton initialization docs
4. Test in different Telegram clients (iOS/Android) to rule out web-specific issues
5. Consider filing an issue with @telegram-apps/sdk if this is a library bug

---

## Attachments

- Screenshot (Home page): ss_8983nsnne
- Screenshot (Settings page): ss_4791s7umg
- Screenshot (Header zoom): ss_4791s7umg (cropped)
