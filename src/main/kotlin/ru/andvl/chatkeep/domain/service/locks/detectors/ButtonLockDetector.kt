package ru.andvl.chatkeep.domain.service.locks.detectors

import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.domain.model.locks.LockType
import ru.andvl.chatkeep.domain.service.locks.AbstractLockDetector
import ru.andvl.chatkeep.domain.service.locks.DetectionContext

@Component
class ButtonLockDetector : AbstractLockDetector() {
    override val lockType = LockType.BUTTON

    override suspend fun detect(message: ContentMessage<*>, context: DetectionContext): Boolean {
        // Check if message has inline keyboard via casting to message with reply markup
        return runCatching {
            val replyMarkup = message::class.java.getDeclaredMethod("getReplyMarkup").invoke(message)
            replyMarkup is InlineKeyboardMarkup
        }.getOrDefault(false)
    }
}
