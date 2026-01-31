package ru.andvl.chatkeep.changelog.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.tools
import ai.koog.agents.ext.agent.chatAgentStrategy
import ai.koog.prompt.executor.clients.openrouter.OpenRouterLLMClient
import ai.koog.prompt.executor.clients.openrouter.OpenRouterModels
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.structure.StructureFixingParser
import ai.koog.prompt.structure.json.JsonStructuredData
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

    private val glm45Air = LLModel(
        provider = LLMProvider.OpenRouter,
        id = "z-ai/glm-4.5-air",
        capabilities = listOf(
            LLMCapability.Completion,
            LLMCapability.Temperature,
            LLMCapability.Schema.JSON.Basic
        ),
        contextLength = 128_000L,
        maxOutputTokens = 8_000L
    )

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

        return parseWithFixingParser(executor, result)
    }

    private suspend fun parseWithFixingParser(
        executor: SingleLLMPromptExecutor,
        llmResponse: String
    ): ChangelogResponse {
        // Pre-clean the response: strip markdown code blocks and extract JSON
        val cleaned = cleanLlmResponse(llmResponse)

        // Create structured data definition for ChangelogResponse
        val structure = JsonStructuredData.createJsonStructure(
            id = "ChangelogResponse",
            serializer = ChangelogResponse.serializer(),
            json = json
        )

        // Create fixing parser with GLM 4.5 Air model (retries up to 3 times)
        val fixingParser = StructureFixingParser(
            fixingModel = glm45Air,
            retries = 3
        )

        // parse() first tries direct deserialization, then uses GLM to fix if needed
        return fixingParser.parse(executor, structure, cleaned)
    }

    private fun cleanLlmResponse(llmResponse: String): String {
        // Try as-is first
        val trimmed = llmResponse.trim()
        try {
            json.decodeFromString<ChangelogResponse>(trimmed)
            return trimmed
        } catch (_: Exception) { /* continue */ }

        // Strip markdown code blocks
        val stripped = trimmed
            .replace(Regex("```json\\s*"), "")
            .replace(Regex("```\\s*"), "")
            .trim()

        try {
            json.decodeFromString<ChangelogResponse>(stripped)
            return stripped
        } catch (_: Exception) { /* continue */ }

        // Extract JSON object from surrounding text
        val jsonStart = stripped.indexOfFirst { it == '{' }
        val jsonEnd = stripped.indexOfLast { it == '}' }

        if (jsonStart != -1 && jsonEnd > jsonStart) {
            return stripped.substring(jsonStart, jsonEnd + 1)
        }

        // Return as-is, let the fixing parser handle it
        return stripped
    }
}
