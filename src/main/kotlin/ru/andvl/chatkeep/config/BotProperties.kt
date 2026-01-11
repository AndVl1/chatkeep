package ru.andvl.chatkeep.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "telegram.bot")
data class BotProperties(
    val token: String,
    val username: String
)

@ConfigurationProperties(prefix = "telegram.adminbot")
data class AdminBotProperties(
    val username: String = "",  // Admin bot username for Login Widget on admin subdomain
    val token: String = ""      // Admin bot token for Login Widget hash validation
)
