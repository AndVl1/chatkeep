package ru.andvl.chatkeep.domain.service.locks.detectors

import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.domain.model.locks.LockType
import ru.andvl.chatkeep.domain.service.locks.AbstractLockDetector
import ru.andvl.chatkeep.domain.service.locks.DetectionContext

@Component
class LinkPreviewLockDetector : AbstractLockDetector() {
    override val lockType = LockType.LINKPREVIEW

    override suspend fun detect(message: ContentMessage<*>, context: DetectionContext): Boolean {
        // Check if message has link preview via reflection
        return runCatching {
            val linkPreviewMethod = message::class.java.getDeclaredMethod("getLinkPreviewOptions")
            linkPreviewMethod.invoke(message) != null
        }.onFailure {
            // Try legacy webPage property
            runCatching {
                val webPageField = message::class.java.getDeclaredField("webPage")
                webPageField.isAccessible = true
                return webPageField.get(message) != null
            }
        }.getOrDefault(false)
    }
}
