package ru.andvl.chatkeep.domain.service.locks

import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import ru.andvl.chatkeep.domain.model.locks.LockType

/**
 * Interface for lock type detectors.
 * Each implementation handles detection for a specific LockType.
 */
interface LockDetector {
    /**
     * The lock type this detector handles
     */
    val lockType: LockType

    /**
     * Check if the message violates this lock
     * @param message The message to check
     * @param context Additional context for detection (allowlisted URLs, etc)
     * @return true if the message should be deleted (lock violated)
     */
    suspend fun detect(
        message: ContentMessage<*>,
        context: DetectionContext
    ): Boolean
}

/**
 * Context passed to detectors with shared data
 */
data class DetectionContext(
    val chatId: Long,
    val allowlistedUrls: Set<String> = emptySet(),
    val allowlistedDomains: Set<String> = emptySet(),
    val allowlistedCommands: Set<String> = emptySet()
)
