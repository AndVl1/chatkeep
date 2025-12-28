package ru.andvl.chatkeep.bot.handlers

import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext

interface Handler {
    suspend fun BehaviourContext.register()
}
