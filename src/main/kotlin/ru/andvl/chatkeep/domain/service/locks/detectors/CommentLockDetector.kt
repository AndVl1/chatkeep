package ru.andvl.chatkeep.domain.service.locks.detectors

import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.abstracts.FromChannelGroupContentMessage
import dev.inmo.tgbotapi.types.message.abstracts.PossiblyReplyMessage
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.domain.model.locks.LockType
import ru.andvl.chatkeep.domain.service.locks.AbstractLockDetector
import ru.andvl.chatkeep.domain.service.locks.DetectionContext

/**
 * Detects comment messages (replies to channel posts in discussion groups).
 */
@Component
class CommentLockDetector : AbstractLockDetector() {
    override val lockType = LockType.COMMENT

    override suspend fun detect(message: ContentMessage<*>, context: DetectionContext): Boolean {
        // Comment is a reply to a channel post
        if (message !is PossiblyReplyMessage) return false

        val replyTo = message.replyTo ?: return false

        // Check if the replied-to message is from a channel
        return replyTo is FromChannelGroupContentMessage<*>
    }
}
