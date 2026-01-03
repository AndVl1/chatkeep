package ru.andvl.chatkeep.domain.service.locks.detectors

import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.textsources.URLTextSource
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.domain.model.locks.LockType
import ru.andvl.chatkeep.domain.service.locks.AbstractLockDetector
import ru.andvl.chatkeep.domain.service.locks.DetectionContext

@Component
class InviteLockDetector : AbstractLockDetector() {
    override val lockType = LockType.INVITE

    private val invitePatterns = listOf(
        Regex("t\\.me/\\+[a-zA-Z0-9_-]+", RegexOption.IGNORE_CASE),
        Regex("t\\.me/joinchat/[a-zA-Z0-9_-]+", RegexOption.IGNORE_CASE),
        Regex("telegram\\.me/\\+[a-zA-Z0-9_-]+", RegexOption.IGNORE_CASE),
        Regex("telegram\\.me/joinchat/[a-zA-Z0-9_-]+", RegexOption.IGNORE_CASE)
    )

    override suspend fun detect(message: ContentMessage<*>, context: DetectionContext): Boolean {
        val textSources = message.getTextSources()

        val urlSources = textSources.filterIsInstance<URLTextSource>()

        return urlSources.any { urlSource ->
            val url = urlSource.source
            invitePatterns.any { pattern -> pattern.containsMatchIn(url) }
        }
    }
}
