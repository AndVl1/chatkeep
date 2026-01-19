package ru.andvl.chatkeep.domain.service.media

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class MediaCleanupScheduler(
    private val mediaStorageService: MediaStorageService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 0 3 * * *") // Every day at 3:00 AM
    fun cleanupOrphanMedia() {
        logger.info("Starting orphan media cleanup")
        try {
            val deletedCount = mediaStorageService.deleteOrphanMedia(olderThanDays = 7)
            logger.info("Orphan media cleanup completed: $deletedCount entries deleted")
        } catch (e: Exception) {
            logger.error("Failed to cleanup orphan media: ${e.message}", e)
        }
    }
}
