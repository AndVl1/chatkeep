package ru.andvl.chatkeep.api.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.domains")
data class AppDomainsConfig(
    val main: String = "localhost",
    val api: String = "localhost:8080",
    val miniapp: String = "localhost",
    val admin: String = "localhost"
)
