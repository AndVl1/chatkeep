package ru.andvl.chatkeep.domain.service.locks

import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.*
import dev.inmo.tgbotapi.types.message.textsources.TextSourcesList

abstract class AbstractLockDetector : LockDetector {

    /**
     * Helper to check if message has specific content type
     */
    protected inline fun <reified T : MessageContent> ContentMessage<*>.hasContent(): Boolean =
        this.content is T

    /**
     * Helper to get text sources from any content with text
     */
    protected fun ContentMessage<*>.getTextSources(): TextSourcesList = when (val c = content) {
        is TextContent -> c.textSources
        is PhotoContent -> c.textSources ?: emptyList()
        is VideoContent -> c.textSources ?: emptyList()
        is DocumentContent -> c.textSources ?: emptyList()
        is AnimationContent -> c.textSources ?: emptyList()
        is AudioContent -> c.textSources ?: emptyList()
        is VoiceContent -> c.textSources ?: emptyList()
        else -> emptyList()
    }
}
