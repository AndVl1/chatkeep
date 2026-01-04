package ru.andvl.chatkeep.domain.service.locks.detectors

import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.AudioContent
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.domain.model.locks.LockType
import ru.andvl.chatkeep.domain.service.locks.DetectionContext
import ru.andvl.chatkeep.domain.service.locks.LockDetector

@Component
class AudioLockDetector : LockDetector {
    override val lockType = LockType.AUDIO
    
    override suspend fun detect(message: ContentMessage<*>, context: DetectionContext): Boolean {
        return message.content is AudioContent
    }
}
