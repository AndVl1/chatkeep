package ru.andvl.chatkeep.changelog.agent

import kotlinx.serialization.Serializable

@Serializable
data class ChangelogResponse(
    val production: List<ChangelogEntry>,
    val internal: List<ChangelogEntry>,
    val summary: String
)

@Serializable
data class ChangelogEntry(
    val title: String,
    val details: String? = null
)
