# TEAM STATE

## Classification
- Type: BUG_FIX + OPS
- Complexity: MEDIUM
- Workflow: STANDARD (5 phases + deployment verification)

## Task
Fix test environment login button showing production bot nickname (@chatautomoderbot) instead of test bot (@tg_chat_dev_env_bot). Parameterize bot nickname configuration, deploy to test environment from custom branch, and verify login button at chatmodtest.ru.

## Progress
- [x] Phase 1: Discovery - COMPLETED
- [x] Phase 2: Exploration - COMPLETED
- [x] Phase 3: Questions - SKIPPED (clear bug fix)
- [x] Phase 4: Architecture - COMPLETED
- [x] Phase 5: Implementation - COMPLETED
- [x] Phase 6: Review - COMPLETED
- [x] Phase 6.5: Review Fixes - COMPLETED
- [x] Phase 7: Deployment & Verification - COMPLETED
- [x] Phase 8: Summary - COMPLETED

## Phase 5 Output
**Commit**: e140f97

**Changes Made**:
1. `.github/workflows/deploy.yml`:
   - Added `IS_PRODUCTION` env to `build-mini-app` job
   - Added "Configure Mini App environment" step before build
   - Dynamically injects bot username from GitHub variables
   - Uses `PROD_BOT_USERNAME` for production, `TEST_BOT_USERNAME` for test

**Files Modified**:
- .github/workflows/deploy.yml (added env block + new step)

## Phase 6 Output
**Review Findings**:
1. CRITICAL: Missing validation for empty BOT_USERNAME
2. CRITICAL: GitHub variables not created yet
3. MEDIUM: Expression syntax could be clearer

## Phase 6.5 Output
**Commit**: ba58ee6

**Fixes Applied**:
1. Added BOT_USERNAME validation with helpful error message
2. Improved expression syntax with explicit parentheses
3. Prevents silent failures when GitHub variables not set

## Phase 7 Output
**GitHub Variables Created**:
- `PROD_BOT_USERNAME = chatAutoModerBot`
- `TEST_BOT_USERNAME = tg_chat_dev_env_bot`

**Deployment**:
- Workflow Run: 21152521334
- Triggered: workflow_dispatch on branch fix/test-env-bot-nickname-parameterization
- Environment: test
- Status: ✓ SUCCESS (all jobs completed)
- Total time: ~11 minutes

**Verification Results** (manual-qa):
- ✅ chatmodtest.ru login button shows `@tg_chat_dev_env_bot`
- ✅ No references to production bot `@chatautomoderbot`
- ✅ Widget script tag: `data-telegram-login="tg_chat_dev_env_bot"`
- ✅ No console errors
- ✅ READY FOR RELEASE

## Key Decisions
- Created fix branch: fix/test-env-bot-nickname-parameterization
- Need to find all hardcoded bot references (@chatautomoderbot)
- Need to parameterize bot nickname for different environments
- Deploy to test environment and verify at chatmodtest.ru

## Phase 2 Findings

### Root Cause
Mini App uses hardcoded bot username in `.env.production` file:
- File: `mini-app/.env.production`
- Line 6: `VITE_BOT_USERNAME=chatAutoModerBot` (production bot)
- This is used for BOTH production AND test deployments
- Test environment should use `VITE_BOT_USERNAME=tg_chat_dev_env_bot`

### Current Architecture
1. **Frontend (Mini App)**:
   - Build time: Uses `.env.production` for both prod and test
   - Runtime: `VITE_BOT_USERNAME` embedded in built JavaScript
   - Component: `LoginPage.tsx` reads bot username and passes to Telegram Widget

2. **Backend**:
   - Already parameterized via `TELEGRAM_BOT_TOKEN` (prod vs test secrets)
   - Validates authentication hash against bot token
   - No bot username configuration needed in backend

3. **Deploy Workflow** (`.github/workflows/deploy.yml`):
   - Line 158: Detects environment (production vs test)
   - Line 250-253: Uses correct secrets per environment (BOT_TOKEN, DB_PASSWORD, etc.)
   - Line 98: Builds Mini App with `npm run build` (ALWAYS uses `.env.production`)

### Missing Configuration
- `TELEGRAM_BOT_USERNAME` not exposed to Mini App build process
- Mini App build doesn't know which environment it's deploying to
- No `.env.test` file for test-specific bot username

## Files Identified

### Files Requiring Changes
1. `mini-app/.env.production` - Production bot username
2. `mini-app/.env.test` - NEW: Test bot username
3. `.github/workflows/deploy.yml` - Update Mini App build step to select correct .env file
4. GitHub Variables - Add `TEST_BOT_USERNAME` and `PROD_BOT_USERNAME`

## Chosen Approach
**Dynamic Environment Variable Injection from GitHub Variables**

Implementation:
1. Add GitHub Variables: PROD_BOT_USERNAME, TEST_BOT_USERNAME
2. Update deploy workflow to inject bot username before Mini App build
3. Use sed to update .env.production with correct username based on environment
4. Build Mini App with environment-specific configuration

Benefits:
- Bot usernames managed centrally in GitHub Variables
- Not stored in repository
- Follows existing pattern (DEPLOY_HOST/TEST_DEPLOY_HOST)
- No duplicate .env files needed

## Recovery
Currently at Phase 1 complete. Feature branch created. Moving to Phase 2 exploration.
