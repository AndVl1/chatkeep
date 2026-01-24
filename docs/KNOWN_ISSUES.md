# Known Issues & Bug Fixes

This document tracks significant bugs, their root causes, and fixes. Use this as a reference when:
- Investigating similar issues
- Writing new features to avoid repeating mistakes
- Understanding why certain code patterns exist

---

## 2024-01-24: Test assertions for config change logging

**Issue**: CI tests failing in `MiniAppSettingsControllerTest` and `MiniAppLocksControllerTest`

**Symptoms**:
- `PUT settings - updates multiple settings at once` - AssertionError: expected log to contain `thresholdAction: MUTE`
- `PUT locks - disables lock warns and logs change` - AssertionError: log entry was null

**Root Cause**:
The controller only logs config changes when the value actually changes (old != new). Tests were setting up initial configs with default values that matched what the test was sending, so no change was detected and no log was written.

1. `ModerationConfig` has `thresholdAction = "MUTE"` as default. Test sent `thresholdAction: "MUTE"` - no change.
2. `LockSettings` has `lockWarns = false` as default. Test tried to disable it without enabling first - no change.

**Fix**:
- Set initial config values different from what the test will send
- For Settings test: `thresholdAction = BAN`, `defaultBlocklistAction = BAN` so changing to MUTE/WARN triggers logging
- For Locks test: First save `LockSettings.createNew(chatId, lockWarns = true)` before disabling

**Files Changed**:
- `src/test/kotlin/ru/andvl/chatkeep/api/MiniAppSettingsControllerTest.kt`
- `src/test/kotlin/ru/andvl/chatkeep/api/MiniAppLocksControllerTest.kt`

**Lesson Learned**:
When testing logging/auditing of changes, always ensure the initial state differs from the final state. Don't rely on default values matching test expectations.

---

## 2024-01-24: 500 errors on save for Rules/Welcome/Antiflood pages

**Issue**: Mini App returned HTTP 500 when saving Rules, Welcome, or Antiflood settings

**Symptoms**:
- PUT /api/v1/miniapp/chats/{chatId}/rules returned 500
- PUT /api/v1/miniapp/chats/{chatId}/welcome returned 500
- Logs showed: `JdbcSQLIntegrityConstraintViolationException: NULL not allowed for column "CHAT_ID"`

**Root Cause**:
Domain models (`Rules`, `WelcomeSettings`, `AntifloodSettings`, `Note`) were missing Spring Data JDBC annotations and `Persistable` interface. Without `Persistable`, Spring Data JDBC couldn't determine whether to INSERT or UPDATE.

The entities used `chatId` as primary key (not auto-generated), but without `Persistable.isNew()` implementation, Spring Data always tried UPDATE, which failed for new records.

**Fix**:
1. Added `@Table`, `@Id`, `@Column` annotations to domain models
2. Implemented `Persistable<Long>` interface with `isNew()` method
3. Added factory method `createNew()` that sets `_isNew = true` flag
4. Updated services to use `createNew()` when inserting new records

**Files Changed**:
- `src/main/kotlin/ru/andvl/chatkeep/domain/model/Rules.kt`
- `src/main/kotlin/ru/andvl/chatkeep/domain/model/WelcomeSettings.kt`
- `src/main/kotlin/ru/andvl/chatkeep/domain/model/AntifloodSettings.kt`
- `src/main/kotlin/ru/andvl/chatkeep/domain/model/Note.kt`
- `src/main/kotlin/ru/andvl/chatkeep/domain/service/RulesService.kt`
- `src/main/kotlin/ru/andvl/chatkeep/domain/service/WelcomeService.kt`
- `src/main/kotlin/ru/andvl/chatkeep/domain/service/AntifloodService.kt`

**Lesson Learned**:
For Spring Data JDBC entities with non-auto-generated primary keys (like `chatId`):
1. Always implement `Persistable<T>` interface
2. Add `isNew()` method to distinguish INSERT from UPDATE
3. Use factory methods like `createNew()` to properly initialize the `isNew` flag

**Pattern to Follow**:
```kotlin
@Table("table_name")
data class MyEntity(
    @Id
    @Column("chat_id")
    val chatId: Long,
    // other fields...
) : Persistable<Long> {
    @Transient
    private var _isNew: Boolean = false

    override fun getId(): Long = chatId
    override fun isNew(): Boolean = _isNew

    companion object {
        fun createNew(chatId: Long, /* params */): MyEntity {
            return MyEntity(chatId, /* values */).also { it._isNew = true }
        }
    }
}
```

---

## Template for New Entries

```markdown
## YYYY-MM-DD: Brief title

**Issue**: One-line description

**Symptoms**:
- What the user/developer observed
- Error messages, HTTP codes, etc.

**Root Cause**:
Detailed explanation of why the bug occurred.

**Fix**:
What was changed to fix the issue.

**Files Changed**:
- List of modified files

**Lesson Learned**:
Key takeaway to prevent similar issues in the future.
```
