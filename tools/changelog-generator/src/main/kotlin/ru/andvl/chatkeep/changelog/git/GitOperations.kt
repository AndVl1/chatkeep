package ru.andvl.chatkeep.changelog.git

import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class ChangedFile(
    val path: String,
    val additions: Int,
    val deletions: Int
)

class GitOperations {

    fun listChangedFiles(baseBranch: String, headBranch: String): List<ChangedFile> {
        val output = executeGitCommand("git", "diff", "--numstat", "$baseBranch...$headBranch")

        return output.lines()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.split(Regex("\\s+"), limit = 3)
                if (parts.size == 3) {
                    val additions = parts[0].toIntOrNull() ?: 0
                    val deletions = parts[1].toIntOrNull() ?: 0
                    val path = parts[2]
                    ChangedFile(path, additions, deletions)
                } else {
                    null
                }
            }
    }

    fun getFileDiff(baseBranch: String, headBranch: String, path: String): String {
        return try {
            executeGitCommand("git", "diff", "$baseBranch...$headBranch", "--", path)
        } catch (e: Exception) {
            "Error getting diff for $path: ${e.message}"
        }
    }

    fun getCommitMessages(baseBranch: String, headBranch: String): List<String> {
        val output = executeGitCommand("git", "log", "--oneline", "$baseBranch..$headBranch")

        return output.lines()
            .filter { it.isNotBlank() }
    }

    fun getFileContent(path: String): String {
        return try {
            // Prevent path traversal — only allow relative paths within repo
            val normalized = File(path).normalize().path
            if (normalized.startsWith("..") || File(normalized).isAbsolute) {
                "Error: path traversal not allowed: $path"
            } else {
                File(normalized).readText()
            }
        } catch (e: Exception) {
            "Error reading file $path: ${e.message}"
        }
    }

    private fun executeGitCommand(vararg command: String): String {
        val process = ProcessBuilder(*command)
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()

        if (exitCode != 0) {
            throw RuntimeException("Git command failed with exit code $exitCode: ${command.joinToString(" ")}\n$output")
        }

        return output
    }
}
