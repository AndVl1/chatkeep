package ru.andvl.chatkeep.changelog.agent

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import ru.andvl.chatkeep.changelog.config.Config
import java.util.concurrent.TimeUnit

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.7,
    val max_tokens: Int? = null
)

@Serializable
data class ChatResponse(
    val choices: List<Choice>
) {
    @Serializable
    data class Choice(
        val message: ChatMessage
    )
}

class ChangelogAgent(
    private val config: Config,
    private val tools: ChangelogTools
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val systemPrompt = """
Ты — ассистент для генерации changelog'ов. Проанализируй изменения в Pull Request и создай структурированный changelog.

Классификация:
- **Продакшн**: изменения, которые увидят конечные пользователи (новые фичи, баг-фиксы пользовательских сценариев, изменения в поведении бота/приложения)
- **Внутренние**: инфраструктура, CI/CD, рефакторинг, тесты, админка, документация, зависимости

Формат ответа — JSON:
{
  "production": [{"title": "Краткое описание", "details": "Подробности (опционально)"}],
  "internal": [{"title": "Краткое описание", "details": "Подробности (опционально)"}],
  "summary": "Одно предложение — общая суть PR"
}

Пиши понятным языком для разработчиков. Группируй связанные изменения.
    """.trimIndent()

    suspend fun generateChangelog(): ChangelogResponse {
        // Gather information
        val changedFiles = tools.listChangedFiles()
        val commits = tools.getCommitMessages()

        // Get diffs for key files (limit to avoid token overflow)
        val keyFiles = selectKeyFiles(changedFiles.files)
        val diffs = keyFiles.take(10).map { file ->
            val diff = tools.getFileDiff(file.path)
            "File: ${file.path} (+${file.additions}/-${file.deletions})\n$diff"
        }

        // Build context
        val context = buildString {
            appendLine("=== Коммиты ===")
            commits.forEach { appendLine(it) }
            appendLine()
            appendLine("=== Измененные файлы ===")
            changedFiles.files.forEach {
                appendLine("${it.path} (+${it.additions}/-${it.deletions})")
            }
            appendLine()
            if (diffs.isNotEmpty()) {
                appendLine("=== Ключевые изменения ===")
                diffs.forEach { appendLine(it); appendLine() }
            }
        }

        // Call LLM
        val userMessage = """
Проанализируй следующие изменения в Pull Request и создай changelog в формате JSON.

$context

Верни ТОЛЬКО JSON, без дополнительного текста.
        """.trimIndent()

        val response = callOpenRouter(userMessage)

        // Parse JSON from response
        return parseChangelogResponse(response)
    }

    private fun selectKeyFiles(files: List<ChangelogTools.FileInfo>): List<ChangelogTools.FileInfo> {
        // Prioritize source files, exclude common generated/config files
        return files
            .filter { file ->
                val path = file.path.lowercase()
                // Include source files
                (path.endsWith(".kt") ||
                 path.endsWith(".java") ||
                 path.endsWith(".ts") ||
                 path.endsWith(".tsx") ||
                 path.endsWith(".sql") ||
                 path.contains("docker") ||
                 path.contains(".yml") ||
                 path.contains(".yaml")) &&
                // Exclude generated/lock files
                !path.contains("build/") &&
                !path.contains("node_modules/") &&
                !path.contains(".lock") &&
                !path.contains("package-lock")
            }
            .sortedByDescending { it.additions + it.deletions }
    }

    private fun callOpenRouter(userMessage: String): String {
        val chatRequest = ChatRequest(
            model = config.changelogModel,
            messages = listOf(
                ChatMessage(role = "system", content = systemPrompt),
                ChatMessage(role = "user", content = userMessage)
            ),
            temperature = 0.7,
            max_tokens = 4000
        )

        val requestBody = json.encodeToString(ChatRequest.serializer(), chatRequest)

        val request = Request.Builder()
            .url("https://openrouter.ai/api/v1/chat/completions")
            .header("Authorization", "Bearer ${config.openRouterApiKey}")
            .header("Content-Type", "application/json")
            .header("HTTP-Referer", "https://github.com/${config.githubRepository}")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val body = response.body?.string() ?: "No error details"
                throw RuntimeException("OpenRouter API call failed: ${response.code} ${response.message}\n$body")
            }

            val responseBody = response.body?.string()
                ?: throw RuntimeException("Empty response from OpenRouter")

            val chatResponse = json.decodeFromString<ChatResponse>(responseBody)
            return chatResponse.choices.firstOrNull()?.message?.content
                ?: throw RuntimeException("No response content from LLM")
        }
    }

    private fun parseChangelogResponse(llmResponse: String): ChangelogResponse {
        // Extract JSON from response (in case LLM adds extra text)
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
