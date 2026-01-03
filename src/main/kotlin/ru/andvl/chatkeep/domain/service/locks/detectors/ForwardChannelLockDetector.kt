package ru.andvl.chatkeep.domain.service.locks.detectors

import dev.inmo.tgbotapi.types.message.ForwardInfo
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.abstracts.PossiblyForwardedMessage
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.domain.model.locks.LockType
import ru.andvl.chatkeep.domain.service.locks.AbstractLockDetector
import ru.andvl.chatkeep.domain.service.locks.DetectionContext

/**
 * Detects messages forwarded FROM channels.
 * Includes both FromChannel and SentByChannel types.
 */
@Component
class ForwardChannelLockDetector : AbstractLockDetector() {
    override val lockType = LockType.FORWARDCHANNEL

    override suspend fun detect(
        message: ContentMessage<*>,
        context: DetectionContext
    ): Boolean {
        val forwardInfo = (message as? PossiblyForwardedMessage)?.forwardInfo
        return when (forwardInfo) {
            is ForwardInfo.PublicChat.FromChannel -> true
            is ForwardInfo.PublicChat.SentByChannel -> true
            else -> false
        }
    }
}
