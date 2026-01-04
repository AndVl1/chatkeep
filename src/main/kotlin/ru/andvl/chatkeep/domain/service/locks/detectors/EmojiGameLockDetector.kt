package ru.andvl.chatkeep.domain.service.locks.detectors

import dev.inmo.tgbotapi.types.dice.DiceAnimationType
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.DiceContent
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.domain.model.locks.LockType
import ru.andvl.chatkeep.domain.service.locks.AbstractLockDetector
import ru.andvl.chatkeep.domain.service.locks.DetectionContext

/**
 * Detects emoji game messages (dice with specific emojis like 🎯🎰🏀⚽🎳).
 * This includes all dice animation types except regular dice.
 */
@Component
class EmojiGameLockDetector : AbstractLockDetector() {
    override val lockType = LockType.EMOJIGAME

    override suspend fun detect(message: ContentMessage<*>, context: DetectionContext): Boolean {
        val diceContent = message.content as? DiceContent ?: return false

        // Game emojis have emojis other than regular dice 🎲
        // Check emoji: 🎯 (Darts), 🎰 (SlotMachine), 🏀 (Basketball), ⚽ (Football), 🎳 (Bowling)
        val emoji = diceContent.dice.animationType.emoji
        return emoji != "🎲"
    }
}
