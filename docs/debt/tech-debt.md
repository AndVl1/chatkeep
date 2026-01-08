# Technical Debt Registry

This file tracks technical debt items discovered during research and development.

**Format**: See CLAUDE.md for entry format guidelines.

---

<!-- Add new entries below this line -->

## 2026-01-08 - Test setup was missing i18n initialization

**Found by**: Frontend Developer (react-vite skill)
**Location**: mini-app/src/test/setup.ts
**Priority**: low
**Category**: test

**Description**:
Tests were failing because components using i18n translations (via `useTranslation()` hook) were not working in test environment. The test setup file was missing the i18n initialization import, causing translation keys to be displayed instead of actual translations.

**Suggested Fix**:
Added `import '../i18n'` to the test setup file. This initializes i18next with all translation resources before tests run, ensuring components can properly translate text.
