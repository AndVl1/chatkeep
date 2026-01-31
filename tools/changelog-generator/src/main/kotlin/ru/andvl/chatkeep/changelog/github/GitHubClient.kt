package ru.andvl.chatkeep.changelog.github

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

@Serializable
data class PullRequest(
    val number: Int,
    val body: String?
)

@Serializable
data class UpdatePRRequest(
    val body: String
)

class GitHubClient(private val token: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun updatePRBody(repo: String, prNumber: Int, changelog: String) {
        // Get current PR body
        val currentPR = getPullRequest(repo, prNumber)
        val currentBody = currentPR.body ?: ""

        // Update or append changelog
        val updatedBody = updateChangelogSection(currentBody, changelog)

        // Update PR
        patchPullRequest(repo, prNumber, updatedBody)

        println("✓ Updated PR #$prNumber with changelog")
    }

    private fun getPullRequest(repo: String, prNumber: Int): PullRequest {
        val request = Request.Builder()
            .url("https://api.github.com/repos/$repo/pulls/$prNumber")
            .header("Authorization", "token $token")
            .header("Accept", "application/vnd.github.v3+json")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw RuntimeException("Failed to get PR: ${response.code} ${response.message}")
            }

            val body = response.body?.string()
                ?: throw RuntimeException("Empty response body")

            return json.decodeFromString<PullRequest>(body)
        }
    }

    private fun patchPullRequest(repo: String, prNumber: Int, body: String) {
        val requestBody = json.encodeToString(
            UpdatePRRequest.serializer(),
            UpdatePRRequest(body)
        )

        val request = Request.Builder()
            .url("https://api.github.com/repos/$repo/pulls/$prNumber")
            .header("Authorization", "token $token")
            .header("Accept", "application/vnd.github.v3+json")
            .header("Content-Type", "application/json")
            .patch(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw RuntimeException("Failed to update PR: ${response.code} ${response.message}")
            }
        }
    }

    private fun updateChangelogSection(currentBody: String, changelog: String): String {
        val startMarker = "<!-- CHANGELOG_START -->"
        val endMarker = "<!-- CHANGELOG_END -->"

        val startIndex = currentBody.indexOf(startMarker)
        val endIndex = currentBody.indexOf(endMarker)

        return if (startIndex != -1 && endIndex != -1) {
            // Replace existing changelog
            currentBody.substring(0, startIndex) +
                    changelog +
                    currentBody.substring(endIndex + endMarker.length)
        } else {
            // Append changelog
            if (currentBody.isBlank()) {
                changelog
            } else {
                "$currentBody\n\n$changelog"
            }
        }
    }
}
