package ru.andvl.chatkeep.domain.service.locks.detectors

import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.abstracts.FromChannelGroupContentMessage
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.domain.model.locks.LockType
import ru.andvl.chatkeep.domain.service.locks.AbstractLockDetector
import ru.andvl.chatkeep.domain.service.locks.DetectionContext

@Component
class AnonChannelLockDetector : AbstractLockDetector() {
    override val lockType = LockType.ANONCHANNEL

    override suspend fun detect(message: ContentMessage<*>, context: DetectionContext): Boolean {
        return message is FromChannelGroupContentMessage<*>
    }
}
