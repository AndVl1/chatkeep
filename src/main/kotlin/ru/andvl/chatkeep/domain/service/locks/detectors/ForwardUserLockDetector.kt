package ru.andvl.chatkeep.domain.service.locks.detectors

import dev.inmo.tgbotapi.types.message.ForwardInfo
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.abstracts.PossiblyForwardedMessage
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.domain.model.locks.LockType
import ru.andvl.chatkeep.domain.service.locks.AbstractLockDetector
import ru.andvl.chatkeep.domain.service.locks.DetectionContext

/**
 * Detects messages forwarded FROM users (not anonymous, not channels/groups).
 */
@Component
class ForwardUserLockDetector : AbstractLockDetector() {
    override val lockType = LockType.FORWARDUSER

    override suspend fun detect(
        message: ContentMessage<*>,
        context: DetectionContext
    ): Boolean {
        val forwardInfo = (message as? PossiblyForwardedMessage)?.forwardInfo
        return forwardInfo is ForwardInfo.ByUser
    }
}
