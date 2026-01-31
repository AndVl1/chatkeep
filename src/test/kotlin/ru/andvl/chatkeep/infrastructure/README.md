# Database Migration Tests

Comprehensive test suite for database migrations, schema integrity, and BLOB serialization.

## Test Files

### 1. FlywayMigrationTest.kt
**Purpose**: Basic migration validation
**What it tests**:
- All migrations apply successfully
- Tables and columns exist
- Indexes are created
- Constraints are enforced

**When to run**: Always (runs in CI automatically)

---

### 2. MediaStorageBlobTest.kt
**Purpose**: BLOB (BYTEA) serialization/deserialization
**What it tests**:
- Binary data integrity (small and large files)
- All byte values (0x00-0xFF) are preserved
- Empty blobs
- MD5 hash consistency after retrieval
- Data survives updates

**Critical for**: Ensuring media files aren't corrupted in database

**When to run**:
- After changes to MediaStorage model
- After changes to V12__add_media_storage.sql
- Before deploying changes that affect BYTEA columns

---

### 3. MigrationCompatibilityTest.kt
**Purpose**: Migration quality and backward compatibility
**What it tests**:
- Migrations are versioned sequentially (no gaps)
- Unique migration descriptions
- Migrations execute in order
- Detection of breaking changes (DROP TABLE, DROP COLUMN)
- All required tables exist
- Foreign key ON DELETE behavior
- Timestamp columns use TIMESTAMP WITH TIME ZONE
- Primary keys are indexed

**When to run**:
- After adding new migrations
- Before merging migration PRs

---

### 4. DatabaseSchemaSnapshotTest.kt
**Purpose**: Track schema changes over time
**What it does**:
- Generates complete schema snapshot (tables, columns, indexes, constraints)
- Compares current schema with saved snapshot
- Fails if schema differs (prevents accidental changes)

**Workflow**:

#### Normal test run (CI):
```bash
./gradlew test --tests DatabaseSchemaSnapshotTest
```
If schema changed, test fails with diff showing what changed.

#### Regenerate snapshot after intentional changes:
```bash
REGENERATE_SNAPSHOT=true ./gradlew test --tests DatabaseSchemaSnapshotTest
```

**Review the changes** in `src/test/resources/db-schema-snapshot.txt`, then commit:
```bash
git add src/test/resources/db-schema-snapshot.txt
git commit -m "docs: update database schema snapshot after migration V25"
```

**When to regenerate**:
- After adding new migrations
- After modifying existing migrations (avoid if possible!)
- When MigrationCompatibilityTest detects schema changes

---

## CI Integration

All tests run automatically in CI pipeline (`.github/workflows/ci.yml`).

### What happens in CI:
1. **Backend Build and Test** job starts
2. Testcontainers spins up PostgreSQL 16
3. Flyway applies all migrations
4. All 4 test suites run
5. If snapshot test fails → PR must update snapshot
6. If blob test fails → binary serialization broken (critical!)
7. If compatibility test fails → migration quality issues

---

## Local Development

### Prerequisites:
- Docker running (for Testcontainers)

### Run all migration tests:
```bash
./gradlew test --tests "ru.andvl.chatkeep.infrastructure.*"
```

### Run specific test:
```bash
# BLOB tests
./gradlew test --tests MediaStorageBlobTest

# Migration compatibility
./gradlew test --tests MigrationCompatibilityTest

# Schema snapshot
./gradlew test --tests DatabaseSchemaSnapshotTest
```

### Regenerate snapshot locally:
```bash
REGENERATE_SNAPSHOT=true ./gradlew test --tests DatabaseSchemaSnapshotTest
```

---

## Adding New Migrations

### Checklist:
1. Create migration file: `VXX__description.sql`
2. Apply migration locally and test manually
3. Run `./gradlew test` to verify:
   - FlywayMigrationTest passes
   - MigrationCompatibilityTest passes
   - No breaking changes detected
4. Regenerate snapshot:
   ```bash
   REGENERATE_SNAPSHOT=true ./gradlew test --tests DatabaseSchemaSnapshotTest
   ```
5. Review snapshot diff carefully
6. Commit migration + updated snapshot
7. PR gets reviewed with schema changes visible

---

## Common Issues

### "Docker not found" error
**Cause**: Docker not running or Testcontainers can't access it
**Fix**: Start Docker Desktop or Docker daemon

### Snapshot test fails
**Cause**: Schema changed but snapshot not updated
**Fix**:
1. Review schema diff
2. Regenerate: `REGENERATE_SNAPSHOT=true ./gradlew test --tests DatabaseSchemaSnapshotTest`
3. Commit updated snapshot

### BLOB test fails
**Cause**: Binary data corruption in database
**Fix**:
1. Check if Spring Data JDBC mapping changed
2. Verify PostgreSQL BYTEA driver compatibility
3. Check if migration modified `content` column type

### Migration compatibility warnings
**Cause**: Risky operations detected (DROP TABLE, DROP COLUMN)
**Fix**: Review migration - breaking changes may need data migration strategy

---

## Best Practices

### For migrations:
- ✅ Always use sequential versioning (no gaps)
- ✅ Add indexes for foreign keys
- ✅ Use `TIMESTAMP WITH TIME ZONE` for all timestamps
- ✅ Define ON DELETE behavior for foreign keys
- ✅ Test rollback scenario (manually)
- ❌ Avoid DROP TABLE in production migrations
- ❌ Avoid DROP COLUMN (add new column instead)

### For BLOB fields:
- ✅ Test with realistic file sizes
- ✅ Verify MD5 hash after retrieval
- ✅ Test empty blobs
- ✅ Test all byte values (including 0x00)

### For schema snapshots:
- ✅ Review diff carefully before regenerating
- ✅ Commit snapshot with migration in same PR
- ✅ Add explanation in PR description if schema changes significantly
- ❌ Don't regenerate snapshot without reviewing changes

---

## Example PR Flow

```
1. Create migration V26__add_user_preferences.sql
2. Test locally: ./gradlew test
3. Regenerate snapshot: REGENERATE_SNAPSHOT=true ./gradlew test --tests DatabaseSchemaSnapshotTest
4. Review changes in db-schema-snapshot.txt
5. Commit both files:
   - V26__add_user_preferences.sql
   - db-schema-snapshot.txt
6. Create PR
7. CI runs all tests
8. Reviewer sees schema changes in snapshot diff
9. Merge after approval
```
