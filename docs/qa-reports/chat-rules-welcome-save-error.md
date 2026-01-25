# QA Test Report: Chat Rules & Welcome Messages Save Error

**Test Date**: 2026-01-24
**Environment**: https://chatmodtest.ru
**Tested By**: Manual QA Agent
**Test Type**: Bug Investigation

---

## Executive Summary

**Status**: CRITICAL ISSUE FOUND
**Severity**: HIGH
**Impact**: Users cannot save Chat Rules or Welcome Messages

Both Chat Rules and Welcome Messages save operations fail with 500 Internal Server Error. The backend endpoints are returning server errors when processing PUT requests.

---

## Test Execution

### Test 1: Chat Rules Save

**Page**: `/chat/-1003591184161/rules`

**Steps**:
1. Navigated to Chat Rules page
2. Entered test text in rules textarea:
   ```
   Test rules for QA investigation
   1. No spam
   2. Be respectful
   3. Stay on topic
   ```
3. Clicked Save button
4. Monitored network requests

**Network Request Details**:
- **URL**: `https://chatmodtest.ru/api/v1/miniapp/chats/-1003591184161/rules`
- **Method**: PUT
- **Status Code**: 500 (Internal Server Error)
- **Multiple Attempts**: 3 failed requests observed

**Expected**: Rules should be saved successfully with 200 OK response
**Actual**: Server returns 500 error, textarea resets to default example text

**Screenshots**:
- Before save: Text entered successfully
- After save: Text reset to default, no success message

**Console Errors**: None (error handling on backend, not frontend)

---

### Test 2: Welcome Messages Save

**Page**: `/chat/-1003591184161/welcome`

**Steps**:
1. Navigated to Welcome Messages page
2. Entered test text in welcome message textarea:
   ```
   Welcome to our chat! Please be respectful and follow the rules.
   ```
3. Clicked Save button
4. Monitored network requests

**Network Request Details**:
- **URL**: `https://chatmodtest.ru/api/v1/miniapp/chats/-1003591184161/welcome`
- **Method**: PUT
- **Status Code**: 500 (Internal Server Error)
- **Multiple Attempts**: 3 failed requests observed

**Expected**: Welcome message should be saved successfully with 200 OK response
**Actual**: Server returns 500 error

**GET Request (Page Load)**:
- **URL**: Same as above
- **Method**: GET
- **Status Code**: 200 OK
- **Note**: GET works fine, only PUT fails

---

## Backend Analysis

### Chat Rules Controller
**File**: `src/main/kotlin/ru/andvl/chatkeep/api/controller/MiniAppRulesController.kt`

**PUT Endpoint** (Lines 66-92):
```kotlin
@PutMapping
fun updateRules(
    @PathVariable chatId: Long,
    @Valid @RequestBody updateRequest: UpdateRulesRequest,
    request: HttpServletRequest
): RulesResponse {
    val user = getUserFromRequest(request)

    val isAdmin = runBlocking(Dispatchers.IO) {
        adminCacheService.isAdmin(user.id, chatId, forceRefresh = true)
    }
    if (!isAdmin) {
        throw AccessDeniedException("You are not an admin in this chat")
    }

    val saved = rulesService.setRules(chatId, updateRequest.rulesText)

    return RulesResponse(
        chatId = saved.chatId,
        rulesText = saved.rulesText
    )
}
```

**Potential Issues**:
1. `rulesService.setRules()` may be throwing an unhandled exception
2. Database operation may be failing
3. Missing error handling for service layer exceptions

---

### Welcome Messages Controller
**File**: `src/main/kotlin/ru/andvl/chatkeep/api/controller/MiniAppWelcomeController.kt`

**PUT Endpoint** (Lines 68-107):
```kotlin
@PutMapping
fun updateWelcomeSettings(
    @PathVariable chatId: Long,
    @Valid @RequestBody updateRequest: UpdateWelcomeRequest,
    request: HttpServletRequest
): WelcomeSettingsResponse {
    val user = getUserFromRequest(request)

    val isAdmin = runBlocking(Dispatchers.IO) {
        adminCacheService.isAdmin(user.id, chatId, forceRefresh = true)
    }
    if (!isAdmin) {
        throw AccessDeniedException("You are not an admin in this chat")
    }

    val existing = welcomeService.getWelcomeSettings(chatId)
        ?: WelcomeSettings(chatId = chatId)

    val updated = existing.copy(
        enabled = updateRequest.enabled ?: existing.enabled,
        messageText = updateRequest.messageText ?: existing.messageText,
        sendToChat = updateRequest.sendToChat ?: existing.sendToChat,
        deleteAfterSeconds = updateRequest.deleteAfterSeconds ?: existing.deleteAfterSeconds
    )

    val saved = welcomeService.updateWelcomeSettings(chatId, updated)

    return WelcomeSettingsResponse(
        chatId = saved.chatId,
        enabled = saved.enabled,
        messageText = saved.messageText,
        sendToChat = saved.sendToChat,
        deleteAfterSeconds = saved.deleteAfterSeconds
    )
}
```

**Potential Issues**:
1. `welcomeService.updateWelcomeSettings()` may be throwing an unhandled exception
2. Database operation may be failing
3. Request payload validation may be failing silently

---

## Common Patterns

Both endpoints share identical structure:
1. Authenticate user from request
2. Check admin permissions with `adminCacheService.isAdmin()`
3. Call service layer method (setRules / updateWelcomeSettings)
4. Return response

**Hypothesis**: The issue is likely in:
- Service layer implementation (RulesService / WelcomeService)
- Database operations (INSERT/UPDATE queries)
- Data validation or constraints

---

## Recommended Next Steps

### 1. Check Backend Logs
```bash
# Check application logs for stack traces
kubectl logs -n chatkeep deployment/chatkeep-backend --tail=100

# Or if using Docker
docker logs chatkeep-backend --tail=100
```

**Look for**:
- Stack traces around the time of failed requests
- SQL exceptions
- Constraint violations
- NullPointerException or validation errors

### 2. Inspect Service Layer
Need to examine:
- `RulesService.setRules()` implementation
- `WelcomeService.updateWelcomeSettings()` implementation
- Database repository methods

### 3. Check Database Schema
Verify:
- Table existence (chat_rules, welcome_settings)
- Column definitions and constraints
- Foreign key constraints
- Any database migration issues

### 4. Add Error Handling
Controllers should catch and log service exceptions:
```kotlin
try {
    val saved = rulesService.setRules(chatId, updateRequest.rulesText)
    return RulesResponse(...)
} catch (e: Exception) {
    log.error("Failed to save rules for chat $chatId", e)
    throw InternalServerException("Failed to save rules: ${e.message}")
}
```

---

## Test Summary

**Total Tests**: 2
**Passed**: 0
**Failed**: 2

**Issues Found**:

1. **Chat Rules Save - 500 Error** (Severity: HIGH)
   - Users cannot save chat rules
   - Backend returns 500 on PUT /api/v1/miniapp/chats/{chatId}/rules
   - Multiple retry attempts all fail

2. **Welcome Messages Save - 500 Error** (Severity: HIGH)
   - Users cannot save welcome messages
   - Backend returns 500 on PUT /api/v1/miniapp/chats/{chatId}/welcome
   - Multiple retry attempts all fail

---

## Recommendation

**BLOCK RELEASE** - These are critical features that are completely non-functional. Users cannot configure chat rules or welcome messages, which are core moderation features.

**Priority**: CRITICAL - Requires immediate backend investigation and fix.

**Next Action**: Backend developer should:
1. Review backend logs for stack traces
2. Examine service layer implementations
3. Verify database schema and migrations
4. Add proper error handling and logging
5. Test with real payloads to reproduce the issue locally
