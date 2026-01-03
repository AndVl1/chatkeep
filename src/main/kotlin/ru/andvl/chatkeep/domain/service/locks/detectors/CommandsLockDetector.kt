package ru.andvl.chatkeep.domain.service.locks.detectors

import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.textsources.BotCommandTextSource
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.domain.model.locks.LockType
import ru.andvl.chatkeep.domain.service.locks.AbstractLockDetector
import ru.andvl.chatkeep.domain.service.locks.DetectionContext

@Component
class CommandsLockDetector : AbstractLockDetector() {
    override val lockType = LockType.COMMANDS

    override suspend fun detect(message: ContentMessage<*>, context: DetectionContext): Boolean {
        val textSources = message.getTextSources()

        val commands = textSources.filterIsInstance<BotCommandTextSource>()
        if (commands.isEmpty()) return false

        // If no allowlist configured, all commands are violations
        if (context.allowlistedCommands.isEmpty()) {
            return true
        }

        // Normalize allowlist to lowercase for case-insensitive comparison
        val allowlistedCommandsLowercase = context.allowlistedCommands.map { it.lowercase() }.toSet()

        // Check if any command is not allowlisted (case-insensitive)
        return commands.any { commandSource ->
            val command = commandSource.command.trim().removePrefix("/").lowercase()
            !allowlistedCommandsLowercase.contains(command)
        }
    }
}
