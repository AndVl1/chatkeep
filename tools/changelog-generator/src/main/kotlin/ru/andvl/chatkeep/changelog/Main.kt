package ru.andvl.chatkeep.changelog

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.andvl.chatkeep.changelog.agent.ChangelogAgent
import ru.andvl.chatkeep.changelog.agent.ChangelogResponse
import ru.andvl.chatkeep.changelog.agent.ChangelogTools
import ru.andvl.chatkeep.changelog.config.Config
import ru.andvl.chatkeep.changelog.formatter.MarkdownFormatter
import ru.andvl.chatkeep.changelog.git.GitOperations
import ru.andvl.chatkeep.changelog.github.GitHubClient

fun main(args: Array<String>) {
    try {
        val config = Config.fromEnvAndArgs(args)

        println("Changelog Generator")
        println("===================")
        println("Repository: ${config.githubRepository}")
        println("PR: #${config.prNumber}")
        println("Base: ${config.baseBranch}, Head: ${config.headBranch}")
        println("Mode: ${config.mode}")
        println()

        when (config.mode) {
            Config.Mode.GENERATE -> generateChangelog(config)
            Config.Mode.CHECK_UPDATE -> checkAndUpdate(config)
        }
    } catch (e: Exception) {
        System.err.println("Error: ${e.message}")
        e.printStackTrace()
        kotlin.system.exitProcess(1)
    }
}

private fun generateChangelog(config: Config) {
    val gitOps = GitOperations()
    val tools = ChangelogTools(gitOps, config.baseBranch, config.headBranch)

    // Try to generate with LLM, with retry
    val changelog = try {
        println("Generating changelog with LLM...")
        generateWithRetry(config, tools, maxAttempts = 2)
    } catch (e: Exception) {
        System.err.println("LLM generation failed after retries: ${e.message}")
        System.err.println("Falling back to commit list...")

        // Fallback to commit list
        val commits = gitOps.getCommitMessages(config.baseBranch, config.headBranch)
        val fallbackChangelog = MarkdownFormatter.formatFallback(commits)

        val githubClient = GitHubClient(config.githubToken)
        githubClient.updatePRBody(config.githubRepository, config.prNumber, fallbackChangelog)

        println("Updated PR with fallback changelog (commit list)")
        return
    }

    // Format for PR
    val prChangelog = MarkdownFormatter.formatForPR(changelog)

    // Update PR
    val githubClient = GitHubClient(config.githubToken)
    githubClient.updatePRBody(config.githubRepository, config.prNumber, prChangelog)

    println("Changelog generated and PR updated")
    println()

    // Output production changes for Telegram step
    val telegramOutput = MarkdownFormatter.formatForTelegram(
        changelog,
        config.prNumber,
        config.headBranch
    )

    println("Telegram notification:")
    println(telegramOutput)
    println()

    // Output as JSON for CI to use
    val json = Json { prettyPrint = true }
    val jsonOutput = json.encodeToString(changelog)

    println("JSON output:")
    println(jsonOutput)
}

private fun generateWithRetry(
    config: Config,
    tools: ChangelogTools,
    maxAttempts: Int
): ChangelogResponse {
    var lastException: Exception? = null

    repeat(maxAttempts) { attempt ->
        try {
            println("Attempt ${attempt + 1}/$maxAttempts...")

            val agent = ChangelogAgent(config, tools)
            return agent.generateChangelog()
        } catch (e: Exception) {
            lastException = e
            System.err.println("Attempt ${attempt + 1} failed: ${e.message}")

            if (attempt < maxAttempts - 1) {
                println("Retrying...")
                Thread.sleep(2000L * (attempt + 1))
            }
        }
    }

    throw lastException ?: RuntimeException("All attempts failed")
}

private fun checkAndUpdate(config: Config) {
    println("Check-update mode: re-running generation...")
    generateChangelog(config)
}
