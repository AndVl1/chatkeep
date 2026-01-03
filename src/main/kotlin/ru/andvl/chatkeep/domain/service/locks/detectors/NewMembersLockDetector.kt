package ru.andvl.chatkeep.domain.service.locks.detectors

import dev.inmo.tgbotapi.types.message.ChatEvents.NewChatMembers
import dev.inmo.tgbotapi.types.message.abstracts.ChatEventMessage
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.domain.model.locks.LockType
import ru.andvl.chatkeep.domain.service.locks.AbstractLockDetector
import ru.andvl.chatkeep.domain.service.locks.DetectionContext

/**
 * Detects "new members joined" service messages.
 */
@Component
class NewMembersLockDetector : AbstractLockDetector() {
    override val lockType = LockType.NEWMEMBERS

    override suspend fun detect(message: ContentMessage<*>, context: DetectionContext): Boolean {
        if (message !is ChatEventMessage<*>) return false
        return message.chatEvent is NewChatMembers
    }
}
