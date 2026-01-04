package ru.andvl.chatkeep.domain.service.locks.detectors

import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.domain.model.locks.LockType
import ru.andvl.chatkeep.domain.service.locks.AbstractLockDetector
import ru.andvl.chatkeep.domain.service.locks.DetectionContext

@Component
class TextLockDetector : AbstractLockDetector() {
    override val lockType = LockType.TEXT

    override suspend fun detect(message: ContentMessage<*>, context: DetectionContext): Boolean {
        return message.content is TextContent
    }
}
