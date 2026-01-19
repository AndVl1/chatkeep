package ru.andvl.chatkeep.domain.service.media

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.andvl.chatkeep.domain.model.media.MediaStorage
import ru.andvl.chatkeep.infrastructure.repository.media.MediaStorageRepository
import java.security.MessageDigest
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class MediaStorageService(
    private val repository: MediaStorageRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun calculateMd5(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    @Transactional
    fun storeMedia(file: org.springframework.web.multipart.MultipartFile): String {
        val bytes = file.bytes
        val hash = calculateMd5(bytes)

        // Check if already exists
        repository.findByHash(hash)?.let {
            logger.debug("Media with hash $hash already exists, returning existing")
            return hash
        }

        // Store new media
        val mediaStorage = MediaStorage(
            hash = hash,
            content = bytes,
            mimeType = file.contentType ?: "application/octet-stream",
            fileSize = file.size
        )

        repository.save(mediaStorage)
        logger.info("Stored media with hash $hash (${file.size} bytes)")
        return hash
    }

    fun getMedia(hash: String): MediaStorage? {
        return repository.findByHash(hash)
    }

    fun getFileId(hash: String): String? {
        return repository.findByHash(hash)?.telegramFileId
    }

    @Transactional
    fun saveFileId(hash: String, fileId: String) {
        val media = repository.findByHash(hash)
            ?: throw IllegalArgumentException("Media with hash $hash not found")

        if (media.telegramFileId != null) {
            logger.debug("Media $hash already has file_id: ${media.telegramFileId}")
            return
        }

        val updated = media.copy(telegramFileId = fileId)
        repository.save(updated)
        logger.info("Saved file_id for media $hash: $fileId")
    }

    @Transactional
    fun deleteOrphanMedia(olderThanDays: Int): Int {
        val cutoff = Instant.now().minus(olderThanDays.toLong(), ChronoUnit.DAYS)
        val deleted = repository.deleteByCreatedAtBeforeAndTelegramFileIdIsNull(cutoff)
        logger.info("Deleted $deleted orphan media entries older than $olderThanDays days")
        return deleted
    }
}
