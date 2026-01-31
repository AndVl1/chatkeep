package ru.andvl.chatkeep.changelog.agent

import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("ChangelogResponse")
@LLMDescription("Структурированный changelog Pull Request на русском языке")
data class ChangelogResponse(
    @property:LLMDescription("Продакшн-изменения: фичи и баг-фиксы, которые увидят конечные пользователи")
    val production: List<ChangelogEntry>,
    @property:LLMDescription("Внутренние изменения: инфраструктура, CI/CD, рефакторинг, тесты, документация")
    val internal: List<ChangelogEntry>,
    @property:LLMDescription("Краткая суть PR в одном предложении на русском языке")
    val summary: String
)

@Serializable
@SerialName("ChangelogEntry")
@LLMDescription("Одна запись changelog на русском языке")
data class ChangelogEntry(
    @property:LLMDescription("Краткое описание изменения на русском языке")
    val title: String,
    @property:LLMDescription("Подробности изменения на русском (опционально)")
    val details: String? = null
)
