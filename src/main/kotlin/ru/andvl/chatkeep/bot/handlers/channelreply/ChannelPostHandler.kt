package ru.andvl.chatkeep.bot.handlers.channelreply

import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.replyWithAnimation
import dev.inmo.tgbotapi.extensions.api.send.replyWithDocument
import dev.inmo.tgbotapi.extensions.api.send.replyWithPhoto
import dev.inmo.tgbotapi.extensions.api.send.replyWithVideo
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onContentMessage
import dev.inmo.tgbotapi.requests.abstracts.FileId
import dev.inmo.tgbotapi.requests.abstracts.InputFile
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.URLInlineKeyboardButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.chat.ChannelChat
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage
import dev.inmo.tgbotapi.types.message.abstracts.ConnectedFromChannelGroupContentMessage
import dev.inmo.tgbotapi.types.message.content.AnimationContent
import dev.inmo.tgbotapi.types.message.content.DocumentContent
import dev.inmo.tgbotapi.types.message.content.PhotoContent
import dev.inmo.tgbotapi.types.message.content.VideoContent
import dev.inmo.tgbotapi.utils.matrix
import dev.inmo.tgbotapi.utils.row
import dev.inmo.tgbotapi.utils.RiskFeature
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.bot.handlers.Handler
import ru.andvl.chatkeep.domain.model.channelreply.ChannelReplySettings
import ru.andvl.chatkeep.domain.model.channelreply.MediaType
import ru.andvl.chatkeep.domain.service.channelreply.ChannelReplyService

@Component
class ChannelPostHandler(
    private val channelReplyService: ChannelReplyService,
    private val mediaStorageService: ru.andvl.chatkeep.domain.service.media.MediaStorageService
) : Handler {

    private val logger = LoggerFactory.getLogger(javaClass)

    @OptIn(RiskFeature::class)
    override suspend fun BehaviourContext.register() {
        onContentMessage(
            initialFilter = { message ->
                // Filter: only messages from LINKED channel (automatic forwards)
                // ConnectedFromChannelGroupContentMessage = posts from the channel that is linked to this discussion group
                // This excludes: messages from unlinked channels, anonymous admin messages, etc.
                message is ConnectedFromChannelGroupContentMessage<*> && message.chat !is ChannelChat
            },
            markerFactory = null
        ) { message ->
            try {
                val chatId = message.chat.id.chatId.long

                // Get settings for chat
                val settings = withContext(Dispatchers.IO) {
                    channelReplyService.getSettings(chatId)
                }

                if (settings == null || !settings.enabled) {
                    logger.debug("Channel reply disabled or not configured for chat $chatId")
                    return@onContentMessage
                }

                // Check if we have something to send (text or media)
                val hasText = !settings.replyText.isNullOrBlank()
                val hasMediaHash = !settings.mediaHash.isNullOrBlank()
                val hasLegacyMedia = !settings.mediaFileId.isNullOrBlank() && !settings.mediaType.isNullOrBlank()

                if (!hasText && !hasMediaHash && !hasLegacyMedia) {
                    logger.debug("Channel reply has no text or media for chat $chatId")
                    return@onContentMessage
                }

                // Build URL keyboard if buttons configured
                val keyboard = buildKeyboard(settings)

                // Determine which media approach to use
                when {
                    // Hash-based media (BLOB storage)
                    hasMediaHash -> {
                        sendMediaReplyWithBlob(message, settings, keyboard)
                    }
                    // Legacy media (file_id already cached)
                    hasLegacyMedia -> {
                        sendMediaReply(message, settings, keyboard)
                    }
                    // Text-only reply
                    hasText -> {
                        reply(
                            to = message,
                            text = settings.replyText!!,
                            replyMarkup = keyboard
                        )
                    }
                }

                logger.info("Sent channel reply for chat $chatId")
            } catch (e: Exception) {
                logger.error("Failed to send channel reply: ${e.message}", e)
            }
        }
    }

    private fun buildKeyboard(settings: ChannelReplySettings): InlineKeyboardMarkup? {
        if (settings.buttonsJson.isNullOrBlank()) return null
        val buttons = channelReplyService.parseButtons(settings.buttonsJson)
        if (buttons.isEmpty()) return null
        return InlineKeyboardMarkup(
            keyboard = matrix {
                buttons.forEach { button ->
                    row { +URLInlineKeyboardButton(button.text, button.url) }
                }
            }
        )
    }

    private suspend fun BehaviourContext.sendMediaReply(
        message: AccessibleMessage,
        settings: ChannelReplySettings,
        keyboard: InlineKeyboardMarkup?
    ) {
        val fileId = FileId(settings.mediaFileId!!)
        val caption = settings.replyText // Can be null

        when (MediaType.valueOf(settings.mediaType!!)) {
            MediaType.PHOTO -> replyWithPhoto(
                to = message,
                fileId = fileId,
                text = caption,
                replyMarkup = keyboard
            )
            MediaType.VIDEO -> replyWithVideo(
                to = message,
                video = fileId,
                text = caption,
                replyMarkup = keyboard
            )
            MediaType.DOCUMENT -> replyWithDocument(
                to = message,
                document = fileId,
                text = caption,
                replyMarkup = keyboard
            )
            MediaType.ANIMATION -> replyWithAnimation(
                to = message,
                animation = fileId,
                text = caption,
                replyMarkup = keyboard
            )
        }
    }

    /**
     * Sends media reply using BLOB storage.
     * If file_id is not cached, uploads from BLOB and captures file_id from the actual reply message.
     */
    private suspend fun BehaviourContext.sendMediaReplyWithBlob(
        message: AccessibleMessage,
        settings: ChannelReplySettings,
        keyboard: InlineKeyboardMarkup?
    ) {
        val mediaHash = settings.mediaHash!!

        // Check if file_id is already cached
        val cachedFileId = withContext(Dispatchers.IO) {
            mediaStorageService.getFileId(mediaHash)
        }

        if (cachedFileId != null) {
            // Use cached file_id
            logger.debug("Using cached file_id for media hash $mediaHash")
            val settingsWithFileId = settings.copy(mediaFileId = cachedFileId)
            sendMediaReply(message, settingsWithFileId, keyboard)
            return
        }

        // No cached file_id - need to upload from BLOB
        logger.info("No cached file_id for media hash $mediaHash, uploading from BLOB")

        val media = withContext(Dispatchers.IO) {
            mediaStorageService.getMedia(mediaHash)
        } ?: run {
            logger.error("Media not found for hash $mediaHash")
            // Fallback to text-only reply
            if (!settings.replyText.isNullOrBlank()) {
                reply(
                    to = message,
                    text = settings.replyText,
                    replyMarkup = keyboard
                )
            }
            return
        }

        // Create temp file from BLOB content
        val tempFile = withContext(Dispatchers.IO) {
            java.nio.file.Files.createTempFile("media_", ".tmp").toFile().apply {
                writeBytes(media.content)
            }
        }

        try {
            val caption = settings.replyText

            // Send media as reply and capture file_id from response
            val sentMessage = when {
                media.mimeType.startsWith("image/") -> {
                    replyWithPhoto(
                        to = message,
                        fileId = InputFile.fromFile(tempFile),
                        text = caption,
                        replyMarkup = keyboard
                    )
                }
                media.mimeType.startsWith("video/") -> {
                    replyWithVideo(
                        to = message,
                        video = InputFile.fromFile(tempFile),
                        text = caption,
                        replyMarkup = keyboard
                    )
                }
                media.mimeType == "image/gif" -> {
                    replyWithAnimation(
                        to = message,
                        animation = InputFile.fromFile(tempFile),
                        text = caption,
                        replyMarkup = keyboard
                    )
                }
                else -> {
                    replyWithDocument(
                        to = message,
                        document = InputFile.fromFile(tempFile),
                        text = caption,
                        replyMarkup = keyboard
                    )
                }
            }

            // Extract file_id from sent message content
            val extractedFileId = when (val content = sentMessage.content) {
                is PhotoContent -> content.media.fileId.fileId
                is VideoContent -> content.media.fileId.fileId
                is DocumentContent -> content.media.fileId.fileId
                is AnimationContent -> content.media.fileId.fileId
                else -> {
                    logger.warn("Unexpected message content type: ${content::class.simpleName}")
                    null
                }
            }

            // Save file_id to database for future reuse
            if (extractedFileId != null) {
                withContext(Dispatchers.IO) {
                    mediaStorageService.saveFileId(mediaHash, extractedFileId)
                }
                logger.info("Captured and saved file_id for media hash $mediaHash: $extractedFileId")
            }
        } finally {
            // Cleanup temp file
            withContext(Dispatchers.IO) {
                if (tempFile.exists()) {
                    tempFile.delete()
                }
            }
        }
    }
}
