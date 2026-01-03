package ru.andvl.chatkeep.domain.service.locks.detectors

import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.abstracts.SignedMessage
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.domain.model.locks.LockType
import ru.andvl.chatkeep.domain.service.locks.AbstractLockDetector
import ru.andvl.chatkeep.domain.service.locks.DetectionContext

/**
 * Detects channel posts with author signature.
 */
@Component
class SignatureLockDetector : AbstractLockDetector() {
    override val lockType = LockType.SIGNATURE

    override suspend fun detect(message: ContentMessage<*>, context: DetectionContext): Boolean {
        // SignedMessage interface indicates message has author signature
        if (message !is SignedMessage) return false
        return message.authorSignature != null
    }
}
