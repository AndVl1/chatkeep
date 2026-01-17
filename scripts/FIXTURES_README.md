# API Fixtures for Contract Testing

This directory contains tools for generating JSON fixtures from the OpenAPI schema.

## Prerequisites

- **jq**: JSON processor - install with `brew install jq`
- **Running application**: The OpenAPI plugin requires the app to be running

## Usage

### 1. Generate OpenAPI Schema

The `springdoc-openapi-gradle-plugin` requires the application to be running:

```bash
# Terminal 1: Start the application
./gradlew bootRun

# Terminal 2: Generate OpenAPI docs
./gradlew generateOpenApiDocs
```

This creates `build/openapi/openapi.json`.

### 2. Generate Fixtures

```bash
./scripts/generate-fixtures.sh
```

Or specify custom paths:

```bash
./scripts/generate-fixtures.sh path/to/openapi.json path/to/output
```

### 3. Generated Fixtures

The script generates these fixtures in `build/fixtures/`:

- `login_response.json` - JWT authentication token
- `dashboard_response.json` - Dashboard overview with service status
- `chats_response.json` - Array of chat summaries
- `logs_response.json` - Paginated log entries
- `workflows_response.json` - GitHub Actions workflows
- `trigger_response.json` - Workflow trigger confirmation
- `action_response.json` - Generic action response
- `user_response.json` - Telegram user information
- `error_response.json` - Error response format
- `settings_response.json` - Chat settings
- `index.json` - Fixture manifest

## Using Fixtures in Tests

### Frontend (TypeScript)

```typescript
import loginResponse from '../fixtures/login_response.json';

test('handles successful login', () => {
  mockFetch.mockResolvedValue({
    ok: true,
    json: async () => loginResponse
  });

  // test code...
});
```

### Mobile (KMP)

```kotlin
// In test resources
val loginFixture = this::class.java.classLoader
    .getResource("fixtures/login_response.json")
    .readText()

val loginResponse = Json.decodeFromString<TokenResponse>(loginFixture)
```

### Contract Testing

Use fixtures to verify:
1. Response structure matches expectations
2. Field types are correct
3. Required fields are present
4. Nullable fields handle null correctly

## CI/CD Integration

Add to your CI workflow:

```yaml
- name: Generate API fixtures
  run: |
    ./gradlew bootRun &
    sleep 10
    ./gradlew generateOpenApiDocs
    ./scripts/generate-fixtures.sh

- name: Upload fixtures
  uses: actions/upload-artifact@v3
  with:
    name: api-fixtures
    path: build/fixtures/
```

## Updating Fixtures

When the API changes:

1. Add/update `@Schema` annotations on DTOs
2. Start the application
3. Re-generate OpenAPI docs
4. Re-generate fixtures
5. Update frontend/mobile tests as needed

## Schema Annotations

Fixtures are generated from `@Schema` annotations on DTOs:

```kotlin
@Schema(description = "User information")
data class UserResponse(
    @Schema(description = "User ID", example = "123")
    val id: Long,

    @Schema(description = "Username", example = "johndoe")
    val username: String
)
```

Good annotations = better fixtures!
