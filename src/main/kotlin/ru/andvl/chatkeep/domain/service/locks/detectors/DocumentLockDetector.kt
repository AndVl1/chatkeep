package ru.andvl.chatkeep.domain.service.locks.detectors

import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.DocumentContent
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.domain.model.locks.LockType
import ru.andvl.chatkeep.domain.service.locks.DetectionContext
import ru.andvl.chatkeep.domain.service.locks.LockDetector

@Component
class DocumentLockDetector : LockDetector {
    override val lockType = LockType.DOCUMENT
    
    override suspend fun detect(message: ContentMessage<*>, context: DetectionContext): Boolean {
        return message.content is DocumentContent
    }
}
