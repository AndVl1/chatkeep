package ru.andvl.chatkeep.domain.service.locks.detectors

import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.abstracts.PossiblyForwardedMessage
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.domain.model.locks.LockType
import ru.andvl.chatkeep.domain.service.locks.AbstractLockDetector
import ru.andvl.chatkeep.domain.service.locks.DetectionContext

/**
 * Detects ANY forwarded message regardless of source type.
 */
@Component
class ForwardLockDetector : AbstractLockDetector() {
    override val lockType = LockType.FORWARD

    override suspend fun detect(
        message: ContentMessage<*>,
        context: DetectionContext
    ): Boolean {
        return (message as? PossiblyForwardedMessage)?.forwardInfo != null
    }
}
