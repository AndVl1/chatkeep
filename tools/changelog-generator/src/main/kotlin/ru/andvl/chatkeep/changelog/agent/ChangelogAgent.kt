package ru.andvl.chatkeep.changelog.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeExecuteTool
import ai.koog.agents.core.dsl.extension.nodeLLMRequest
import ai.koog.agents.core.dsl.extension.nodeLLMRequestStructured
import ai.koog.agents.core.dsl.extension.nodeLLMSendToolResult
import ai.koog.agents.core.dsl.extension.onAssistantMessage
import ai.koog.agents.core.dsl.extension.onToolCall
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.tools
import ai.koog.prompt.executor.clients.openrouter.OpenRouterLLMClient
import ai.koog.prompt.executor.clients.openrouter.OpenRouterModels
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.structure.StructureFixingParser
import ai.koog.prompt.structure.StructuredResponse
import ru.andvl.chatkeep.changelog.config.Config

class ChangelogAgent(
    private val config: Config,
    private val toolSet: ChangelogToolSet
) {

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

    private val fixingParser = StructureFixingParser(
        fixingModel = glm45Air,
        retries = 3
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

ВАЖНО: Весь changelog должен быть строго на русском языке. Все описания, заголовки и summary — только на русском.
Пиши понятным языком для разработчиков. Группируй связанные изменения.
    """.trimIndent()

    private fun changelogStrategy() = strategy<String, ChangelogResponse>("changelog") {
        val nodeLLM by nodeLLMRequest()
        val nodeExecTool by nodeExecuteTool()
        val nodeSendResult by nodeLLMSendToolResult()
        val nodeStructured by nodeLLMRequestStructured<ChangelogResponse>(
            fixingParser = fixingParser
        )

        // Tool calling loop
        edge(nodeStart forwardTo nodeLLM)
        edge(nodeLLM forwardTo nodeExecTool onToolCall { true })
        edge(nodeExecTool forwardTo nodeSendResult)
        edge(nodeSendResult forwardTo nodeExecTool onToolCall { true })

        // When LLM stops calling tools → get structured response
        edge(nodeLLM forwardTo nodeStructured onAssistantMessage { true })
        edge(nodeSendResult forwardTo nodeStructured onAssistantMessage { true })

        // Structured output → finish
        edge(nodeStructured forwardTo nodeFinish onCondition { it.isSuccess }
            transformed { it.getOrThrow().structure })
    }

    suspend fun generateChangelog(): ChangelogResponse {
        val client = OpenRouterLLMClient(apiKey = config.openRouterApiKey)
        val executor = SingleLLMPromptExecutor(client)

        val toolRegistry = ToolRegistry {
            tools(toolSet)
        }

        val agent = AIAgent(
            promptExecutor = executor,
            llmModel = OpenRouterModels.DeepSeekV30324,
            strategy = changelogStrategy(),
            toolRegistry = toolRegistry,
            systemPrompt = systemPrompt,
            temperature = 0.7,
            maxIterations = 15
        )

        return agent.run("Проанализируй изменения в Pull Request и создай changelog.")
    }
}
