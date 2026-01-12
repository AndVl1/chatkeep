package ru.andvl.chatkeep.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "admin")
data class AdminProperties(
    val userIds: List<Long> = emptyList()
)
