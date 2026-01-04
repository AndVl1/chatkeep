package ru.andvl.chatkeep.domain.service.locks.detectors

import dev.inmo.tgbotapi.types.message.ChatEvents.LeftChatMemberEvent
import dev.inmo.tgbotapi.types.message.abstracts.ChatEventMessage
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.domain.model.locks.LockType
import ru.andvl.chatkeep.domain.service.locks.AbstractLockDetector
import ru.andvl.chatkeep.domain.service.locks.DetectionContext

/**
 * Detects "member left" service messages.
 */
@Component
class LeftMemberLockDetector : AbstractLockDetector() {
    override val lockType = LockType.LEFTMEMBER

    override suspend fun detect(message: ContentMessage<*>, context: DetectionContext): Boolean {
        if (message !is ChatEventMessage<*>) return false
        return message.chatEvent is LeftChatMemberEvent
    }
}
