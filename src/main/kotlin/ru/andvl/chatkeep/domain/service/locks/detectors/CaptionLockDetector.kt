package ru.andvl.chatkeep.domain.service.locks.detectors

import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.*
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.domain.model.locks.LockType
import ru.andvl.chatkeep.domain.service.locks.AbstractLockDetector
import ru.andvl.chatkeep.domain.service.locks.DetectionContext

@Component
class CaptionLockDetector : AbstractLockDetector() {
    override val lockType = LockType.CAPTION

    override suspend fun detect(message: ContentMessage<*>, context: DetectionContext): Boolean {
        val content = message.content

        return when (content) {
            is PhotoContent -> content.textSources?.isNotEmpty() == true
            is VideoContent -> content.textSources?.isNotEmpty() == true
            is DocumentContent -> content.textSources?.isNotEmpty() == true
            is AnimationContent -> content.textSources?.isNotEmpty() == true
            is AudioContent -> content.textSources?.isNotEmpty() == true
            is VoiceContent -> content.textSources?.isNotEmpty() == true
            else -> false
        }
    }
}
