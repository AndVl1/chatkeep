# Test Session Report - Welcome Message & Chat Rules Save Functionality

**Feature Tested**: Save functionality for Welcome Message and Chat Rules pages
**Platform**: Web (Chrome)
**Environment**: https://chatmodtest.ru
**Date**: 2026-01-24
**Tester**: manual-qa agent

---

## Executive Summary

**Status**: FAILED - Both Welcome Message and Chat Rules save operations still return 500 errors.

**Critical Findings**:
- Welcome Message PUT request: HTTP 500 Internal Server Error
- Chat Rules PUT request: HTTP 500 Internal Server Error
- GET requests for Chat Rules return 404 (no data exists)
- Backend deployment did NOT resolve the save issues

---

## Tests Executed

### Test 1: Welcome Message Save

**Status**: FAIL

**Steps**:
1. Navigated to https://chatmodtest.ru/chat/-1003591184161/welcome
2. Verified page loaded successfully
3. Enabled welcome message toggle (already ON)
4. Entered welcome message text: "Welcome to our chat! We're glad to have you here."
5. Clicked Save button
6. Monitored network request

**API Call**:
- **Endpoint**: `PUT /api/v1/miniapp/chats/-1003591184161/welcome`
- **Status Code**: **500 Internal Server Error**
- **Expected**: 200 OK or 201 Created
- **Actual**: 500 error

**Screenshots**:
- Welcome Message form with text entered: ✓
- Save button clicked: ✓

**Issues**:
- Save operation failed with 500 error
- No user-facing error message displayed (UI issue)
- Data does NOT persist (verified via backend 500 response)

---

### Test 2: Chat Rules Save

**Status**: FAIL

**Steps**:
1. Navigated to https://chatmodtest.ru/chat/-1003591184161/rules
2. Verified page loaded successfully
3. Entered chat rules text:
   ```
   1. Be respectful to all members
   2. No spam or advertising
   3. Stay on topic
   4. No harassment or bullying
   ```
4. Clicked Save button
5. Monitored network request

**API Call**:
- **Endpoint**: `PUT /api/v1/miniapp/chats/-1003591184161/rules`
- **Status Code**: **500 Internal Server Error**
- **Expected**: 200 OK or 201 Created
- **Actual**: 500 error

**Screenshots**:
- Chat Rules form with text entered: ✓
- Save button clicked: ✓

**Issues**:
- Save operation failed with 500 error
- No user-facing error message displayed (UI issue)
- Data does NOT persist

---

### Test 3: Data Persistence Verification

**Status**: FAIL (as expected due to save failures)

**Steps**:
1. After attempting to save Chat Rules, refreshed the page
2. Checked if rules text persisted
3. Monitored GET request on page reload

**API Call**:
- **Endpoint**: `GET /api/v1/miniapp/chats/-1003591184161/rules`
- **Status Code**: **404 Not Found**
- **Expected**: 200 OK with saved data (if save had succeeded)
- **Actual**: 404 (no data exists on backend)

**Observations**:
- Rules text appeared to persist in UI after refresh (client-side caching)
- Backend confirms no data was saved (404 response)
- This indicates frontend is caching unsaved changes

---

## Network Request Summary

### Welcome Message Endpoint
| Method | Endpoint | Status Code | Result |
|--------|----------|-------------|--------|
| GET | `/api/v1/miniapp/chats/-1003591184161/welcome` | 200 | Success (fetch) |
| PUT | `/api/v1/miniapp/chats/-1003591184161/welcome` | **500** | **FAILURE** |

### Chat Rules Endpoint
| Method | Endpoint | Status Code | Result |
|--------|----------|-------------|--------|
| GET | `/api/v1/miniapp/chats/-1003591184161/rules` | 404 | No data exists |
| PUT | `/api/v1/miniapp/chats/-1003591184161/rules` | **500** | **FAILURE** |

### Other API Calls (Context)
| Method | Endpoint | Status Code | Result |
|--------|----------|-------------|--------|
| GET | `/api/v1/miniapp/chats` | 200 | Success (chat list) |
| GET | `/api/v1/miniapp/chats/-1003591184161/settings` | 200 | Success (settings) |

---

## Issues Found

### 1. Welcome Message Save Failure - CRITICAL
**Severity**: CRITICAL
**Location**: `PUT /api/v1/miniapp/chats/-1003591184161/welcome`

**Description**:
Backend returns HTTP 500 error when attempting to save welcome message settings. This prevents users from configuring welcome messages for their chats.

**Expected**: 200 OK with saved WelcomeMessage object
**Actual**: 500 Internal Server Error

**Backend Investigation Needed**:
- Check server logs for exception stack trace
- Verify WelcomeMessage entity/table exists
- Verify WelcomeMessageRepository implementation
- Check if Unit return type issue was actually fixed and deployed

---

### 2. Chat Rules Save Failure - CRITICAL
**Severity**: CRITICAL
**Location**: `PUT /api/v1/miniapp/chats/-1003591184161/rules`

**Description**:
Backend returns HTTP 500 error when attempting to save chat rules. This prevents users from setting rules for their chats.

**Expected**: 200 OK with saved ChatRules object
**Actual**: 500 Internal Server Error

**Backend Investigation Needed**:
- Check server logs for exception stack trace
- Verify ChatRules entity/table exists
- Verify ChatRulesRepository implementation
- Check if the fix was actually deployed

---

### 3. Missing Error Feedback in UI - HIGH
**Severity**: HIGH
**Location**: Frontend - WelcomeMessagePage.tsx, ChatRulesPage.tsx

**Description**:
When save operations fail with 500 errors, the UI does not display any error message to the user. The Save button completes without feedback, leaving users unaware that their changes were not saved.

**Expected**: Toast notification or inline error message
**Actual**: Silent failure (no user feedback)

**Suggested Fix**:
- Add error toast notification when PUT request fails
- Display error message above Save button
- Disable Save button and show error state
- Example: "Failed to save changes. Please try again."

---

### 4. Client-Side Caching of Unsaved Data - MEDIUM
**Severity**: MEDIUM
**Location**: Frontend - Chat Rules page state management

**Description**:
After attempting to save Chat Rules and refreshing the page, the entered text still appears in the form even though the backend confirms no data was saved (404 response). This creates a false impression that data was saved.

**Root Cause**: Likely React state persistence or localStorage caching unsaved form data.

**Expected**: Form should be empty after refresh if save failed
**Actual**: Form retains unsaved data, misleading user

**Suggested Fix**:
- Clear cached form data on successful save only
- Show indicator that data is "unsaved" if cached
- Consider removing client-side persistence for failed saves

---

## Deployment Verification

**Question**: Was the backend actually redeployed with the Unit return type fix?

**Evidence suggesting NO**:
- Both endpoints still return 500 errors
- Same error pattern as previous test sessions
- No change in behavior after reported deployment

**Recommendation**:
1. Verify backend deployment was successful
2. Check which version/branch is currently deployed
3. Verify the fix exists in the deployed code
4. Check server logs for current errors
5. Consider checking database schema (tables may not exist)

---

## Browser Console Errors

**Note**: Console message tracking was started mid-session, so early errors may not be captured.

**Captured Console Messages**: None with pattern "error|Error|fail|500"

**Recommendation**: Check browser DevTools Network tab response body for detailed error messages from backend.

---

## Test Environment Details

- **URL**: https://chatmodtest.ru
- **Chat ID**: -1003591184161
- **Browser**: Chrome (via MCP tools)
- **Authentication**: Telegram Mini App initData (valid)
- **Network Conditions**: Normal (no throttling)

---

## Recommendation

**STATUS**: NEEDS FIXES - NOT READY FOR RELEASE

**Required Actions**:

1. **BACKEND (Priority: CRITICAL)**:
   - Verify deployment of fix/auth-and-feature-parity branch
   - Check server logs for actual error details
   - Verify database schema includes required tables:
     - `welcome_messages` or equivalent
     - `chat_rules` or equivalent
   - Test PUT endpoints directly (curl/Postman)
   - Fix actual backend errors causing 500 responses

2. **FRONTEND (Priority: HIGH)**:
   - Add error handling and user feedback for failed saves
   - Show toast notification on error
   - Clear client-side cache on failed saves or indicate "unsaved"

3. **QA (Next Steps)**:
   - Retest after backend fix is verified deployed
   - Verify server logs show no errors
   - Test data persistence with page refresh
   - Test error states (network errors, validation errors)

---

## Additional Notes

- Network tab shows multiple failed PUT attempts (3x for rules, 1x for welcome), suggesting possible retry logic or multiple save button clicks in previous sessions
- GET requests for settings endpoint work fine (200 OK), indicating authentication and basic API functionality is working
- The issue is specifically with PUT operations for welcome and rules endpoints
- 404 responses for GET /rules indicate either:
  - No data has ever been saved for this chat
  - Table doesn't exist (would cause 500 on GET too, so likely former)

---

## Test Artifacts

- Screenshots captured: 5
- Network requests monitored: 16+ API calls
- Test duration: ~5 minutes
- Test coverage: Welcome Message save, Chat Rules save, data persistence

---

**Conclusion**: The reported backend deployment did NOT fix the save functionality issues. Both Welcome Message and Chat Rules save operations continue to fail with 500 errors. Investigation of backend deployment status and server logs is required.
