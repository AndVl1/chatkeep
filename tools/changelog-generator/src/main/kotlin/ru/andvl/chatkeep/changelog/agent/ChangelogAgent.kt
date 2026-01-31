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

После анализа верни ТОЛЬКО JSON без дополнительного текста:
{
  "production": [{"title": "Краткое описание", "details": "Подробности (опционально)"}],
  "internal": [{"title": "Краткое описание", "details": "Подробности (опционально)"}],
  "summary": "Одно предложение — общая суть PR"
}

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

        val userMessage = "Проанализируй изменения в Pull Request и создай changelog в формате JSON."
        val result = agent.run(userMessage)

        return parseChangelogResponse(result)
    }

    private fun parseChangelogResponse(llmResponse: String): ChangelogResponse {
        val jsonStart = llmResponse.indexOfFirst { it == '{' }
        val jsonEnd = llmResponse.indexOfLast { it == '}' }

        if (jsonStart == -1 || jsonEnd == -1) {
            throw RuntimeException("No JSON found in LLM response: $llmResponse")
        }

        val jsonString = llmResponse.substring(jsonStart, jsonEnd + 1)

        return try {
            json.decodeFromString<ChangelogResponse>(jsonString)
        } catch (e: Exception) {
            throw RuntimeException("Failed to parse changelog JSON: ${e.message}\nResponse: $jsonString", e)
        }
    }
}
