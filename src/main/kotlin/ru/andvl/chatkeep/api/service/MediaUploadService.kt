package ru.andvl.chatkeep.api.service

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.requests.abstracts.InputFile
import dev.inmo.tgbotapi.requests.send.media.*
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.message.content.*
import dev.inmo.tgbotapi.types.toChatId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import ru.andvl.chatkeep.api.config.MediaUploadConfig
import ru.andvl.chatkeep.api.exception.ValidationException
import ru.andvl.chatkeep.domain.model.channelreply.MediaType
import java.io.File
import java.nio.file.Files

@Service
class MediaUploadService(
    private val bot: TelegramBot,
    private val config: MediaUploadConfig
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Upload file to Telegram Bot API and return file_id and media type.
     * @param file MultipartFile to upload
     * @param chatId Telegram chat ID to send the file to (for verification)
     * @return Pair of (file_id, mediaType)
     * @throws ValidationException if file is invalid
     */
    suspend fun uploadToTelegram(file: MultipartFile, chatId: Long): Pair<String, String> {
        // Validate file size
        val fileSizeBytes = file.size
        val maxSizeBytes = config.maxFileSizeMb * 1024 * 1024
        if (fileSizeBytes > maxSizeBytes) {
            throw ValidationException("File size exceeds maximum allowed size of ${config.maxFileSizeMb}MB")
        }

        // Validate mime type
        val mimeType = file.contentType
        if (mimeType == null || mimeType !in config.allowedMimeTypes) {
            throw ValidationException("File type '$mimeType' is not allowed. Allowed types: ${config.allowedMimeTypes.joinToString(", ")}")
        }

        // Determine media type and upload
        val mediaType = determineMediaType(mimeType)
        if (mediaType.name !in config.allowedMediaTypes) {
            throw ValidationException("Media type '$mediaType' is not allowed")
        }

        logger.info("Uploading file: name=${file.originalFilename}, size=$fileSizeBytes, type=$mimeType, mediaType=$mediaType")

        val fileId = try {
            uploadFile(file, mediaType, chatId.toChatId())
        } catch (e: Exception) {
            logger.error("Failed to upload file to Telegram: ${e.message}", e)
            throw ValidationException("Failed to upload file to Telegram: ${e.message}")
        }

        logger.info("File uploaded successfully: fileId=$fileId, mediaType=$mediaType")
        return Pair(fileId, mediaType.name)
    }

    /**
     * Determine media type based on MIME type.
     */
    private fun determineMediaType(mimeType: String): MediaType {
        return when {
            mimeType.startsWith("image/") -> {
                if (mimeType == "image/gif") MediaType.ANIMATION else MediaType.PHOTO
            }
            mimeType.startsWith("video/") -> MediaType.VIDEO
            else -> MediaType.DOCUMENT
        }
    }

    /**
     * Upload file to Telegram and extract file_id.
     * Creates a temporary file to send to Telegram, then deletes it.
     */
    private suspend fun uploadFile(file: MultipartFile, mediaType: MediaType, chatId: ChatId): String {
        // Create temporary file
        val tempFile = withContext(Dispatchers.IO) {
            Files.createTempFile("upload_", ".tmp").toFile()
        }

        try {
            // Transfer multipart file to temp file
            withContext(Dispatchers.IO) {
                file.transferTo(tempFile)
            }

            // Send file to Telegram using appropriate method based on media type
            val message = when (mediaType) {
                MediaType.PHOTO -> {
                    bot.execute(
                        SendPhoto(
                            chatId = chatId,
                            photo = InputFile.fromFile(tempFile)
                        )
                    )
                }
                MediaType.VIDEO -> {
                    bot.execute(
                        SendVideo(
                            chatId = chatId,
                            video = InputFile.fromFile(tempFile)
                        )
                    )
                }
                MediaType.ANIMATION -> {
                    bot.execute(
                        SendAnimation(
                            chatId = chatId,
                            animation = InputFile.fromFile(tempFile)
                        )
                    )
                }
                MediaType.DOCUMENT -> {
                    bot.execute(
                        SendDocument(
                            chatId = chatId,
                            document = InputFile.fromFile(tempFile)
                        )
                    )
                }
            }

            // Extract file_id from the message content
            return when (val content = message.content) {
                is PhotoContent -> {
                    // content.media is already the biggest PhotoSize
                    content.media.fileId.fileId
                }
                is VideoContent -> content.media.fileId.fileId
                is AnimationContent -> content.media.fileId.fileId
                is DocumentContent -> content.media.fileId.fileId
                else -> throw IllegalStateException("Unexpected message content type: ${content::class.simpleName}")
            }
        } finally {
            // Cleanup temporary file
            withContext(Dispatchers.IO) {
                if (tempFile.exists()) {
                    tempFile.delete()
                }
            }
        }
    }
}
