package ru.andvl.chatkeep.changelog.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.tools
import ai.koog.agents.ext.agent.chatAgentStrategy
import ai.koog.prompt.executor.clients.openrouter.OpenRouterLLMClient
import ai.koog.prompt.executor.clients.openrouter.OpenRouterModels
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import kotlinx.serialization.json.Json
import ru.andvl.chatkeep.changelog.config.Config

class ChangelogAgent(
    private val config: Config,
    private val toolSet: ChangelogToolSet
) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val systemPrompt = """
Ты — ассистент для генерации changelog'ов. Проанализируй изменения в Pull Request и создай структурированный changelog.

Используй доступные инструменты для сбора информации:
1. Вызови listChangedFiles() чтобы увидеть список измененных файлов
2. Вызови getCommitMessages() чтобы увидеть коммиты
3. Вызови getFileDiff(path) для ключевых файлов чтобы понять суть изменений

Классификация:
- **Продакшн**: изменения, которые увидят конечные пользователи (новые фичи, баг-фиксы пользовательских сценариев, изменения в поведении бота/приложения)
- **Внутренние**: инфраструктура, CI/CD, рефакторинг, тесты, админка, документация, зависимости

После анализа верни ТОЛЬКО валидный JSON объект, без markdown, без комментариев, без текста до или после:
{"production": [{"title": "...", "details": "..."}], "internal": [{"title": "...", "details": "..."}], "summary": "..."}

ВАЖНО: Весь changelog должен быть строго на русском языке. Все описания, заголовки и summary — только на русском.
Пиши понятным языком для разработчиков. Группируй связанные изменения.
    """.trimIndent()

    suspend fun generateChangelog(): ChangelogResponse {
        val client = OpenRouterLLMClient(
            apiKey = config.openRouterApiKey
        )
        val executor = SingleLLMPromptExecutor(client)

        val toolRegistry = ToolRegistry {
            tools(toolSet)
        }

        val agent = AIAgent(
            promptExecutor = executor,
            llmModel = OpenRouterModels.DeepSeekV30324,
            strategy = chatAgentStrategy(),
            toolRegistry = toolRegistry,
            systemPrompt = systemPrompt,
            temperature = 0.7,
            maxIterations = 15
        )

        val userMessage = "Проанализируй изменения в Pull Request и создай changelog. Верни ТОЛЬКО JSON."
        val result = agent.run(userMessage)

        return parseChangelogResponse(result)
    }

    private fun parseChangelogResponse(llmResponse: String): ChangelogResponse {
        // Try parsing the whole response as JSON first
        try {
            return json.decodeFromString<ChangelogResponse>(llmResponse.trim())
        } catch (_: Exception) { /* continue */ }

        // Strip markdown code block if present (```json ... ``` or ``` ... ```)
        val stripped = llmResponse
            .replace(Regex("```json\\s*"), "")
            .replace(Regex("```\\s*"), "")
            .trim()

        try {
            return json.decodeFromString<ChangelogResponse>(stripped)
        } catch (_: Exception) { /* continue */ }

        // Extract JSON object from surrounding text
        val jsonStart = stripped.indexOfFirst { it == '{' }
        val jsonEnd = stripped.indexOfLast { it == '}' }

        if (jsonStart == -1 || jsonEnd == -1 || jsonEnd <= jsonStart) {
            throw RuntimeException("No JSON found in LLM response: $llmResponse")
        }

        val jsonString = stripped.substring(jsonStart, jsonEnd + 1)

        return try {
            json.decodeFromString<ChangelogResponse>(jsonString)
        } catch (e: Exception) {
            throw RuntimeException("Failed to parse changelog JSON: ${e.message}\nJSON input: ${jsonString.take(200)}", e)
        }
    }
}
