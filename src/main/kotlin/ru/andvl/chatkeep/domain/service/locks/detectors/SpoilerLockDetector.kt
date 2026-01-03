package ru.andvl.chatkeep.domain.service.locks.detectors

import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.textsources.TextSource
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.domain.model.locks.LockType
import ru.andvl.chatkeep.domain.service.locks.AbstractLockDetector
import ru.andvl.chatkeep.domain.service.locks.DetectionContext

@Component
class SpoilerLockDetector : AbstractLockDetector() {
    override val lockType = LockType.SPOILER

    override suspend fun detect(message: ContentMessage<*>, context: DetectionContext): Boolean {
        val textSources = message.getTextSources()

        return textSources.any { textSource ->
            hasSpoilerFormatting(textSource)
        }
    }

    private fun hasSpoilerFormatting(textSource: TextSource): Boolean {
        // Check if the text source has spoiler formatting
        // This can be done by checking the styling property
        return runCatching {
            val stylingField = textSource::class.java.getDeclaredField("styling")
            stylingField.isAccessible = true
            val styling = stylingField.get(textSource)

            styling?.let { styles ->
                val isSpoiler = styles.toString().contains("Spoiler", ignoreCase = true)
                isSpoiler
            } ?: false
        }.getOrDefault(false)
    }
}
