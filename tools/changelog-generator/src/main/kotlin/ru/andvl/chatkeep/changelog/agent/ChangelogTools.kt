package ru.andvl.chatkeep.changelog.agent

import kotlinx.serialization.Serializable
import ru.andvl.chatkeep.changelog.git.GitOperations

/**
 * Tools that the LLM agent can call to gather information about PR changes.
 */
class ChangelogTools(
    private val gitOps: GitOperations,
    private val baseBranch: String,
    private val headBranch: String
) {

    @Serializable
    data class FileListResult(
        val files: List<FileInfo>
    )

    @Serializable
    data class FileInfo(
        val path: String,
        val additions: Int,
        val deletions: Int
    )

    fun listChangedFiles(): FileListResult {
        val files = gitOps.listChangedFiles(baseBranch, headBranch)
        return FileListResult(
            files = files.map { FileInfo(it.path, it.additions, it.deletions) }
        )
    }

    fun getFileDiff(path: String): String {
        return gitOps.getFileDiff(baseBranch, headBranch, path)
    }

    fun getCommitMessages(): List<String> {
        return gitOps.getCommitMessages(baseBranch, headBranch)
    }

    fun getFileContent(path: String): String {
        return gitOps.getFileContent(path)
    }
}
