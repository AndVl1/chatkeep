# TEAM STATE

## Classification
- Type: BUG_FIX + TESTING + OPS
- Complexity: COMPLEX (comprehensive end-to-end testing + deployment + fixes)
- Workflow: CUSTOM TESTING & FIX WORKFLOW

## Task
Test ALL newly added Mini App features for functionality:
- Each page must open correctly
- All data from pages must persist to backend
- Fix any broken features until everything works
- Deploy using GitHub Actions workflow (temporarily remove Compose WASM build, restore later)
- Test domain: chatmodtest.ru
- Can check server logs via SSH: 204.77.3.163 root

## Progress
- [x] Phase 1: Discovery - COMPLETED
- [ ] Phase 2: Deploy to chatmodtest.ru - IN PROGRESS
- [ ] Phase 3: Identify ALL features to test - pending
- [ ] Phase 4: Manual E2E testing - pending
- [ ] Phase 5: Fix issues found - pending
- [ ] Phase 6: Re-test fixes - pending
- [ ] Phase 7: Final verification - pending
- [ ] Phase 8: Restore WASM build - pending

## Key Decisions
- Feature branch: feat/bot-miniapp-feature-parity
- Test domain: chatmodtest.ru
- SSH access: 204.77.3.163 root
- Autonomous execution (no questions)

## Features to Test

### Existing Features (pre-implementation)
1. HomePage - Chat list selector
2. SettingsPage - Basic chat settings
3. LocksPage - Lock toggles (31+ types)
4. BlocklistPage - Blocklist CRUD
5. ChannelReplyPage - Channel reply configuration

### NEW Features (Phase 5 implementation)
6. StatisticsPage - Chat statistics dashboard (/stats)
7. ModerationPage - Warn/mute/ban/kick users (/warn, /mute, /ban, /kick)
8. SessionPage - Admin session management (/connect, /disconnect)
9. AdminLogsPage - Export admin logs (/viewlogs)
10. WelcomePage - Welcome/goodbye messages
11. RulesPage - Chat rules management (/rules, /setrules)
12. NotesPage - Notes system (/note, /save, /notes, /delnote)
13. AntiFloodPage - Anti-flood protection settings
14. CapabilitiesPage - All features overview

## Phase 2 Output
[Deploy results will go here]

## Phase 3 Output
[Feature testing checklist will go here]

## Recovery
Continue from first incomplete phase. Read this file first.
