package ru.andvl.chatkeep.domain.service.locks.detectors

import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.textsources.MentionTextSource
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.domain.model.locks.LockType
import ru.andvl.chatkeep.domain.service.locks.AbstractLockDetector
import ru.andvl.chatkeep.domain.service.locks.DetectionContext

@Component
class MentionLockDetector : AbstractLockDetector() {
    override val lockType = LockType.MENTION

    override suspend fun detect(message: ContentMessage<*>, context: DetectionContext): Boolean {
        val textSources = message.getTextSources()
        return textSources.any { it is MentionTextSource }
    }
}
