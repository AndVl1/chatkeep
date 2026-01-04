package ru.andvl.chatkeep.domain.service.locks.detectors

import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.StickerContent
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.domain.model.locks.LockType
import ru.andvl.chatkeep.domain.service.locks.DetectionContext
import ru.andvl.chatkeep.domain.service.locks.LockDetector

@Component
class StickerLockDetector : LockDetector {
    override val lockType = LockType.STICKER
    
    override suspend fun detect(message: ContentMessage<*>, context: DetectionContext): Boolean {
        return message.content is StickerContent
    }
}
