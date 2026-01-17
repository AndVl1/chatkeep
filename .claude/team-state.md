# TEAM STATE

## Classification
- Type: FEATURE
- Complexity: COMPLEX
- Workflow: FULL 7-PHASE
- Branch: fix/production-bugs-jan-2026 (continuing)

## Task
Add contract testing between backend and mobile app:
1. Backend generates OpenAPI schema for API responses
2. Generate test JSON fixtures from OpenAPI schema
3. Mobile app uses fixtures in contract tests
4. Single PR pipeline step runs both fixture generation and mobile tests

## Progress
- [x] Phase 1: Discovery - COMPLETED
- [x] Phase 2: Exploration - COMPLETED
- [x] Phase 3: Questions - COMPLETED
- [x] Phase 4: Architecture - COMPLETED
- [ ] Phase 5: Implementation - IN PROGRESS
- [ ] Phase 6: Review - pending
- [ ] Phase 7: Summary - pending

## Phase 3 Decisions
- **Generator**: openapi-generator CLI
- **OpenAPI**: springdoc-openapi-gradle-plugin (no server startup)
- **Tests**: jvmTest only (fast CI)

## Chosen Architecture

### 1. Backend OpenAPI Generation
- Add springdoc-openapi-gradle-plugin to build.gradle.kts
- Generate openapi.json to build/openapi/
- Upload as artifact in CI

### 2. Fixture Generation
- Use openapi-generator CLI or custom script
- Generate example JSONs from schema
- Place in chatkeep-admin/core/network/src/jvmTest/resources/fixtures/

### 3. Mobile Contract Tests
- Add jvmTest source set to core/network module
- Add kotlin-test dependencies
- Create ContractTestBase + 5 test classes:
  - AuthContractTest (LoginResponse, AdminResponse)
  - DashboardContractTest
  - ChatsContractTest
  - LogsContractTest
  - WorkflowsContractTest (WorkflowResponse, TriggerResponse, ActionResponse)

### 4. CI Pipeline
- New contract-tests job depending on backend
- Download OpenAPI artifact → Generate fixtures → Run jvmTest

## Files to Create/Modify

### Backend
- build.gradle.kts (add plugin)
- scripts/generate-fixtures.sh or .kt

### Mobile
- chatkeep-admin/gradle/libs.versions.toml (test deps)
- chatkeep-admin/core/network/build.gradle.kts (jvmTest)
- chatkeep-admin/core/network/src/jvmTest/kotlin/.../contract/*.kt (tests)
- chatkeep-admin/core/network/src/jvmTest/resources/fixtures/*.json

### CI
- .github/workflows/ci.yml (contract-tests job)

## Recovery
Continue from Phase 5 Implementation. Architecture approved.
