# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Chatkeep** is a Spring Boot application written in Kotlin. It's a reactive web service that uses:
- Spring Boot 4.0.1
- Kotlin 2.2.21
- Spring Data JDBC for database access
- Spring WebClient and RestClient for HTTP communication
- Kotlin coroutines with Reactor for reactive programming

## Common Commands

### Build and Run
```bash
# Build the project
./gradlew build

# Run the application
./gradlew bootRun

# Clean and build
./gradlew clean build
```

### Testing
```bash
# Run all tests
./gradlew test

# Run a specific test class
./gradlew test --tests "ru.andvl.chatkeep.ChatkeepApplicationTests"

# Run tests with more detailed output
./gradlew test --info

# Run tests continuously (useful during development)
./gradlew test --continuous
```

### Other Gradle Tasks
```bash
# List all available tasks
./gradlew tasks

# Check for dependency updates
./gradlew dependencyUpdates

# Generate a bootJar
./gradlew bootJar
```

## Architecture

### Technology Stack
- **Language**: Kotlin with strict compiler options (JSR-305 strict mode)
- **Framework**: Spring Boot 4.0.1 with Spring Data JDBC
- **Reactive Programming**: Project Reactor with Kotlin coroutines support
- **HTTP Clients**: Both RestClient (blocking) and WebClient (reactive) are available
- **Java Version**: Java 17
- **JSON**: Jackson with Kotlin module support

### Package Structure
- Base package: `ru.andvl.chatkeep`
- All application code is under `src/main/kotlin/ru/andvl/chatkeep/`
- Tests are under `src/test/kotlin/ru/andvl/chatkeep/`

### Key Dependencies
The application has both blocking (Spring Data JDBC, RestClient) and reactive (WebClient, Reactor) capabilities, allowing for hybrid approaches where needed.

## Development Notes

### Kotlin Compiler Configuration
The project uses strict compiler settings:
- JSR-305 strict mode enabled
- Parameter property annotation target set by default

### Testing Framework
- JUnit 5 (Jupiter)
- Spring Boot Test with `@SpringBootTest`
- Kotlinx Coroutines Test support

## Current Implementation

### Telegram Bot
The project implements a Telegram bot using KTgBotAPI library:
- **ChatkeepBot** - main bot service with long polling
- **GroupMessageHandler** - saves text messages from group chats
- **ChatMemberHandler** - auto-registers chats when bot is added
- **AdminCommandHandler** - handles admin commands in private messages

### Domain Layer
- **ChatMessage** - stored message entity (text, user info, chat ID, timestamp)
- **ChatSettings** - chat configuration (collection enabled/disabled)
- **MessageService** - message persistence logic
- **ChatService** - chat registration and settings
- **AdminService** - statistics and admin operations

### Database
- PostgreSQL with Flyway migrations
- Tables: `messages`, `chat_settings`
- Indexes on chat_id, user_id, message_date

### Environment Variables
```
TELEGRAM_BOT_TOKEN - Bot token from @BotFather
DB_HOST, DB_PORT, DB_NAME, DB_USERNAME, DB_PASSWORD - PostgreSQL connection
LOG_LEVEL - Logging level (default: INFO)
```

## Conventions

### Handler Pattern
Bot handlers implement `Handler` interface with `suspend fun BehaviourContext.register()`.
Each handler is a Spring `@Component` auto-discovered by `ChatkeepBot`.

### Coroutines
Use `withContext(Dispatchers.IO)` when calling blocking Spring Data operations from handlers.

### Error Handling
Catch exceptions in handlers, log errors, don't crash the bot.

## Moderation Features Reference

When implementing or improving moderation features, use **MissRose (Rose Bot)** as the primary reference:
- **Repository**: https://github.com/MRK-YT/Rose-Bot
- **Documentation**: Use DeepWiki MCP to query the repository for implementation details

Rose Bot is a mature Telegram moderation bot with well-designed features including:
- User warnings and bans
- Blocklist/filter management
- Anti-spam and anti-flood
- Welcome messages
- Admin management

To get implementation details, use:
```
mcp__deepwiki__ask_question with repoName="MRK-YT/Rose-Bot"
```

## Git Workflow

**IMPORTANT: ALL features and fixes MUST be developed in separate branches.**

### Branch Naming Convention

Use descriptive branch names following these patterns:
- `feat/<feature-name>` - for new features
- `fix/<bug-description>` - for bug fixes
- `refactor/<what-refactored>` - for refactoring
- `docs/<documentation-change>` - for documentation updates
- `test/<test-description>` - for test additions

Examples:
```
feat/telegram-message-search
fix/database-connection-timeout
refactor/repository-layer
docs/api-documentation
test/integration-tests
```

### Feature Development Workflow

1. **Start from main:**
   ```bash
   git checkout main
   git pull origin main
   ```

2. **Create feature branch:**
   ```bash
   git checkout -b feat/your-feature-name
   ```

3. **Develop incrementally:**
   - Make small, logical commits
   - Each commit should compile and tests should pass
   - Use conventional commit messages (feat:, fix:, docs:, etc.)

4. **Before creating PR:**
   ```bash
   # Ensure tests pass
   ./gradlew test

   # Ensure build succeeds
   ./gradlew build
   ```

5. **Create Pull Request:**
   - Push branch: `git push origin feat/your-feature-name`
   - Create PR to merge into `main`
   - Wait for review and CI/CD checks

### Commit Message Convention

Follow conventional commits format:
```
<type>: <short description>

[optional detailed description]

[optional footer]
```

Types: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`

**CRITICAL: Never commit directly to main. Always use feature branches.**

## Mini App Testing Environment Setup

**MANDATORY for autonomous development/testing of the Mini App.**

When testing Mini App functionality (manual-qa, frontend changes verification), follow this setup procedure:

### Step 1: Start Services

```bash
# Terminal 1: Start backend (requires .env with all variables)
cd /Users/a.vladislavov/personal/chatkeep
./gradlew bootRun

# Terminal 2: Start frontend dev server
cd /Users/a.vladislavov/personal/chatkeep/mini-app
npm run dev
```

Verify both are running:
- Backend: http://localhost:8080 (check health endpoint)
- Frontend: http://localhost:5173

### Step 2: Create Cloudflare Tunnel

```bash
# Create tunnel to expose local frontend
cloudflared tunnel --url http://localhost:5173
```

**Save the generated URL** (format: `https://<random-words>.trycloudflare.com`)

### Step 3: Update BotFather Settings (via MCP Chrome)

Open https://t.me/BotFather and configure the bot:

1. **Send**: `/mybots` → Select your bot
2. **Bot Settings** → **Menu Button** → Update URL to new tunnel link
3. **Bot Settings** → **Configure Mini App** (if available):
   - Update Mini App URL
   - Update Direct Link URL
4. **Bot Settings** → **Domain** → Add `trycloudflare.com` if not present

### Step 4: Update Repository Configs (if URL changed)

Search and replace old trycloudflare URL in:
- Any hardcoded URLs in config files
- Test configurations
- Documentation examples

```bash
# Find files with old URL
grep -r "trycloudflare.com" --include="*.ts" --include="*.json" --include="*.md"
```

### Step 5: Verify Setup

1. Open Mini App via Telegram (web.telegram.org or mobile app)
2. Check browser console for errors
3. Verify API calls go through (Network tab)
4. Check backend logs for incoming requests

### Troubleshooting

| Issue | Solution |
|-------|----------|
| CORS errors | Verify `allowedHosts` in `vite.config.ts` includes `.trycloudflare.com` |
| 401 Unauthorized | Check JWT_SECRET in .env, verify auth header sent |
| Tunnel disconnected | Restart `cloudflared tunnel` command |
| Mini App not loading | Clear Telegram cache, restart app |

### For Automated Testing (manual-qa agent)

When testing via MCP Chrome:
1. Navigate to: `https://web.telegram.org/k/?account=2`
2. Open the bot chat
3. Click Mini App button to launch
4. Use `read_network_requests` to verify API calls
5. Use `read_console_messages` to check for JS errors
6. Verify adminbotlog channel receives change notifications

## Global Execution Rules (IMPORTANT)

These rules apply to the main agent and ALL sub-agents.

### Bash Command Invariants

Whenever generating or executing a bash command:

- Always output a SINGLE, complete, standalone command.
- The command MUST start with an executable (e.g. `./gradlew`, `git`, `docker`, `mkdir`, `bash`).
- NEVER start a command with flags (`-`, `--`) or shell operators (`&&`, `||`, `|`, `>`, `<`, `!`).
- NEVER output partial commands, continuations, or fragments.
- NEVER assume previous shell context (no implicit `cd`, no command chaining).
- NEVER use history expansion (`!`).

If multiple steps are required, wrap them explicitly in:

```bash
bash -c "<full command sequence>"
```

Sub-agent Safety

Sub-agents MUST follow the same bash command rules.
If a sub-agent is not explicitly responsible for executing commands, it MUST NOT output bash commands at all.
