# OAuth Flow Test Procedure - Production Environment
**Date**: 2026-01-20
**Environment**: https://admin.chatmodtest.ru/
**Purpose**: Verify OAuth popup URL fix with cache disabled

## Critical Setup Steps

### 1. Initial Setup
1. Open Chrome browser
2. Navigate to: https://admin.chatmodtest.ru/
3. Open Chrome DevTools:
   - Press F12 (Windows/Linux)
   - Press Cmd+Option+I (macOS)
4. Click on "Network" tab in DevTools

### 2. Enable Cache Disable (CRITICAL!)
1. In the Network tab, locate the checkbox that says "Disable cache"
2. **CHECK this checkbox** - this is the most critical step
3. Keep DevTools open during the entire test session

### 3. Hard Refresh
1. With DevTools still open and "Disable cache" checked
2. Perform a hard refresh:
   - Windows/Linux: Ctrl+Shift+R or Ctrl+F5
   - macOS: Cmd+Shift+R
3. Watch the Network tab to verify composeApp.js loads fresh
4. Verify status code is 200 (not 304 "Not Modified")

## OAuth Flow Test Steps

### Test 1: Popup URL Verification

**Steps**:
1. With DevTools and "Disable cache" still active
2. Click the "Login with Telegram" button on the page
3. A popup window should open

**Verification**:
1. Look at the URL bar of the popup window
2. The URL MUST be: `https://admin.chatmodtest.ru/auth/telegram-login?state=...`
3. Specifically verify:
   - Domain is: `admin.chatmodtest.ru`
   - NOT: `oauth.telegram.org` or any other domain
   - Path starts with: `/auth/telegram-login`
   - Query param exists: `?state=<some-value>`

**Expected Result**: Popup URL domain matches the main app domain (admin.chatmodtest.ru)

**Screenshot Required**: Take a screenshot showing the popup URL bar

---

### Test 2: Console Messages

**Steps**:
1. In the popup window, open DevTools (F12 or Cmd+Option+I)
2. Go to Console tab in the popup's DevTools
3. Complete the Telegram authentication in the popup
4. Click "Open Chatkeep Admin" button in the popup

**Verification**:
1. Watch the Console tab for messages starting with "[Chatkeep Auth]"
2. You should see:
   ```
   [Chatkeep Auth] Sending auth data via postMessage...
   [Chatkeep Auth] Auth data: {user: {...}, hash: "...", ...}
   ```

**Expected Result**: Console shows postMessage being sent

**Screenshot Required**: Take a screenshot of the console messages

---

### Test 3: hasOpener Check

**Steps**:
1. While the popup is still open (before clicking the button)
2. In the popup's DevTools Console tab
3. Type and execute: `window.opener`

**Verification**:
1. The result should be a Window object (not null)
2. This confirms the popup has access to the parent window

**Expected Result**: `window.opener` returns a Window object (truthy value)

**Screenshot Required**: Take a screenshot showing the window.opener check result

---

### Test 4: Popup Auto-Close

**Steps**:
1. Continue from Test 2 - after clicking "Open Chatkeep Admin" button in popup
2. Watch the popup window

**Verification**:
1. Popup should close automatically after postMessage is sent
2. Main app window should receive the authentication data

**Expected Result**: Popup closes without manual intervention

---

### Test 5: Main App Authentication

**Steps**:
1. Return focus to the main app window (https://admin.chatmodtest.ru/)
2. Open DevTools Console tab in the main window
3. Check the console for auth-related messages

**Verification**:
1. Console should show successful authentication
2. No error messages about "window.opener" or "postMessage"
3. App should show authenticated state (user logged in)

**Expected Result**: Main app successfully receives and processes auth data

**Screenshot Required**: Take a screenshot of the authenticated app state

---

## Success Criteria Checklist

Use this checklist to verify the fix is working:

- [ ] Cache was disabled in DevTools Network tab during entire test
- [ ] composeApp.js loaded with 200 status (verified in Network tab)
- [ ] Popup URL domain is `admin.chatmodtest.ru`
- [ ] Popup URL path is `/auth/telegram-login?state=...`
- [ ] `window.opener` is truthy (not null) in popup
- [ ] Console shows "[Chatkeep Auth] Sending auth data via postMessage..."
- [ ] Popup closes automatically after auth
- [ ] Main app receives authentication successfully
- [ ] No console errors related to cross-origin or postMessage
- [ ] User can access authenticated features

## Common Issues and Troubleshooting

### Issue: Cache not disabled
**Symptom**: composeApp.js shows 304 status or loads from cache
**Solution**:
1. Ensure "Disable cache" checkbox is checked
2. Close and reopen DevTools
3. Hard refresh again (Cmd+Shift+R)

### Issue: Popup URL is oauth.telegram.org
**Symptom**: Popup opens to Telegram's OAuth domain instead of app domain
**Solution**: This indicates the fix is NOT working. The app is still using the old OAuth flow.

### Issue: window.opener is null
**Symptom**: Popup cannot access parent window
**Solution**: This is the original bug - popup was opened from a different domain

### Issue: Popup doesn't close
**Symptom**: Popup stays open after authentication
**Solution**: Check console for errors. Likely postMessage failed.

### Issue: Authentication fails in main app
**Symptom**: Main app shows login screen after popup closes
**Solution**: Check main app console for errors. Verify postMessage event listener is working.

## Expected vs. Actual Results

### If Fix is Working (EXPECTED):
- Popup domain: admin.chatmodtest.ru ✓
- hasOpener: true ✓
- postMessage: works ✓
- Popup auto-closes: yes ✓
- Auth succeeds: yes ✓

### If Fix is NOT Working (OLD BEHAVIOR):
- Popup domain: oauth.telegram.org ✗
- hasOpener: false ✗
- postMessage: blocked ✗
- Popup stays open: manual close needed ✗
- Auth fails: yes ✗

## Report Template

After completing all tests, fill in this report:

```
## OAuth Test Report - 2026-01-20

**Environment**: https://admin.chatmodtest.ru/
**Tester**: [Your Name]
**Date/Time**: [Date and time of test]
**Browser**: Chrome [version]

### Setup Verification
- [ ] DevTools opened: YES/NO
- [ ] "Disable cache" checked: YES/NO
- [ ] Hard refresh performed: YES/NO
- [ ] composeApp.js loaded fresh (200 status): YES/NO

### Test Results

**Test 1 - Popup URL**:
- Status: PASS / FAIL
- Popup domain: [actual domain]
- Screenshot: [reference]

**Test 2 - Console Messages**:
- Status: PASS / FAIL
- Messages observed: [list]
- Screenshot: [reference]

**Test 3 - hasOpener**:
- Status: PASS / FAIL
- window.opener value: [Window object / null]
- Screenshot: [reference]

**Test 4 - Popup Auto-Close**:
- Status: PASS / FAIL
- Closed automatically: YES / NO

**Test 5 - Main App Auth**:
- Status: PASS / FAIL
- Auth successful: YES / NO
- Screenshot: [reference]

### Overall Result
- **All Tests Passed**: YES / NO
- **Fix is Working**: YES / NO
- **Ready for Production**: YES / NO

### Issues Found
[List any issues discovered]

### Notes
[Any additional observations]
```

## Next Steps

### If All Tests Pass:
1. Document success in test report
2. Confirm fix is ready for production deployment
3. Consider merging fix branch to main

### If Tests Fail:
1. Document exact failure point
2. Include all screenshots and console logs
3. Share findings with development team
4. Review recent commits for potential issues
