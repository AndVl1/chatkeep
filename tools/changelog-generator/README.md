# Changelog Generator

LLM-powered changelog generator for Pull Requests. Uses OpenRouter API to analyze changes and generate human-readable changelogs in Russian.

## Features

- Analyzes git diffs and commit messages
- Classifies changes into "Production" (user-facing) and "Internal" (infrastructure/dev)
- Generates structured changelog in Markdown format
- Updates GitHub PR body automatically
- Outputs Telegram-friendly format for notifications
- Fallback to raw commit list if LLM fails
- Retry logic for resilience

## Prerequisites

- Java 17+
- Git repository with changes between two branches
- OpenRouter API key
- GitHub token with PR write permissions

## Environment Variables

Required:
- `OPENROUTER_API_KEY` - API key from OpenRouter
- `GITHUB_TOKEN` - GitHub personal access token
- `PR_NUMBER` - Pull request number
- `GITHUB_REPOSITORY` - Repository in format `owner/repo`
- `HEAD_BRANCH` - Branch with changes

Optional:
- `CHANGELOG_MODEL` - LLM model to use (default: `deepseek/deepseek-chat-v3-0324`)
- `BASE_BRANCH` - Base branch to compare against (default: `main`)
- `MODE` - Operation mode: `generate` or `check-update` (default: `generate`)
- `TOKEN_LIMIT_WARNING` - Warning threshold for token count (default: `50000`)

## CLI Arguments

Arguments override environment variables:
- `--pr-number <number>` - PR number
- `--repo <owner/repo>` - GitHub repository
- `--base-branch <branch>` - Base branch
- `--head-branch <branch>` - Head branch
- `--mode <mode>` - Operation mode

## Build

```bash
./gradlew shadowJar
```

This creates `build/libs/changelog-generator.jar` (fat JAR with all dependencies).

## Usage

### From CI/CD (GitHub Actions)

```yaml
- name: Generate Changelog
  env:
    OPENROUTER_API_KEY: ${{ secrets.OPENROUTER_API_KEY }}
    GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
    PR_NUMBER: ${{ github.event.pull_request.number }}
    GITHUB_REPOSITORY: ${{ github.repository }}
    HEAD_BRANCH: ${{ github.head_ref }}
    BASE_BRANCH: main
  run: |
    java -jar tools/changelog-generator/build/libs/changelog-generator.jar
```

### Local Testing

```bash
# Set environment variables
export OPENROUTER_API_KEY="your-key"
export GITHUB_TOKEN="your-token"
export PR_NUMBER="123"
export GITHUB_REPOSITORY="owner/repo"
export HEAD_BRANCH="feat/my-feature"
export BASE_BRANCH="main"

# Run from project root
cd tools/changelog-generator
java -jar build/libs/changelog-generator.jar
```

### Using CLI Arguments

```bash
java -jar changelog-generator.jar \
  --pr-number 123 \
  --repo owner/repo \
  --head-branch feat/my-feature \
  --base-branch main
```

## How It Works

1. **Gathers context**:
   - Lists all changed files with additions/deletions
   - Gets commit messages
   - Extracts diffs for key source files (prioritizes `.kt`, `.java`, `.ts`, SQL, Docker, YAML)
   - Excludes generated files, lock files, build artifacts

2. **Calls LLM**:
   - Sends context to OpenRouter API
   - Uses system prompt to instruct classification
   - Requests JSON response with production/internal changes

3. **Updates PR**:
   - Formats changelog in Markdown with HTML markers
   - Updates PR body via GitHub API
   - If markers exist, replaces content; otherwise appends

4. **Outputs results**:
   - Prints Telegram-friendly format (production changes only)
   - Outputs full JSON for further processing

## Changelog Format

### PR Body

```markdown
<!-- CHANGELOG_START -->
## Changelog

**Продакшн-изменения:**
- Добавлена функция поиска: поддержка регулярных выражений
- Исправлена ошибка с уведомлениями

**Внутренние изменения:**
- Обновлены зависимости: Spring Boot 4.0.1
- Добавлены тесты для сервиса пользователей

> Улучшение функциональности поиска и обновление зависимостей
> Сгенерировано автоматически с помощью LLM
<!-- CHANGELOG_END -->
```

### Telegram Notification

```
🚀 PR #42: feat/search

Продакшн-изменения:
• Добавлена функция поиска: поддержка регулярных выражений
• Исправлена ошибка с уведомлениями
```

## Testing

Run tests:

```bash
./gradlew test
```

Test coverage:
- Markdown formatting (PR, Telegram, fallback)
- JSON schema parsing
- Edge cases (empty lists, missing fields)

## Troubleshooting

### LLM API Errors

- Check `OPENROUTER_API_KEY` is valid
- Verify model name is correct (default: `deepseek/deepseek-chat-v3-0324`)
- Check OpenRouter account has credits

### GitHub API Errors

- Ensure `GITHUB_TOKEN` has `repo` scope
- Verify PR number and repository name are correct
- Check token permissions allow PR updates

### No JSON in Response

- LLM may return text before/after JSON - parser handles this
- If parse fails, check logs for actual response
- Try different model if current one is unreliable

### Fallback Activated

Tool falls back to raw commit list if:
- LLM API fails after 2 retries
- Response cannot be parsed as JSON
- Any exception during generation

## Architecture

```
ru.andvl.chatkeep.changelog/
├── config/
│   └── Config.kt              # Configuration from env/args
├── git/
│   └── GitOperations.kt       # Git command execution
├── agent/
│   ├── ChangelogSchema.kt     # Response data models
│   ├── ChangelogTools.kt      # Git wrapper for agent
│   └── ChangelogAgent.kt      # LLM agent using OpenRouter
├── formatter/
│   └── MarkdownFormatter.kt   # Changelog formatting
├── github/
│   └── GitHubClient.kt        # PR update via GitHub API
└── Main.kt                    # Entry point
```

## Future Improvements

- Smart check-update mode (only regenerate if new commits)
- Support for multiple languages (currently Russian)
- Configurable output format
- Integration with other Git platforms (GitLab, Bitbucket)
- Local caching to avoid redundant API calls
- Support for monorepo component detection
