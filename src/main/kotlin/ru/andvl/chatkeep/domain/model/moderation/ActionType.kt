package ru.andvl.chatkeep.domain.model.moderation

enum class ActionType {
    NOTHING,
    WARN,
    UNWARN,
    MUTE,
    UNMUTE,
    BAN,
    UNBAN,
    KICK
}
