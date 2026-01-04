package ru.andvl.chatkeep.domain.service.locks.detectors

import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.abstracts.PossiblyTopicMessage
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.domain.model.locks.LockType
import ru.andvl.chatkeep.domain.service.locks.AbstractLockDetector
import ru.andvl.chatkeep.domain.service.locks.DetectionContext

/**
 * Detects messages in topics/forum threads.
 */
@Component
class TopicLockDetector : AbstractLockDetector() {
    override val lockType = LockType.TOPIC

    override suspend fun detect(message: ContentMessage<*>, context: DetectionContext): Boolean {
        // PossiblyTopicMessage indicates message might be in a topic
        if (message !is PossiblyTopicMessage) return false
        return message.threadId != null
    }
}
