package ru.andvl.chatkeep.domain.service.media

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.types.toChatId
import dev.inmo.tgbotapi.requests.send.media.SendDocument
import dev.inmo.tgbotapi.requests.send.media.SendPhoto
import dev.inmo.tgbotapi.requests.send.media.SendVideo
import dev.inmo.tgbotapi.requests.abstracts.InputFile
import dev.inmo.tgbotapi.types.message.content.PhotoContent
import dev.inmo.tgbotapi.types.message.content.VideoContent
import dev.inmo.tgbotapi.types.message.content.DocumentContent
import kotlinx.coroutines.Dispatchers
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
    private val repository: MediaStorageRepository,
    private val bot: TelegramBot
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

    @Transactional
    suspend fun resolveToTelegramFileId(hash: String, chatId: Long): String {
        val media = repository.findByHash(hash)
            ?: throw IllegalArgumentException("Media with hash $hash not found")

        // If already uploaded, return existing file_id
        media.telegramFileId?.let {
            logger.debug("Media $hash already has telegram file_id: $it")
            return it
        }

        // Upload to Telegram
        logger.info("Uploading media $hash to Telegram (chat: $chatId)")
        val fileId = uploadMediaToTelegram(media, chatId)

        // Save file_id
        val updated = media.copy(telegramFileId = fileId)
        repository.save(updated)

        logger.info("Media $hash uploaded to Telegram, file_id: $fileId")
        return fileId
    }

    private suspend fun uploadMediaToTelegram(media: MediaStorage, chatId: Long): String {
        // Create temp file from byte array
        val tempFile = kotlinx.coroutines.withContext(Dispatchers.IO) {
            java.nio.file.Files.createTempFile("media_", ".tmp").toFile().apply {
                writeBytes(media.content)
            }
        }

        try {
            val telegramChatId = chatId.toChatId()

            val message = when {
                media.mimeType.startsWith("image/") -> {
                    bot.execute(
                        SendPhoto(
                            chatId = telegramChatId,
                            photo = InputFile.fromFile(tempFile)
                        )
                    )
                }
                media.mimeType.startsWith("video/") -> {
                    bot.execute(
                        SendVideo(
                            chatId = telegramChatId,
                            video = InputFile.fromFile(tempFile)
                        )
                    )
                }
                else -> {
                    bot.execute(
                        SendDocument(
                            chatId = telegramChatId,
                            document = InputFile.fromFile(tempFile)
                        )
                    )
                }
            }

            // Extract file_id from message content
            return when (val content = message.content) {
                is PhotoContent -> content.media.fileId.fileId
                is VideoContent -> content.media.fileId.fileId
                is DocumentContent -> content.media.fileId.fileId
                else -> throw IllegalStateException("Unexpected message content type: ${content::class.simpleName}")
            }
        } finally {
            // Cleanup temp file
            kotlinx.coroutines.withContext(Dispatchers.IO) {
                if (tempFile.exists()) {
                    tempFile.delete()
                }
            }
        }
    }

    @Transactional
    fun deleteOrphanMedia(olderThanDays: Int): Int {
        val cutoff = Instant.now().minus(olderThanDays.toLong(), ChronoUnit.DAYS)
        val deleted = repository.deleteByCreatedAtBeforeAndTelegramFileIdIsNull(cutoff)
        logger.info("Deleted $deleted orphan media entries older than $olderThanDays days")
        return deleted
    }
}
