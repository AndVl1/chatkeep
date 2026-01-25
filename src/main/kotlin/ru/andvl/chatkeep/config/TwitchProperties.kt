package ru.andvl.chatkeep.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "twitch")
data class TwitchProperties(
    val clientId: String,
    val clientSecret: String,
    val webhookUrl: String,
    val webhookSecret: String
)
