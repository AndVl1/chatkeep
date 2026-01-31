# Спецификация: LLM-генератор Changelog'ов

## Описание

Инструмент для автоматической генерации человекочитаемых changelog'ов при создании и обновлении Pull Request'ов в production-ветку. Использует Koog-агента с tool calling для анализа изменений и классификации их на пользовательские (продакшн) и внутренние.

## Цели

- Автоматически генерировать структурированный changelog при создании/обновлении PR
- Классифицировать изменения на **Продакшн** (видимые пользователям) и **Внутренние** (инфра, админка, рефакторинг)
- Публиковать changelog в описание PR (секция ## Changelog)
- Отправлять продакшн-изменения в Telegram (опционально, при наличии секретов)
- Умно решать, нужно ли перегенерировать changelog при обновлении PR

## Технический стек

| Компонент | Технология |
|-----------|------------|
| Язык | Kotlin |
| AI-фреймворк | Koog 0.6.0 |
| LLM-провайдер | OpenRouter |
| Модель (по умолчанию) | deepseek/deepseek-v3.2 |
| Сборка | Gradle (Shadow JAR) |
| CI/CD | GitHub Actions |

## Расположение в проекте

Отдельный Gradle-модуль в chatkeep:

```
tools/
  changelog-generator/
    build.gradle.kts
    src/
      main/kotlin/ru/andvl/chatkeep/changelog/
        Main.kt                    # Entry point (CLI)
        agent/
          ChangelogAgent.kt        # Koog-агент с промптом и tools
          ChangelogTools.kt        # Определение tools для агента
          ChangelogSchema.kt       # JSON schema ответа
        git/
          GitOperations.kt         # Git-команды (diff, log, file content)
        github/
          GitHubClient.kt          # GitHub API (обновление PR body)
        telegram/
          TelegramNotifier.kt      # Отправка в Telegram (опционально)
        formatter/
          MarkdownFormatter.kt     # JSON -> Markdown рендеринг
        config/
          Config.kt                # Конфигурация из env variables
      test/kotlin/ru/andvl/chatkeep/changelog/
        formatter/
          MarkdownFormatterTest.kt
        agent/
          ChangelogSchemaTest.kt
```

## Koog-агент

### Режим работы

Полноценный агент с tool calling. Агент сам решает какие файлы анализировать для генерации changelog'а.

### Доступные tools

| Tool | Описание | Параметры |
|------|----------|-----------|
| `list_changed_files` | Список изменённых файлов с количеством добавленных/удалённых строк | - |
| `get_file_diff` | Получить diff конкретного файла | `path: String` |
| `get_commit_messages` | Список сообщений коммитов между ветками | - |
| `get_file_content` | Получить текущее содержимое файла | `path: String` |

### Системный промпт агента

Агент получает инструкции:
- Проанализировать изменения в PR
- Классифицировать каждое изменение как **Продакшн** или **Внутреннее**
- Продакшн: изменения, которые увидят конечные пользователи (новые фичи бота, изменения в поведении, баг-фиксы пользовательских сценариев)
- Внутренние: инфраструктура, CI/CD, рефакторинг, тесты, админка, документация
- Вернуть результат в формате JSON schema
- Язык: русский

### JSON Schema ответа

```json
{
  "production": [
    {
      "title": "Краткое описание изменения",
      "details": "Опциональное подробное описание"
    }
  ],
  "internal": [
    {
      "title": "Краткое описание изменения",
      "details": "Опциональное подробное описание"
    }
  ],
  "summary": "Одно предложение — общая суть PR"
}
```

## GitHub Actions Workflow

### Триггер

```yaml
on:
  pull_request:
    types: [opened, synchronize]
    branches: [main, production]
```

### Логика

1. **При `opened`**: всегда генерировать changelog
2. **При `synchronize`** (push в PR):
   - Собрать список новых коммитов с последней генерации
   - Отправить в LLM с вопросом: "Нужно ли обновлять changelog на основе этих изменений?"
   - Если LLM решает что нужно — перегенерировать
   - Если нет — пропустить

### Сборка JAR

- Использовать GitHub Actions cache для JAR-артефакта
- Ключ кэша: hash файлов модуля `tools/changelog-generator/`
- При изменениях в модуле — пересобрать JAR
- При отсутствии изменений — использовать кэшированный JAR

### Секреты и переменные

| Имя | Тип | Описание |
|-----|-----|----------|
| `OPENROUTER_API_KEY` | Secret | API-ключ OpenRouter |
| `CHANGELOG_MODEL` | Variable (опц.) | Модель для генерации (default: `deepseek/deepseek-v3.2`) |
| `TELEGRAM_BOT_TOKEN` | Secret (опц.) | Токен бота для отправки в Telegram |
| `TELEGRAM_CHAT_ID` | Secret (опц.) | ID чата для уведомлений |

## Обновление PR

### Механизм

- Найти секцию `<!-- CHANGELOG_START -->` ... `<!-- CHANGELOG_END -->` в PR body
- Если секция есть — обновить содержимое между маркерами
- Если секции нет — добавить в конец PR body
- Использовать GitHub API: `PATCH /repos/{owner}/{repo}/pulls/{pr_number}`

### Формат секции в PR

```markdown
<!-- CHANGELOG_START -->
## Changelog

**Продакшн-изменения:**
- Добавлена поддержка поиска сообщений по ключевым словам
- Исправлена ошибка отправки уведомлений в больших группах

**Внутренние изменения:**
- Рефакторинг слоя репозиториев
- Обновлены зависимости Spring Boot

> Сгенерировано автоматически с помощью LLM
<!-- CHANGELOG_END -->
```

## Telegram-интеграция

### Условие

Telegram-уведомление отправляется только если:
- Настроены секреты `TELEGRAM_BOT_TOKEN` и `TELEGRAM_CHAT_ID`
- Есть хотя бы одно продакшн-изменение

### Формат сообщения

Отдельный step в GitHub Actions workflow. Отправляет только продакшн-изменения:

```
PR #42: feat/message-search

Продакшн-изменения:
- Добавлена поддержка поиска сообщений по ключевым словам
- Исправлена ошибка отправки уведомлений в больших группах
```

### Реализация

Использовать готовый GitHub Action для Telegram (например, `appleboy/telegram-action`) в отдельном step.

## Обработка ошибок

### Fallback-стратегия

1. При ошибке LLM-вызова — ретрай (1-2 попытки с экспоненциальной задержкой)
2. Если все ретраи неуспешны — fallback на сырой список коммитов в формате:
   ```markdown
   ## Changelog (автогенерация недоступна)

   Список коммитов:
   - abc1234 feat: добавить поиск сообщений
   - def5678 fix: исправить обработку unicode
   ```

### Мягкий лимит токенов

- Логировать предупреждение если суммарное количество input-токенов превышает порог (настраиваемый, default: 50000)
- Не блокировать выполнение, только warning в логах CI

## Тесты

### Unit-тесты

| Тест | Что проверяет |
|------|---------------|
| `MarkdownFormatterTest` | Корректный рендеринг JSON -> Markdown для PR и Telegram |
| `ChangelogSchemaTest` | Парсинг и валидация JSON-ответа от LLM |

### Что мокать

- LLM-вызовы (Koog agent) — мокать ответ агента
- Git-операции — мокать вывод git-команд
- GitHub API — мокать HTTP-вызовы

## CLI-интерфейс

Entry point принимает аргументы:

```
java -jar changelog-generator.jar \
  --pr-number=42 \
  --repo=owner/repo \
  --base-branch=main \
  --head-branch=feat/feature-name \
  --mode=generate|check-update
```

- `generate` — полная генерация changelog
- `check-update` — проверить нужно ли обновлять (для `synchronize` событий)

## Ограничения и допущения

- Инструмент работает только в контексте GitHub Actions (доступ к `GITHUB_TOKEN`, git history)
- Качество changelog'а зависит от качества commit messages
- DeepSeek V3.2 может не идеально классифицировать изменения — допустимо
- OpenRouter может быть временно недоступен — покрыто fallback'ом

## Приоритеты разработки (MVP)

1. Koog-агент с tools и промптом
2. Git-операции (list files, diff, commits)
3. JSON schema парсинг и Markdown-рендеринг
4. GitHub API для обновления PR body
5. GitHub Actions workflow
6. JAR кэширование
7. Telegram step (опционально)
8. Unit-тесты
9. Умное обновление (check-update mode)

## Открытые вопросы

_Нет — все вопросы закрыты в ходе интервью._
