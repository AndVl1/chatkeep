package ru.andvl.chatkeep.domain.service.locks.detectors

import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.domain.model.locks.LockType
import ru.andvl.chatkeep.domain.service.locks.AbstractLockDetector
import ru.andvl.chatkeep.domain.service.locks.DetectionContext

@Component
class RtlCharLockDetector : AbstractLockDetector() {
    override val lockType = LockType.RTLCHAR

    private val rtlPattern = Regex("[\\u0600-\\u06FF\\u0750-\\u077F\\u08A0-\\u08FF\\uFB50-\\uFDFF\\uFE70-\\uFEFF\\u200F\\u202B\\u202E]")

    override suspend fun detect(message: ContentMessage<*>, context: DetectionContext): Boolean {
        val textSources = message.getTextSources()
        val text = textSources.joinToString("") { it.source }

        if (text.isNotEmpty() && rtlPattern.containsMatchIn(text)) {
            return true
        }

        // Also check plain text content
        if (message.content is TextContent) {
            val plainText = (message.content as TextContent).text
            return rtlPattern.containsMatchIn(plainText)
        }

        return false
    }
}
