package ru.andvl.chatkeep.domain.service.locks.detectors

import dev.inmo.tgbotapi.types.files.CustomEmojiSticker
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.StickerContent
import dev.inmo.tgbotapi.types.message.textsources.CustomEmojiTextSource
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.domain.model.locks.LockType
import ru.andvl.chatkeep.domain.service.locks.AbstractLockDetector
import ru.andvl.chatkeep.domain.service.locks.DetectionContext

@Component
class PremiumLockDetector : AbstractLockDetector() {
    override val lockType = LockType.PREMIUM

    override suspend fun detect(message: ContentMessage<*>, context: DetectionContext): Boolean {
        // Check for premium custom emoji in text
        val textSources = message.getTextSources()
        if (textSources.any { it is CustomEmojiTextSource }) {
            return true
        }

        // Check for custom emoji stickers (premium animated emojis)
        if (message.content is StickerContent) {
            val sticker = (message.content as StickerContent).media
            if (sticker is CustomEmojiSticker) {
                return true
            }
        }

        return false
    }
}
