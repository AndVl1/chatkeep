package ru.andvl.chatkeep.changelog.formatter

import org.junit.jupiter.api.Test
import ru.andvl.chatkeep.changelog.agent.ChangelogEntry
import ru.andvl.chatkeep.changelog.agent.ChangelogResponse
import kotlin.test.assertContains
import kotlin.test.assertTrue

class MarkdownFormatterTest {

    @Test
    fun `formatForPR with both production and internal changes`() {
        val changelog = ChangelogResponse(
            production = listOf(
                ChangelogEntry("Добавлена новая функция поиска", "Поддержка регулярных выражений"),
                ChangelogEntry("Исправлена ошибка с уведомлениями")
            ),
            internal = listOf(
                ChangelogEntry("Обновлены зависимости", "Spring Boot 4.0.1"),
                ChangelogEntry("Добавлены тесты для сервиса пользователей")
            ),
            summary = "Улучшение функциональности поиска и обновление зависимостей"
        )

        val result = MarkdownFormatter.formatForPR(changelog)

        assertContains(result, "<!-- CHANGELOG_START -->")
        assertContains(result, "<!-- CHANGELOG_END -->")
        assertContains(result, "## Changelog")
        assertContains(result, "**Продакшн-изменения:**")
        assertContains(result, "**Внутренние изменения:**")
        assertContains(result, "Добавлена новая функция поиска")
        assertContains(result, "Поддержка регулярных выражений")
        assertContains(result, "Обновлены зависимости")
        assertContains(result, "Улучшение функциональности поиска и обновление зависимостей")
        assertContains(result, "Сгенерировано автоматически с помощью LLM")
    }

    @Test
    fun `formatForPR with empty production changes`() {
        val changelog = ChangelogResponse(
            production = emptyList(),
            internal = listOf(
                ChangelogEntry("Рефакторинг кода")
            ),
            summary = "Технические улучшения"
        )

        val result = MarkdownFormatter.formatForPR(changelog)

        assertContains(result, "<!-- CHANGELOG_START -->")
        assertContains(result, "**Внутренние изменения:**")
        assertTrue(result.contains("Рефакторинг кода"))
        assertTrue(!result.contains("**Продакшн-изменения:**"))
    }

    @Test
    fun `formatForPR with empty internal changes`() {
        val changelog = ChangelogResponse(
            production = listOf(
                ChangelogEntry("Новая фича")
            ),
            internal = emptyList(),
            summary = "Добавлена новая фича"
        )

        val result = MarkdownFormatter.formatForPR(changelog)

        assertContains(result, "**Продакшн-изменения:**")
        assertContains(result, "Новая фича")
        assertTrue(!result.contains("**Внутренние изменения:**"))
    }

    @Test
    fun `formatForTelegram with production changes`() {
        val changelog = ChangelogResponse(
            production = listOf(
                ChangelogEntry("Добавлена функция поиска"),
                ChangelogEntry("Исправлен баг с авторизацией", "Теперь работает корректно")
            ),
            internal = listOf(
                ChangelogEntry("Обновлены тесты")
            ),
            summary = "Улучшения поиска и исправление багов"
        )

        val result = MarkdownFormatter.formatForTelegram(changelog, 42, "feat/search")

        assertContains(result, "🚀 PR #42: feat/search")
        assertContains(result, "Продакшн-изменения:")
        assertContains(result, "• Добавлена функция поиска")
        assertContains(result, "• Исправлен баг с авторизацией: Теперь работает корректно")
        assertTrue(!result.contains("Обновлены тесты"))
    }

    @Test
    fun `formatForTelegram with no production changes`() {
        val changelog = ChangelogResponse(
            production = emptyList(),
            internal = listOf(
                ChangelogEntry("Рефакторинг")
            ),
            summary = "Технические улучшения"
        )

        val result = MarkdownFormatter.formatForTelegram(changelog, 15, "refactor/cleanup")

        assertContains(result, "🚀 PR #15: refactor/cleanup")
        assertContains(result, "Нет продакшн-изменений")
    }

    @Test
    fun `formatFallback with commit list`() {
        val commits = listOf(
            "abc123 feat: add search functionality",
            "def456 fix: resolve authentication bug",
            "ghi789 refactor: clean up service layer"
        )

        val result = MarkdownFormatter.formatFallback(commits)

        assertContains(result, "<!-- CHANGELOG_START -->")
        assertContains(result, "<!-- CHANGELOG_END -->")
        assertContains(result, "**Коммиты:**")
        assertContains(result, "abc123 feat: add search functionality")
        assertContains(result, "def456 fix: resolve authentication bug")
        assertContains(result, "ghi789 refactor: clean up service layer")
        assertContains(result, "LLM недоступен")
    }
}
