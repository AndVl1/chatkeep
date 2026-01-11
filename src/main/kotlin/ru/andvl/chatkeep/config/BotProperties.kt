package ru.andvl.chatkeep.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "telegram.bot")
data class BotProperties(
    val token: String,
    val username: String
)
