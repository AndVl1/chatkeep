# TEAM STATE

## Classification
- Type: FEATURE + BUG_FIX
- Complexity: COMPLEX
- Workflow: FULL 7-PHASE
- Branch: feat/mini-app-auto-reply-editor (existing)

## Task
Улучшения функциональности Auto-Reply для Mini App:

1. **Терминология**: Auto-reply работает на ПОСТЫ ПРИВЯЗАННОГО КАНАЛА (не просто "channel reply"). Если канал не привязан - фича должна быть выключена/недоступна.

2. **Дебаунс текстового поля**: Добавить 500ms дебаунс при вводе текста реплая (избежать лишних API вызовов).

3. **Новая архитектура загрузки медиа**:
   - Проблема: MaxUploadSizeExceededException при загрузке 1.4MB картинки
   - Решение: Отложенная загрузка в Telegram
     - Загружаем картинку на сервер (MD5 hash → BLOB в БД)
     - При первом посте в привязанном канале отправляем в Telegram
     - Сохраняем file_id, удаляем BLOB
     - TTL cleanup для "зависших" картинок

## User Decisions (Phase 0)
1. **Hash algorithm**: MD5 (быстрее, достаточно для идентификации)
2. **Storage**: Database BLOB (всё в одном месте)
3. **Orphan media**: TTL cleanup (удалять через N дней)
4. **Debounce**: 500ms

## Progress
- [x] Phase 0: Classification - COMPLETED
- [x] Phase 1: Discovery - COMPLETED
- [x] Phase 2: Exploration - COMPLETED
- [x] Phase 3: Questions - COMPLETED
- [x] Phase 4: Architecture - COMPLETED
- [x] Phase 5: Implementation - COMPLETED
- [x] Phase 6: Review - COMPLETED
- [x] Phase 6.5: Review Fixes - COMPLETED
- [x] Phase 7: Summary - COMPLETED

## Key Decisions
- Feature branch: feat/mini-app-auto-reply-editor (продолжаем работу)
- Hash: MD5
- Storage: PostgreSQL BLOB
- Debounce: 500ms
- Orphan cleanup: TTL-based

## Files Identified

### Backend
1. `src/main/kotlin/ru/andvl/chatkeep/bot/handlers/channelreply/ChannelPostHandler.kt` - обработка постов канала
2. `src/main/kotlin/ru/andvl/chatkeep/domain/service/channelreply/ChannelReplyService.kt` - сервис CRUD
3. `src/main/kotlin/ru/andvl/chatkeep/api/service/MediaUploadService.kt` - загрузка медиа
4. `src/main/kotlin/ru/andvl/chatkeep/api/config/MediaUploadConfig.kt` - конфиг размеров
5. `src/main/resources/application.yml` - **НЕТ multipart конфига (причина ошибки)**
6. `src/main/resources/db/migration/V8__add_channel_reply_settings.sql` - схема БД

### Frontend
7. `mini-app/src/pages/ChannelReplyPage.tsx` - страница (нет debounce)
8. `mini-app/src/components/channel-reply/TextEditor.tsx` - поле ввода текста
9. `mini-app/src/hooks/api/useChannelReply.ts` - хук API
10. `mini-app/src/hooks/ui/useDebouncedValue.ts` - **существует, но не используется**
11. `mini-app/src/i18n/locales/ru.json` - **неправильная терминология**

## Phase 2 Findings

### 1. Терминология (КРИТИЧНО)
i18n ключи описывают фичу как "автоответ новым участникам":
- `settings.channelReply`: "Автоответ новым участникам"
- `channelReply.enabledDescription`: "при присоединении новых участников"
- `channelReply.previewNote`: "для новых участников"

**На самом деле**: Это автоответ на ПОСТЫ ИЗ ПРИВЯЗАННОГО КАНАЛА в дискуссионном чате.

### 2. MaxUploadSizeExceededException (ROOT CAUSE)
- `application.yml` НЕ содержит `spring.servlet.multipart.max-file-size`
- Spring Boot default = **1MB**
- MediaUploadConfig.maxFileSizeMb = 10MB (проверка внутри кода)
- 1.4MB файл отклоняется Spring'ом ДО достижения нашего кода

### 3. Дебаунс отсутствует
- `useDebouncedValue` хук существует (500ms default)
- ChannelReplyPage.handleChange() вызывает mutate() напрямую
- Каждое нажатие клавиши = API запрос

### 4. Текущая архитектура медиа
- Файл → Telegram Bot API → file_id → сохраняем в БД
- Нет локального хранения
- Нет BLOB в БД

## Phase 3 Decisions
1. **Architecture**: Full BLOB (MD5 hash, DB storage, lazy upload to Telegram)
2. **TTL**: 7 days for orphan media cleanup
3. **Linked channel validation**: Yes - проверять через Telegram API

## Chosen Approach

### Backend (BLOB Media Storage)
1. Новая таблица `media_storage` (hash, BYTEA blob, mime_type, telegram_file_id)
2. `MediaStorageService` - MD5 hashing, blob storage, lazy file_id resolution
3. `MediaCleanupScheduler` - @Scheduled cleanup orphan media (7 days TTL)
4. `LinkedChannelService` - проверка привязки канала через Telegram API
5. Модификация `ChannelPostHandler` - resolve blob → file_id при первом посте
6. Spring multipart config: max-file-size=20MB

### Frontend
1. i18n fix - исправить терминологию на "посты из привязанного канала"
2. Debounce 500ms в `ChannelReplyPage.tsx` через `useDebouncedValue`
3. `NoLinkedChannelWarning` component - предупреждение если нет канала
4. Обновить types для linkedChannel info

## Phase 5 Output

### Backend (developer agent)
**Commits:**
- `b1cb884 feat: add media storage with hash-based deduplication`

**Files Created:**
- `src/main/resources/db/migration/V12__add_media_storage.sql`
- `src/main/kotlin/ru/andvl/chatkeep/domain/model/media/MediaStorage.kt`
- `src/main/kotlin/ru/andvl/chatkeep/infrastructure/repository/media/MediaStorageRepository.kt`
- `src/main/kotlin/ru/andvl/chatkeep/domain/service/media/MediaStorageService.kt`
- `src/main/kotlin/ru/andvl/chatkeep/domain/service/media/MediaCleanupScheduler.kt`
- `src/main/kotlin/ru/andvl/chatkeep/domain/service/channelreply/LinkedChannelService.kt`

**Files Modified:**
- `application.yml` - added multipart config (20MB)
- `ChannelReplySettings.kt` - added mediaHash field
- `ChannelReplyService.kt` - added setMediaByHash
- `MiniAppChannelReplyController.kt` - blob upload flow
- `ChannelPostHandler.kt` - resolve hash to file_id
- `ResponseDtos.kt` - added mediaHash, hasMedia, linkedChannel

**Build Status:** ✅ PASS

### Frontend (frontend-developer agent)
**Commits:**
- `71370bf feat: add debounced text input and linked channel warning`
- `53280d3 feat: update ChannelReplyForm to support local text state`
- `8792089 feat: add NoLinkedChannelWarning component`
- `2eab3fa feat: add LinkedChannel type and extend ChannelReply`
- `49445ef feat: update channel reply i18n terminology`

**Files Created:**
- `NoLinkedChannelWarning.tsx`

**Files Modified:**
- `ru.json`, `en.json` - fixed terminology
- `types/index.ts` - added LinkedChannel
- `ChannelReplyForm.tsx` - local text state
- `ChannelReplyPage.tsx` - debounce + warning

**Build Status:** ✅ PASS

## Phase 6.5 Fixes

### Code Review Issues Fixed:
- `1217b1f fix: resolve code review issues in channel reply frontend`
  - Fixed debounce effect missing dependencies (stale closures)
  - Fixed race condition when switching chats (chatId validation)
  - Fixed unstable fetchChannelReply callback

### Test Fixes:
- `5c3c025 test: fix media upload tests for hash-based storage`
  - Updated tests to mock `mediaStorageService.storeMedia()` instead of old `mediaUploadService.uploadToTelegram()`
  - Updated response assertions from `fileId` to `hash`

### Migration Compatibility Fix:
- `adb7a15 fix: use standard SQL TIMESTAMP WITH TIME ZONE for H2 compatibility`
  - Changed `TIMESTAMPTZ` to `TIMESTAMP WITH TIME ZONE` in V12 migration
  - Required for H2 PostgreSQL mode compatibility (OpenAPI generation profile)

## Phase 7: Summary

### Feature Completed Successfully ✅

**Implemented Features:**

1. **Terminology Fix**: Updated i18n strings (RU/EN) to correctly describe the feature as "auto-reply to linked channel posts" instead of "auto-reply to new members"

2. **Debounce (500ms)**: Added debounce to text input in ChannelReplyPage to prevent excessive API calls

3. **BLOB Media Storage Architecture**:
   - New `media_storage` table with MD5 hash deduplication
   - Upload stores BYTEA blob in DB, returns hash
   - On first channel post, blob is uploaded to Telegram
   - `telegram_file_id` cached, blob deleted
   - 7-day TTL cleanup for orphan media via `MediaCleanupScheduler`

4. **Linked Channel Validation**:
   - `LinkedChannelService` checks if chat has linked channel via Telegram API
   - `NoLinkedChannelWarning` component shows warning when no channel linked
   - API returns `linkedChannel` info in response

**All Commits:**
```
adb7a15 fix: use standard SQL TIMESTAMP WITH TIME ZONE for H2 compatibility
5c3c025 test: fix media upload tests for hash-based storage
1217b1f fix: resolve code review issues in channel reply frontend
f14bac0 feat: add loading states and user feedback improvements
...
b1cb884 feat: add media storage with hash-based deduplication
```

**Build Status:** ✅ All tests pass locally
**CI Status:** Pending (triggered)
