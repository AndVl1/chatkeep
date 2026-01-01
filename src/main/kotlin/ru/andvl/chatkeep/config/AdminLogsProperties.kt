package ru.andvl.chatkeep.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "admin-logs")
data class AdminLogsProperties(
    /**
     * Path to directory where admin log JSON files will be stored.
     */
    val path: String = "/tmp/chatkeep/admin-logs",

    /**
     * Maximum number of log entries to export at once.
     * Prevents resource exhaustion on large chats.
     */
    val maxExportLimit: Int = 10000,

    /**
     * Whether to include message text in exported logs.
     * Set to false for privacy compliance (GDPR).
     */
    val includeMessageText: Boolean = false,

    /**
     * Maximum length of message text to include in exports.
     * Only applies if includeMessageText is true.
     */
    val maxMessageTextLength: Int = 100
)
