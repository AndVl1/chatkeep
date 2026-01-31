package ru.andvl.chatkeep.changelog.formatter

import ru.andvl.chatkeep.changelog.agent.ChangelogResponse

object MarkdownFormatter {

    fun formatForPR(changelog: ChangelogResponse): String = buildString {
        appendLine("<!-- CHANGELOG_START -->")
        appendLine("## Changelog")
        appendLine()

        if (changelog.production.isNotEmpty()) {
            appendLine("**Продакшн-изменения:**")
            changelog.production.forEach { entry ->
                append("- ${entry.title}")
                if (entry.details != null) {
                    append(": ${entry.details}")
                }
                appendLine()
            }
            appendLine()
        }

        if (changelog.internal.isNotEmpty()) {
            appendLine("**Внутренние изменения:**")
            changelog.internal.forEach { entry ->
                append("- ${entry.title}")
                if (entry.details != null) {
                    append(": ${entry.details}")
                }
                appendLine()
            }
            appendLine()
        }

        appendLine("> ${changelog.summary}")
        appendLine("> Сгенерировано автоматически с помощью LLM")
        append("<!-- CHANGELOG_END -->")
    }

    fun formatForTelegram(
        changelog: ChangelogResponse,
        prNumber: Int,
        branchName: String
    ): String = buildString {
        appendLine("🚀 PR #$prNumber: $branchName")
        appendLine()

        if (changelog.production.isNotEmpty()) {
            appendLine("Продакшн-изменения:")
            changelog.production.forEach { entry ->
                append("• ${entry.title}")
                if (entry.details != null) {
                    append(": ${entry.details}")
                }
                appendLine()
            }
        } else {
            appendLine("Нет продакшн-изменений")
        }
    }.trim()

    fun formatFallback(commits: List<String>): String = buildString {
        appendLine("<!-- CHANGELOG_START -->")
        appendLine("## Changelog")
        appendLine()
        appendLine("**Коммиты:**")
        commits.forEach { commit ->
            appendLine("- $commit")
        }
        appendLine()
        appendLine("> Автоматически сгенерированный список коммитов (LLM недоступен)")
        append("<!-- CHANGELOG_END -->")
    }
}
