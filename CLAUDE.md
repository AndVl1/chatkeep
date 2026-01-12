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

## Documentation Sources

When researching libraries and frameworks, use these MCP tools:

### Context7 MCP
For up-to-date documentation and code examples of any library/framework:
```
# First, resolve library ID
mcp__context7__resolve-library-id with libraryName="ktgbotapi" query="how to handle callbacks"

# Then query documentation
mcp__context7__query-docs with libraryId="/insanusmokrassar/ktgbotapi" query="callback handling"
```

Use Context7 for:
- Official documentation with code examples
- API references
- Best practices and patterns
- Version-specific information

### DeepWiki MCP
For understanding GitHub repositories and their architecture:
```
mcp__deepwiki__ask_question with repoName="owner/repo" question="how does X work?"
mcp__deepwiki__read_wiki_structure with repoName="owner/repo"
```

Use DeepWiki for:
- Understanding project architecture
- Finding implementation patterns in open-source projects
- Learning how specific features are implemented

### When to Use Which
| Need | Tool |
|------|------|
| Library docs (Spring, React, ktgbotapi) | Context7 |
| Framework patterns | Context7 |
| GitHub repo analysis | DeepWiki |
| Open-source implementation examples | DeepWiki |
| API reference | Context7 |
| Codebase architecture | DeepWiki |

## Mobile Admin App (chatkeep-admin)

Admin panel application for Chatkeep built with Kotlin Multiplatform.

### Technology Stack
- **Language**: Kotlin 2.1.0
- **UI**: Compose Multiplatform 1.7.3
- **Navigation**: Decompose 3.2.0
- **HTTP**: Ktor Client 3.1.1
- **Database**: Room 2.7.0 (Android/iOS/Desktop)
- **Preferences**: DataStore 1.1.1
- **Platforms**: Android, iOS, Desktop (JVM), Web (WASM)

### Commands
```bash
cd chatkeep-admin

# Build all
./gradlew assemble

# Run Android
./gradlew :composeApp:installDebug

# Run Desktop
./gradlew :composeApp:run

# iOS: open iosApp/iosApp.xcodeproj in Xcode
```

### Structure
- `core/` - shared modules (common, network, ui, data, database)
- `feature/` - feature modules with api/impl separation
- `composeApp/` - platform entry points

---

## Mobile Development (KMP)

The project includes infrastructure for Kotlin Multiplatform mobile development.

### Skills

| Skill | Description |
|-------|-------------|
| `kmp` | KMP fundamentals, expect/actual, source sets, multi-module setup |
| `compose` | Compose Multiplatform UI patterns, theming, resources |
| `decompose` | Navigation, components, lifecycle, state preservation |
| `metro-di-mobile` | Metro DI for compile-time dependency injection |

### Agents

| Agent | Description |
|-------|-------------|
| `developer-mobile` | Implements KMP features with Compose UI |
| `init-mobile` | Creates new KMP project with full structure |

### Commands

- `/init-mobile [project-name]` - Creates a complete KMP Compose Multiplatform project

### Technology Stack

- **Platforms**: Android, iOS, Desktop (JVM), Web (WASM)
- **UI**: Compose Multiplatform
- **Navigation**: Decompose
- **DI**: Metro (compile-time)
- **HTTP**: Ktor Client
- **Database**: Room (Android/iOS/JVM)
- **Preferences**: DataStore

### Project Structure Pattern

```
project/
├── core/
│   ├── common/      # Utilities, Result types
│   ├── data/        # DataStore
│   ├── database/    # Room (mobile + desktop)
│   ├── network/     # Ktor client
│   └── ui/          # Theme, components
├── feature/
│   └── [name]/
│       ├── api/     # Public interfaces
│       └── impl/    # Implementation + UI
└── composeApp/      # Platform entry points
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

## Technical Debt Tracking

**MANDATORY for all agents during research and development.**

When agents (main or sub-agents) discover code that needs refactoring, improvements, or has issues that are not directly related to the current task, they MUST:

1. **Document the finding** in `docs/debt/tech-debt.md`
2. **Commit the change** to the repository

### What to Track

- Code smells and anti-patterns discovered
- Missing tests or test coverage gaps
- Performance bottlenecks identified
- Security concerns noticed
- Deprecated dependencies or APIs
- Duplicated code that should be extracted
- Missing documentation
- Inconsistent coding patterns
- TODO/FIXME comments found in code

### Tech Debt Entry Format

```markdown
## [Date] - [Brief Title]

**Found by**: [agent name or "main agent"]
**Location**: [file path and line numbers]
**Priority**: [low/medium/high/critical]
**Category**: [refactor/test/security/performance/docs/other]

**Description**:
[What was found and why it's a problem]

**Suggested Fix**:
[How to address this issue]
```

### Example Entry

```markdown
## 2024-01-07 - Duplicate validation logic in handlers

**Found by**: code-reviewer agent
**Location**: src/main/kotlin/ru/andvl/chatkeep/bot/handlers/
**Priority**: medium
**Category**: refactor

**Description**:
BlocklistManagementHandler and AdminCommandHandler both have similar user permission checking logic that could be extracted to a shared utility.

**Suggested Fix**:
Create a `PermissionUtils` object with reusable permission checking functions.
```

### Rules for Agents

- **DO NOT** fix tech debt during current task unless explicitly requested
- **DO** document all findings immediately when discovered
- **DO** commit tech debt updates as separate commits with message: `docs: add tech debt entry - [brief description]`
- **DO** continue with the main task after documenting

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

### Clarifying Questions

**All clarifying questions MUST be asked via `AskUserQuestionTool`.**

- NEVER ask questions in plain text output
- ALWAYS use the `AskUserQuestion` tool for any clarifications
- This applies to BOTH main agent and ALL sub-agents
- Provide clear, structured options when possible
