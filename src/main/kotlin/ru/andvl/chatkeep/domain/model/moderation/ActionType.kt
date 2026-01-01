package ru.andvl.chatkeep.domain.model.moderation

enum class ActionType {
    NOTHING,
    WARN,
    UNWARN,
    MUTE,
    UNMUTE,
    BAN,
    UNBAN,
    KICK,
    CLEAN_SERVICE_ON,
    CLEAN_SERVICE_OFF
}
