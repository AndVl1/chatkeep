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
    CLEAN_SERVICE_OFF,
    LOCK_WARNS_ON,
    LOCK_WARNS_OFF,
    LOCK_ENABLED,
    LOCK_DISABLED,
    CONFIG_CHANGED,
    BLOCKLIST_REMOVED
}
