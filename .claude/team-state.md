# TEAM STATE

## Classification
- Type: FEATURE
- Complexity: COMPLEX
- Workflow: FULL 7-PHASE

## Task
Implement full localization (i18n) for both Telegram bot and Mini App:
- Support RU and EN locales
- User-level locale preference
- Chat-level locale preference (for bot messages in groups)
- Localize: configurations, descriptions, toggle names
- Allow switching between locales in both bot and Mini App

## Progress
- [x] Phase 1: Discovery - COMPLETED
- [x] Phase 2: Exploration - COMPLETED
- [x] Phase 3: Questions - SKIPPED (user requested no questions)
- [x] Phase 4: Architecture - COMPLETED
- [ ] Phase 5: Implementation - IN PROGRESS
- [ ] Phase 6: Review - pending
- [ ] Phase 7: Summary - pending

## Key Decisions
- Branch created: feat/full-localization
- User requested implementation without questions
- Pragmatic approach: Spring MessageSource + react-i18next

## Phase 4 Output: Architecture Design

### Database Migration (V11__add_locale_support.sql)
- Add `locale` column to `chat_settings` table (VARCHAR(5), default 'en')
- Create `user_preferences` table (user_id BIGINT PK, locale VARCHAR(5))

### Backend Components
1. **UserPreferences.kt** - New entity for user locale
2. **UserPreferencesRepository.kt** - Repository interface
3. **BotMessageService.kt** - Central service for localized bot messages
4. **LocaleResolver.kt** - Resolves locale from chat/user context
5. **messages_en.properties** - English strings (~150 keys)
6. **messages_ru.properties** - Russian strings (~150 keys)

### Frontend Components
1. **i18n/index.ts** - i18next configuration
2. **i18n/locales/en.json** - English UI strings (~80 keys)
3. **i18n/locales/ru.json** - Russian UI strings
4. **hooks/i18n/useLocale.ts** - Custom locale hook
5. **components/settings/LocaleSelector.tsx** - Language switcher

### API Changes
- GET/PUT /api/v1/miniapp/preferences - User locale
- Chat settings DTO includes locale field

### Locale Resolution
- Group chats: chat_settings.locale -> default EN
- Private chats: user_preferences.locale -> default EN
- Mini App: user_preferences.locale -> Telegram WebApp language -> default EN

## Chosen Approach
- Backend: Spring MessageSource with properties files
- Frontend: react-i18next with JSON locale files
- Database: Add locale fields to existing tables + new user_preferences table

## Recovery
Continue from Phase 5 Implementation. Launch backend and frontend developers in parallel.
