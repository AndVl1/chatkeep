package ru.andvl.chatkeep.domain.service.locks.detectors

import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.textsources.URLTextSource
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.domain.model.locks.LockType
import ru.andvl.chatkeep.domain.service.locks.AbstractLockDetector
import ru.andvl.chatkeep.domain.service.locks.DetectionContext

/**
 * Detects URLs in text (entity-based links).
 * Note: This is similar to URL lock but specifically for the LINK type.
 */
@Component
class LinkLockDetector : AbstractLockDetector() {
    override val lockType = LockType.LINK

    override suspend fun detect(message: ContentMessage<*>, context: DetectionContext): Boolean {
        val textSources = message.getTextSources()

        val urlSources = textSources.filterIsInstance<URLTextSource>()
        if (urlSources.isEmpty()) return false

        // If no allowlist configured, all URLs are violations
        if (context.allowlistedUrls.isEmpty() && context.allowlistedDomains.isEmpty()) {
            return true
        }

        // Check if any URL is not allowlisted
        return urlSources.any { urlSource ->
            val url = urlSource.source
            val isUrlAllowlisted = context.allowlistedUrls.any { url.contains(it, ignoreCase = true) }
            val isDomainAllowlisted = context.allowlistedDomains.any { url.contains(it, ignoreCase = true) }

            !isUrlAllowlisted && !isDomainAllowlisted
        }
    }
}
