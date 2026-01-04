package ru.andvl.chatkeep.domain.model.locks

data class LockConfig(
    val locked: Boolean = false,
    val reason: String? = null
)
