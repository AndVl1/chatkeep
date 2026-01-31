package ru.andvl.chatkeep.changelog.config

data class Config(
    val openRouterApiKey: String,
    val changelogModel: String,
    val githubToken: String,
    val prNumber: Int,
    val githubRepository: String,
    val baseBranch: String,
    val headBranch: String,
    val mode: Mode,
    val tokenLimitWarning: Int
) {
    enum class Mode {
        GENERATE, CHECK_UPDATE
    }

    companion object {
        fun fromEnvAndArgs(args: Array<String>): Config {
            val argsMap = parseArgs(args)

            val openRouterApiKey = System.getenv("OPENROUTER_API_KEY")
                ?: throw IllegalArgumentException("OPENROUTER_API_KEY environment variable is required")

            val githubToken = System.getenv("GITHUB_TOKEN")
                ?: throw IllegalArgumentException("GITHUB_TOKEN environment variable is required")

            val prNumber = argsMap["pr-number"]?.toIntOrNull()
                ?: System.getenv("PR_NUMBER")?.toIntOrNull()
                ?: throw IllegalArgumentException("PR_NUMBER is required (via env or --pr-number)")

            val githubRepository = argsMap["repo"]
                ?: System.getenv("GITHUB_REPOSITORY")
                ?: throw IllegalArgumentException("GITHUB_REPOSITORY is required (via env or --repo)")

            val headBranch = argsMap["head-branch"]
                ?: System.getenv("HEAD_BRANCH")
                ?: throw IllegalArgumentException("HEAD_BRANCH is required (via env or --head-branch)")

            return Config(
                openRouterApiKey = openRouterApiKey,
                changelogModel = System.getenv("CHANGELOG_MODEL") ?: "deepseek/deepseek-chat-v3-0324",
                githubToken = githubToken,
                prNumber = prNumber,
                githubRepository = githubRepository,
                baseBranch = argsMap["base-branch"] ?: System.getenv("BASE_BRANCH") ?: "main",
                headBranch = headBranch,
                mode = when (argsMap["mode"] ?: System.getenv("MODE") ?: "generate") {
                    "check-update" -> Mode.CHECK_UPDATE
                    else -> Mode.GENERATE
                },
                tokenLimitWarning = System.getenv("TOKEN_LIMIT_WARNING")?.toIntOrNull() ?: 50000
            )
        }

        private fun parseArgs(args: Array<String>): Map<String, String> {
            val result = mutableMapOf<String, String>()
            var i = 0
            while (i < args.size) {
                val arg = args[i]
                if (arg.startsWith("--")) {
                    val key = arg.removePrefix("--")
                    if (i + 1 < args.size && !args[i + 1].startsWith("--")) {
                        result[key] = args[i + 1]
                        i += 2
                    } else {
                        i++
                    }
                } else {
                    i++
                }
            }
            return result
        }
    }
}
