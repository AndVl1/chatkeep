package ru.andvl.chatkeep.bot.handlers.channelreply

import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onContentMessage
import dev.inmo.tgbotapi.types.chat.PrivateChat
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.AnimationContent
import dev.inmo.tgbotapi.types.message.content.DocumentContent
import dev.inmo.tgbotapi.types.message.content.MessageContent
import dev.inmo.tgbotapi.types.message.content.PhotoContent
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.types.message.content.VideoContent
import dev.inmo.tgbotapi.types.message.textsources.BotCommandTextSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.bot.handlers.Handler
import ru.andvl.chatkeep.bot.util.SessionAuthHelper
import ru.andvl.chatkeep.domain.model.channelreply.MediaType
import ru.andvl.chatkeep.domain.service.channelreply.ChannelReplyService

@Component
class ReplyMediaHandler(
    private val channelReplyService: ChannelReplyService,
    private val sessionAuthHelper: SessionAuthHelper
) : Handler {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val DISABLED_REMINDER = "\n\nNote: Auto-reply is currently DISABLED. Use /replyenable to activate it."
    }

    override suspend fun BehaviourContext.register() {
        val privateFilter = { msg: CommonMessage<*> -> msg.chat is PrivateChat }

        // Handle /replymedia with attached photo
        onContentMessage(
            initialFilter = { message ->
                message.chat is PrivateChat &&
                    message.content is PhotoContent &&
                    hasReplyMediaCommand(message.content)
            }
        ) { message ->
            @Suppress("UNCHECKED_CAST")
            handleMediaAttachment(message as CommonMessage<PhotoContent>)
        }

        // Handle /replymedia with attached video
        onContentMessage(
            initialFilter = { message ->
                message.chat is PrivateChat &&
                    message.content is VideoContent &&
                    hasReplyMediaCommand(message.content)
            }
        ) { message ->
            @Suppress("UNCHECKED_CAST")
            handleMediaAttachment(message as CommonMessage<VideoContent>)
        }

        // Handle /replymedia with attached document
        onContentMessage(
            initialFilter = { message ->
                message.chat is PrivateChat &&
                    message.content is DocumentContent &&
                    hasReplyMediaCommand(message.content)
            }
        ) { message ->
            @Suppress("UNCHECKED_CAST")
            handleMediaAttachment(message as CommonMessage<DocumentContent>)
        }

        // Handle /replymedia with attached animation (GIF)
        onContentMessage(
            initialFilter = { message ->
                message.chat is PrivateChat &&
                    message.content is AnimationContent &&
                    hasReplyMediaCommand(message.content)
            }
        ) { message ->
            @Suppress("UNCHECKED_CAST")
            handleMediaAttachment(message as CommonMessage<AnimationContent>)
        }

        // Handle /replymedia as reply to a message with media
        onCommand("replymedia", initialFilter = privateFilter) { message ->
            handleReplyToMedia(message)
        }

        // Handle /clearmedia command
        onCommand("clearmedia", initialFilter = privateFilter) { message ->
            handleClearMedia(message)
        }
    }

    private fun hasReplyMediaCommand(content: MessageContent): Boolean {
        val textSources = when (content) {
            is PhotoContent -> content.textSources
            is VideoContent -> content.textSources
            is DocumentContent -> content.textSources
            is AnimationContent -> content.textSources
            else -> null
        }
        return textSources
            ?.filterIsInstance<BotCommandTextSource>()
            ?.any { it.command == "replymedia" }
            ?: false
    }

    private suspend fun <T : MessageContent> BehaviourContext.handleMediaAttachment(
        message: CommonMessage<T>
    ) {
        sessionAuthHelper.withSessionAuth(this, message) { ctx ->
            val (fileId, mediaType) = extractMediaInfo(message.content)

            if (fileId == null || mediaType == null) {
                reply(message, buildString {
                    appendLine(sessionAuthHelper.formatReplyPrefix(ctx.session))
                    appendLine()
                    append("Unsupported media type.")
                })
                return@withSessionAuth
            }

            withContext(Dispatchers.IO) {
                channelReplyService.setMedia(ctx.chatId, fileId, mediaType)
            }

            val settings = withContext(Dispatchers.IO) {
                channelReplyService.getSettings(ctx.chatId)
            }

            reply(message, buildString {
                appendLine(sessionAuthHelper.formatReplyPrefix(ctx.session))
                appendLine()
                append("Media set (${mediaType.name.lowercase()}).")
                if (settings?.enabled != true) {
                    append(DISABLED_REMINDER)
                }
            })

            logger.info("Set channel reply media for chat ${ctx.chatId} by user ${ctx.userId} (type: ${mediaType.name})")
        }
    }

    private suspend fun BehaviourContext.handleReplyToMedia(message: CommonMessage<TextContent>) {
        sessionAuthHelper.withSessionAuth(this, message) { ctx ->
            @Suppress("UNCHECKED_CAST")
            val replyTo = message.replyTo as? ContentMessage<MessageContent>

            if (replyTo == null) {
                reply(message, buildString {
                    appendLine(sessionAuthHelper.formatReplyPrefix(ctx.session))
                    appendLine()
                    appendLine("Usage:")
                    appendLine("1. Send media with /replymedia as caption")
                    appendLine("2. Reply to a message with media using /replymedia")
                })
                return@withSessionAuth
            }

            val (fileId, mediaType) = extractMediaInfo(replyTo.content)

            if (fileId == null || mediaType == null) {
                reply(message, buildString {
                    appendLine(sessionAuthHelper.formatReplyPrefix(ctx.session))
                    appendLine()
                    append("The replied message doesn't contain supported media (photo, video, document, or GIF).")
                })
                return@withSessionAuth
            }

            withContext(Dispatchers.IO) {
                channelReplyService.setMedia(ctx.chatId, fileId, mediaType)
            }

            val settings = withContext(Dispatchers.IO) {
                channelReplyService.getSettings(ctx.chatId)
            }

            reply(message, buildString {
                appendLine(sessionAuthHelper.formatReplyPrefix(ctx.session))
                appendLine()
                append("Media set (${mediaType.name.lowercase()}).")
                if (settings?.enabled != true) {
                    append(DISABLED_REMINDER)
                }
            })

            logger.info("Set channel reply media for chat ${ctx.chatId} by user ${ctx.userId} (type: ${mediaType.name})")
        }
    }

    private suspend fun BehaviourContext.handleClearMedia(message: CommonMessage<*>) {
        sessionAuthHelper.withSessionAuth(this, message) { ctx ->
            withContext(Dispatchers.IO) {
                channelReplyService.clearMedia(ctx.chatId)
            }

            val settings = withContext(Dispatchers.IO) {
                channelReplyService.getSettings(ctx.chatId)
            }

            reply(message, buildString {
                appendLine(sessionAuthHelper.formatReplyPrefix(ctx.session))
                appendLine()
                append("Media cleared.")
                if (settings?.enabled != true) {
                    append(DISABLED_REMINDER)
                }
            })

            logger.info("Cleared channel reply media for chat ${ctx.chatId} by user ${ctx.userId}")
        }
    }

    private fun extractMediaInfo(content: MessageContent): Pair<String?, MediaType?> {
        return when (content) {
            is PhotoContent -> {
                // content.media is already the biggest PhotoSize
                content.media.fileId.fileId to MediaType.PHOTO
            }
            is VideoContent -> {
                content.media.fileId.fileId to MediaType.VIDEO
            }
            is DocumentContent -> {
                content.media.fileId.fileId to MediaType.DOCUMENT
            }
            is AnimationContent -> {
                content.media.fileId.fileId to MediaType.ANIMATION
            }
            else -> null to null
        }
    }
}
