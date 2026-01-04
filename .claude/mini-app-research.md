# Telegram Mini App Research Report

**Project**: Chatkeep Bot Configuration Mini App
**Date**: 2026-01-04
**Branch**: `research/telegram-mini-apps`
**Status**: Research Complete - Ready for Implementation

---

## Executive Summary

This report covers comprehensive research for building a Telegram Mini App to configure the Chatkeep bot. The Mini App will provide a visual interface for all configuration options currently available via bot commands.

**Key Findings**:
- Telegram Mini Apps are fully supported for bot configuration use cases
- React + Vite + @telegram-apps/sdk is the recommended stack
- Local development without deployment is possible via mock SDK + TMA Studio
- All 10 configurable features from Chatkeep can be exposed via Mini App
- Backend needs new REST API endpoints with initData authentication

---

## 1. Telegram Mini Apps Overview

### What Are Mini Apps?
Web applications (HTML/CSS/JS) running inside Telegram's WebView. They integrate with bots, authenticate users automatically, and provide native-like UI.

### Key Capabilities
| Feature | Description |
|---------|-------------|
| **User Authentication** | Automatic via signed `initData` |
| **Theme Integration** | Matches Telegram's light/dark mode |
| **Native Buttons** | MainButton, BackButton integration |
| **Storage** | 5MB device storage, 10 items secure storage |
| **Haptic Feedback** | Vibration support |
| **Full-Screen Mode** | Available on mobile |

### Limitations
| Limitation | Impact |
|------------|--------|
| HTTPS Required | Need tunneling for local dev |
| 4KB sendData limit | Use REST API instead |
| No camera/microphone | Not needed for config |
| initData expires in ~1 hour | Re-validate on critical actions |
| WebView constraints | Standard web limitations apply |

### Platform Support
- iOS, Android, macOS, Windows, Linux (Desktop)
- Telegram Web (K), Telegram Web (A)
- All platforms support debugging

---

## 2. Recommended Technology Stack

### Frontend

| Technology | Package | Purpose |
|------------|---------|---------|
| **React 18+** | `react`, `react-dom` | UI framework |
| **TypeScript** | `typescript` | Type safety |
| **Vite** | `vite` | Build tool, HMR |
| **TMA SDK** | `@telegram-apps/sdk-react` | Telegram integration |
| **UI Components** | `@telegram-apps/ui` | Native Telegram look |
| **Routing** | `react-router-dom` | Navigation |
| **State** | `zustand` | State management |
| **HTTP Client** | `ky` | API calls |

### Backend (Additions to Existing)

| Technology | Purpose |
|------------|---------|
| **Spring REST Controllers** | New `/api/v1/miniapp/*` endpoints |
| **TelegramAuthService** | Validate initData signatures |
| **JWT Sessions** (optional) | Persistent auth after validation |

### Development Tools

| Tool | Purpose |
|------|---------|
| **TMA Studio** | Local Telegram simulation |
| **Cloudflare Tunnel** | HTTPS for device testing |
| **Eruda** | Mobile browser console |

---

## 3. Local Development Setup

### Phase 1: Browser-Only (Fastest)

```bash
# Clone official template
git clone https://github.com/Telegram-Mini-Apps/reactjs-template
cd reactjs-template
npm install
npm run dev
```

The template includes mock environment - open `http://localhost:5173` in browser.

### Phase 2: TMA Studio (Telegram Simulation)

1. Download TMA Studio from [GitHub](https://github.com/erfanmola/TMA-Studio)
2. Launch and select platform (Android/iOS/Desktop)
3. Enter `http://localhost:5173`
4. Configure mock user data
5. Test with full Telegram simulation

### Phase 3: Real Device Testing

```bash
# Install Cloudflare Tunnel
brew install cloudflared

# Start tunnel
cloudflared tunnel --url http://localhost:5173

# Use returned HTTPS URL in @BotFather
```

### Mock Environment Code

```tsx
// src/mockEnv.ts
import { mockTelegramEnv } from '@telegram-apps/sdk-react';

if (import.meta.env.DEV && !window.Telegram?.WebApp) {
  mockTelegramEnv({
    themeParams: {
      bgColor: '#17212b',
      textColor: '#f5f5f5',
      buttonColor: '#5288c1',
      // ... full theme
    },
    initData: {
      user: {
        id: 99281932,
        firstName: 'Test',
        username: 'testuser',
        isPremium: true,
      },
      hash: 'mock-hash',
      authDate: new Date(),
    },
    version: '7.2',
    platform: 'tdesktop',
  });
}
```

---

## 4. Features to Expose in Mini App

### Priority 1: Critical (Core Moderation)

| Feature | Current Command | UI Type |
|---------|-----------------|---------|
| **Locks Management** | `/lock`, `/unlock`, `/locks` | Toggle grid (47 types in 6 categories) |
| **Blocklist CRUD** | `/addblock`, `/delblock`, `/blocklist` | List with inline editing |
| **Chat Selection** | `/connect` | Dropdown selector |
| **Warning Config** | Session-based | Sliders + dropdowns |

### Priority 2: High (Frequent Use)

| Feature | Current Command | UI Type |
|---------|-----------------|---------|
| **Channel Reply** | `/setreply`, `/replybutton`, etc. | Text editor + button builder |
| **Service Messages** | `/cleanservice` | ON/OFF toggle |
| **Log Channel** | `/setlogchannel` | Channel ID input |
| **Collection Toggle** | `/enable`, `/disable` | ON/OFF toggle |

### Priority 3: Medium (Advanced)

| Feature | Current Command | UI Type |
|---------|-----------------|---------|
| **Locks Allowlist** | (not exposed) | URL/domain list editor |
| **Punishment History** | (not exposed) | Searchable log |
| **User Lookup** | (not exposed) | Search by username/ID |
| **Rose Import** | Document upload | File upload |

### Priority 4: Low (Nice to Have)

| Feature | Current Command | UI Type |
|---------|-----------------|---------|
| **Statistics** | `/stats` | Dashboard charts |
| **Export Settings** | (not exposed) | Download button |
| **Bulk Operations** | (not exposed) | Multi-select |

---

## 5. Backend API Design

### Authentication Flow

```
1. Mini App sends: Authorization: tma {initDataRaw}
2. Backend validates HMAC-SHA256 signature with bot token
3. Backend checks auth_date (< 1 hour old)
4. Backend extracts user ID from validated data
5. Backend checks isAdmin(userId, chatId) for chat operations
```

### New Endpoints Needed

```
GET    /api/v1/miniapp/chats                  - List user's admin chats
GET    /api/v1/miniapp/chats/{id}/settings    - Get chat settings
PUT    /api/v1/miniapp/chats/{id}/settings    - Update settings

GET    /api/v1/miniapp/chats/{id}/blocklist   - List blocklist patterns
POST   /api/v1/miniapp/chats/{id}/blocklist   - Add pattern
DELETE /api/v1/miniapp/chats/{id}/blocklist/{patternId}

GET    /api/v1/miniapp/chats/{id}/locks       - Get lock settings
PUT    /api/v1/miniapp/chats/{id}/locks       - Update locks
GET    /api/v1/miniapp/chats/{id}/locks/allowlist
POST   /api/v1/miniapp/chats/{id}/locks/allowlist
DELETE /api/v1/miniapp/chats/{id}/locks/allowlist/{id}

GET    /api/v1/miniapp/chats/{id}/channel-reply
PUT    /api/v1/miniapp/chats/{id}/channel-reply
```

### Response Format

```kotlin
data class ChatSettingsResponse(
    val chatId: Long,
    val chatTitle: String,
    val collectionEnabled: Boolean,
    val cleanServiceEnabled: Boolean,
    val maxWarnings: Int,
    val warningTtlHours: Int,
    val thresholdAction: PunishmentType,
    val thresholdDurationHours: Int,
    val defaultBlocklistAction: PunishmentType,
    val logChannelId: Long?,
    val lockwarnsEnabled: Boolean
)
```

---

## 6. Database Entities Reference

### Existing Tables to Query

| Table | Purpose | Key Fields |
|-------|---------|------------|
| `chat_settings` | Basic chat config | `collection_enabled`, `chat_title` |
| `moderation_config` | Moderation settings | `max_warnings`, `threshold_action`, `log_channel_id` |
| `blocklist_patterns` | Blocklist entries | `pattern`, `match_type`, `action` |
| `lock_settings` | Locks (JSONB) | `locked_types`, `lockwarns_enabled` |
| `lock_allowlist` | Allowed items | `chat_id`, `item_type`, `value` |
| `channel_reply_settings` | Auto-reply config | `reply_text`, `buttons_json`, `media_file_id` |

### Lock Types (47 total in 6 categories)

```kotlin
enum class LockCategory {
    CONTENT,    // PHOTO, VIDEO, GIF, AUDIO, VOICE, DOCUMENT, STICKER, POLL, etc.
    FORWARD,    // FORWARD, FORWARD_USER, FORWARD_BOT, etc.
    URL,        // URL, TELEGRAM_LINK, EMAIL, PHONE
    TEXT,       // TEXT_TOO_LONG, RTLO, ZALGO, etc.
    ENTITY,     // MENTION, BOT_COMMAND, HASHTAG, CASHTAG, etc.
    OTHER       // DICE, GAME, STORY, TOPIC_CHANGE, etc.
}
```

---

## 7. Security Considerations

### InitData Validation (CRITICAL)

```kotlin
fun validateInitData(initDataRaw: String): TelegramUser? {
    val params = parseInitData(initDataRaw)
    val hash = params["hash"] ?: return null

    // Create data check string (sorted params without hash)
    val dataCheckString = params
        .filterKeys { it != "hash" }
        .toSortedMap()
        .map { "${it.key}=${it.value}" }
        .joinToString("\n")

    // Validate signature
    val secretKey = hmacSha256("WebAppData".toByteArray(), botToken.toByteArray())
    val calculatedHash = hmacSha256(secretKey, dataCheckString.toByteArray()).toHex()

    if (calculatedHash != hash) return null

    // Check expiry (1 hour)
    val authDate = params["auth_date"]?.toLongOrNull() ?: return null
    if (System.currentTimeMillis() / 1000 - authDate > 3600) return null

    return parseUser(params["user"]!!)
}
```

### Admin Verification

Always check `AdminCacheService.isAdmin(userId, chatId)` before any chat operation.

### Input Validation

- Blocklist patterns: max 500 chars
- Reply text: max 4096 chars
- Buttons: max 10 per message
- URLs: validate format

---

## 8. Project Structure Recommendation

```
chatkeep/
├── src/main/kotlin/ru/andvl/chatkeep/
│   ├── api/                          # NEW: REST controllers
│   │   ├── MiniAppController.kt
│   │   ├── MiniAppSettingsController.kt
│   │   ├── MiniAppBlocklistController.kt
│   │   └── MiniAppLocksController.kt
│   ├── auth/                         # NEW: Auth service
│   │   └── TelegramAuthService.kt
│   └── ... existing code
│
├── mini-app/                         # NEW: Frontend project
│   ├── src/
│   │   ├── components/
│   │   ├── hooks/
│   │   ├── pages/
│   │   ├── services/
│   │   ├── stores/
│   │   └── types/
│   ├── package.json
│   ├── vite.config.ts
│   └── tsconfig.json
│
├── docker-compose.yml                # Update to include mini-app build
└── ...
```

---

## 9. Implementation Roadmap

### Phase 1: Foundation (Week 1-2)
1. Create `mini-app/` directory with React + Vite template
2. Implement `TelegramAuthService` for initData validation
3. Create base `MiniAppController` with `/chats` endpoint
4. Set up local development environment with TMA Studio

### Phase 2: Core Settings (Week 2-3)
5. Implement chat selector component
6. Build settings page with toggles
7. Add locks management grid UI
8. Create blocklist CRUD interface

### Phase 3: Advanced Features (Week 3-4)
9. Channel reply configuration UI
10. Warning threshold settings
11. Punishment history viewer
12. User lookup feature

### Phase 4: Polish (Week 4-5)
13. Theme customization
14. Error handling and loading states
15. Responsive design testing
16. Device testing via Cloudflare Tunnel

### Phase 5: Deployment (Week 5)
17. Build pipeline for mini-app
18. Deploy to static hosting (Cloudflare Pages)
19. Configure bot menu button
20. Production testing

---

## 10. Skills Added

Two new skills were created:

### `.claude/skills/telegram-mini-apps/SKILL.md`
- WebApp API reference
- SDK usage patterns
- Authentication flow
- Component library guide

### `.claude/skills/react-vite/SKILL.md`
- Project structure
- Component patterns
- Custom hooks
- API client setup
- Mock environment

---

## 11. Team Prompt Updates

Updated `.claude/commands/team.md`:
- Added **frontend-developer** agent (10th agent)
- Defined specializations for backend vs frontend work
- Updated project description to include Mini App

---

## 12. Open Questions for Implementation

1. **Separate repo or monorepo?**
   - Recommendation: Keep in same repo under `mini-app/` directory

2. **Authentication approach?**
   - Option A: Validate initData on every request (simpler)
   - Option B: Exchange initData for JWT token (better UX for long sessions)
   - Recommendation: Start with Option A, add JWT later if needed

3. **Real-time updates?**
   - Not critical for v1
   - Can add WebSocket later for multi-admin scenarios

4. **Hosting for Mini App?**
   - Options: Cloudflare Pages, Vercel, Netlify, self-hosted
   - Recommendation: Cloudflare Pages (free, fast, simple)

---

## 13. Resources

### Official Documentation
- [Telegram Mini Apps Core Docs](https://core.telegram.org/bots/webapps)
- [Telegram Mini Apps Community Docs](https://docs.telegram-mini-apps.com/)
- [Test Environment](https://docs.telegram-mini-apps.com/platform/test-environment)
- [Debugging Guide](https://docs.telegram-mini-apps.com/platform/debugging)

### SDK & Libraries
- [@telegram-apps/sdk](https://www.npmjs.com/package/@telegram-apps/sdk)
- [@telegram-apps/sdk-react](https://www.npmjs.com/package/@telegram-apps/sdk-react)
- [@telegram-apps/ui](https://www.npmjs.com/package/@telegram-apps/ui)
- [React Template](https://github.com/Telegram-Mini-Apps/reactjs-template)

### Development Tools
- [TMA Studio](https://github.com/erfanmola/TMA-Studio)
- [Cloudflare Tunnel](https://www.cloudflare.com/products/tunnel/)
- [Eruda Console](https://github.com/liriliri/eruda)

---

## Conclusion

The research confirms that building a Telegram Mini App for Chatkeep bot configuration is feasible and well-supported. The recommended approach:

1. **Frontend**: React + TypeScript + Vite + @telegram-apps/sdk
2. **Backend**: Add REST endpoints with initData authentication
3. **Development**: TMA Studio + mock SDK for browser-based testing
4. **Deployment**: Cloudflare Pages for static hosting

All current bot configuration features can be exposed via visual UI, providing a better user experience than command-based configuration.

**Next Steps**: Start implementation following the roadmap above.

---

*Report generated by: Intelligent Engineering Manager (EM)*
*Research agents used: analyst (2), tech-researcher (2)*
