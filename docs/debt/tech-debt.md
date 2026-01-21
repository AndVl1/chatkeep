# Technical Debt Registry

This file tracks technical debt items discovered during research and development.

**Format**: See CLAUDE.md for entry format guidelines.

---

<!-- Add new entries below this line -->

## 2026-01-22 - Inconsistent 404 error handling across Mini App hooks

**Found by**: manual-qa + main agent
**Location**: mini-app/src/hooks/api/*.ts
**Priority**: medium
**Category**: refactor

**Description**:
Several API hooks (useRules, useSession) didn't properly handle 404 errors for resources that can be created. When a resource (rules, session) doesn't exist, the API returns 404, but the UI should show an empty form to create it, not an error state. This was fixed for useRules and useSession, but other hooks should be audited:
- useNotes - should handle 404? Or always returns empty array?
- useBlocklist - same question
- Other similar hooks

**Suggested Fix**:
1. Audit all hooks in mini-app/src/hooks/api/
2. Determine which resources can be created (should handle 404 gracefully)
3. Add consistent 404 handling pattern
4. Consider creating a utility function for this pattern

---

## 2026-01-22 - Bot token mismatch between environments

**Found by**: main agent
**Location**: .github/workflows/deploy.yml, GitHub Secrets
**Priority**: high
**Category**: docs/config

**Description**:
For test environment, TEST_BOT_USERNAME and TEST_BOT_TOKEN must be from the same Telegram bot, and that bot must have the test domain configured in BotFather. Currently this is correctly set up, but the relationship is not documented. If someone changes TEST_BOT_USERNAME without updating TEST_BOT_TOKEN, login will fail silently.

**Suggested Fix**:
1. Add documentation in README about bot configuration requirements
2. Add validation in deploy workflow to check bot username matches token
3. Consider adding environment health check that validates bot config on startup

---

## 2026-01-08 - Test setup was missing i18n initialization

**Found by**: Frontend Developer (react-vite skill)
**Location**: mini-app/src/test/setup.ts
**Priority**: low
**Category**: test

**Description**:
Tests were failing because components using i18n translations (via `useTranslation()` hook) were not working in test environment. The test setup file was missing the i18n initialization import, causing translation keys to be displayed instead of actual translations.

**Suggested Fix**:
Added `import '../i18n'` to the test setup file. This initializes i18next with all translation resources before tests run, ensuring components can properly translate text.
