package ru.andvl.chatkeep.domain.service.locks.detectors

import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.LocationContent
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.domain.model.locks.LockType
import ru.andvl.chatkeep.domain.service.locks.DetectionContext
import ru.andvl.chatkeep.domain.service.locks.LockDetector

@Component
class LocationLockDetector : LockDetector {
    override val lockType = LockType.LOCATION
    
    override suspend fun detect(message: ContentMessage<*>, context: DetectionContext): Boolean {
        return message.content is LocationContent
    }
}
