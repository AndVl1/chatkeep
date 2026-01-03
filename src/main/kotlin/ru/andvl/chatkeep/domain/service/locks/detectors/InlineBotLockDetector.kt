package ru.andvl.chatkeep.domain.service.locks.detectors

import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.abstracts.PossiblySentViaBotCommonMessage
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.domain.model.locks.LockType
import ru.andvl.chatkeep.domain.service.locks.AbstractLockDetector
import ru.andvl.chatkeep.domain.service.locks.DetectionContext

@Component
class InlineBotLockDetector : AbstractLockDetector() {
    override val lockType = LockType.INLINE

    override suspend fun detect(message: ContentMessage<*>, context: DetectionContext): Boolean {
        return (message as? PossiblySentViaBotCommonMessage)?.senderBot != null
    }
}
