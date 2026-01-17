# TEAM STATE

## Classification
- Type: BUG_FIX
- Complexity: MEDIUM
- Workflow: STANDARD
- Branch: feat/miniapp-bot-admin-warning

## Task
Исправить 4 проблемы:
1. Grafana active_chats метрика показывает 0 - нужно считать чаты как в мобильном приложении
2. Mobile App: Workflows не отображаются (PAT добавлен в .env, но не работает)
3. Mobile App: Добавить pull-to-refresh через expect/actual (Android)
4. Mini App: Добавить предупреждение если бот не админ в чате

Создать один PR с фиксами. Использовать manual QA для тестирования.

## Progress
- [x] Phase 1: Discovery - COMPLETED
- [x] Phase 2: Exploration - COMPLETED
- [x] Phase 3: Questions - COMPLETED
- [x] Phase 4: Architecture - COMPLETED
- [x] Phase 5: Implementation - COMPLETED
- [ ] Phase 6: Review - IN PROGRESS
- [ ] Phase 6.5: Review Fixes - pending (if needed)
- [ ] Phase 7: Summary - pending

## Key Decisions
- Issue 2: PAT есть на проде, нужно только добавить в .env.example для документации
- Issue 4: Показывать предупреждение в обоих местах (список чатов + страницы настроек)

## Files Modified

### Backend (Issue 1 + Issue 4 API)
- `src/main/kotlin/ru/andvl/chatkeep/domain/service/ChatService.kt` - @PostConstruct для инициализации метрики
- `src/main/kotlin/ru/andvl/chatkeep/api/dto/ResponseDtos.kt` - добавлено поле isBotAdmin
- `src/main/kotlin/ru/andvl/chatkeep/api/controller/MiniAppChatsController.kt` - проверка bot admin статуса
- `.env.example` - добавлен GITHUB_PAT

### Mobile App (Issue 3)
- `chatkeep-admin/feature/chats/impl/src/commonMain/kotlin/com/chatkeep/admin/feature/chats/ui/ChatsScreen.kt`
- `chatkeep-admin/feature/dashboard/impl/src/commonMain/kotlin/com/chatkeep/admin/feature/dashboard/ui/DashboardScreen.kt`
- `chatkeep-admin/feature/deploy/impl/src/commonMain/kotlin/com/chatkeep/admin/feature/deploy/ui/DeployScreen.kt`

### Mini App (Issue 4 UI)
- `mini-app/src/types/index.ts` - добавлено isBotAdmin
- `mini-app/src/i18n/locales/en.json` - translations
- `mini-app/src/i18n/locales/ru.json` - translations
- `mini-app/src/components/common/AdminWarningBanner.tsx` - новый компонент
- `mini-app/src/components/chats/ChatCard.tsx` - warning badge
- `mini-app/src/pages/SettingsPage.tsx` - banner
- `mini-app/src/pages/BlocklistPage.tsx` - banner
- `mini-app/src/pages/LocksPage.tsx` - banner
- `mini-app/src/test/mocks/data.ts` - mock data

## Commits
- 7462d07 feat: add isBotAdmin field to Chat interface
- 2d63a6f feat: add admin warning translations for bot admin status
- 80de561 feat: create AdminWarningBanner component
- 9a2fe56 feat: add warning badge to ChatCard for non-admin bot status
- 2cc91ac feat: add admin warning banners to Settings, Blocklist and Locks pages
- 3aeda09 test: update mock data with isBotAdmin field
- ae968fa feat: add pull-to-refresh to Chats, Dashboard and Deploy screens
- 9e70e26 fix: initialize active_chats metric on application startup
- 34c2c0d docs: add GITHUB_PAT to .env.example
- 89b9dae feat: add isBotAdmin field to chat API response

## Recovery
Continue from Phase 6. Implementation complete. Ready for QA review and manual testing.
