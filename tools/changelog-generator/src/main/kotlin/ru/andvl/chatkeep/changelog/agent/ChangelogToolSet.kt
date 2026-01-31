package ru.andvl.chatkeep.changelog.agent

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import ru.andvl.chatkeep.changelog.git.GitOperations

@LLMDescription("Tools for analyzing Pull Request changes in a git repository")
class ChangelogToolSet(
    private val gitOps: GitOperations,
    private val baseBranch: String,
    private val headBranch: String
) : ToolSet {

    @Tool
    @LLMDescription("List all files changed between base and head branches with addition/deletion counts")
    fun listChangedFiles(): String {
        val files = gitOps.listChangedFiles(baseBranch, headBranch)
        return files.joinToString("\n") { "${it.path} (+${it.additions}/-${it.deletions})" }
    }

    @Tool
    @LLMDescription("Get the diff for a specific file between base and head branches")
    fun getFileDiff(
        @LLMDescription("Path to the file to get diff for")
        path: String
    ): String {
        return gitOps.getFileDiff(baseBranch, headBranch, path)
    }

    @Tool
    @LLMDescription("Get all commit messages between base and head branches")
    fun getCommitMessages(): String {
        return gitOps.getCommitMessages(baseBranch, headBranch).joinToString("\n")
    }

    @Tool
    @LLMDescription("Read the content of a file in the repository")
    fun getFileContent(
        @LLMDescription("Path to the file to read")
        path: String
    ): String {
        return gitOps.getFileContent(path)
    }
}
