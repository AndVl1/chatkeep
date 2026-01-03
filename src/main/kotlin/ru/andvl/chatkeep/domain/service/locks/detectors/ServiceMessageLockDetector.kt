package ru.andvl.chatkeep.domain.service.locks.detectors

import dev.inmo.tgbotapi.types.message.abstracts.ChatEventMessage
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.domain.model.locks.LockType
import ru.andvl.chatkeep.domain.service.locks.AbstractLockDetector
import ru.andvl.chatkeep.domain.service.locks.DetectionContext

@Component
class ServiceMessageLockDetector : AbstractLockDetector() {
    override val lockType = LockType.SERVICE

    override suspend fun detect(message: ContentMessage<*>, context: DetectionContext): Boolean {
        // ChatEventMessage contains service messages like new member, left member, etc
        return message is ChatEventMessage<*>
    }
}
