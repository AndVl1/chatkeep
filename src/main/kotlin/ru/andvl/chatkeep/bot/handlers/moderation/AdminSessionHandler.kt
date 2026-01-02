package ru.andvl.chatkeep.bot.handlers.moderation

import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.types.chat.PrivateChat
import dev.inmo.tgbotapi.types.message.abstracts.FromUserMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.bot.handlers.Handler
import ru.andvl.chatkeep.domain.service.AdminService
import ru.andvl.chatkeep.domain.service.moderation.AdminCacheService
import ru.andvl.chatkeep.domain.service.moderation.AdminSessionService

@Component
class AdminSessionHandler(
    private val adminSessionService: AdminSessionService,
    private val adminCacheService: AdminCacheService,
    private val adminService: AdminService
) : Handler {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun BehaviourContext.register() {
        val privateFilter = { msg: dev.inmo.tgbotapi.types.message.abstracts.CommonMessage<*> ->
            msg.chat is PrivateChat
        }

        onCommand("connect", requireOnlyCommandInMessage = false, initialFilter = privateFilter) { message ->
            logger.info("Received /connect command from chat ${message.chat.id}")
            handleConnect(message)
        }

        onCommand("disconnect", initialFilter = privateFilter) { message ->
            logger.info("Received /disconnect command from chat ${message.chat.id}")
            handleDisconnect(message)
        }
    }

    private suspend fun BehaviourContext.handleConnect(message: dev.inmo.tgbotapi.types.message.abstracts.CommonMessage<*>) {
        val userId = (message as? FromUserMessage)?.from?.id?.chatId?.long ?: run {
            logger.warn("/connect: Cannot extract user ID from message type ${message::class.simpleName}")
            return
        }

        val textContent = message.content as? TextContent
        val args = textContent?.text?.split(" ")?.drop(1) ?: emptyList()
        logger.debug("/connect: userId=$userId, args=$args")
        val chatId = args.firstOrNull()?.toLongOrNull()

        if (chatId == null) {
            reply(message, "Usage: /connect <chat_id>")
            return
        }

        // Verify user is admin in target chat
        val isAdmin = withContext(Dispatchers.IO) {
            adminCacheService.isAdmin(userId, chatId)
        }

        if (!isAdmin) {
            reply(message, "You are not an admin in this chat.")
            return
        }

        // Get chat info
        val stats = withContext(Dispatchers.IO) {
            adminService.getStatistics(chatId)
        }

        if (stats == null) {
            reply(message, "Chat not found or I'm not added to it.")
            return
        }

        withContext(Dispatchers.IO) {
            adminSessionService.connect(userId, chatId, stats.chatTitle)
        }

        reply(message, "Connected to: ${stats.chatTitle ?: "Chat $chatId"}\n\nAll moderation commands will now apply to this chat.")
        logger.info("Admin session connected: userId=$userId, chatId=$chatId")
    }

    private suspend fun BehaviourContext.handleDisconnect(message: dev.inmo.tgbotapi.types.message.abstracts.CommonMessage<*>) {
        val userId = (message as? FromUserMessage)?.from?.id?.chatId?.long ?: run {
            logger.warn("/disconnect: Cannot extract user ID from message type ${message::class.simpleName}")
            return
        }

        val session = withContext(Dispatchers.IO) {
            adminSessionService.getSession(userId)
        }

        if (session == null) {
            reply(message, "You are not connected to any chat.")
            return
        }

        withContext(Dispatchers.IO) {
            adminSessionService.disconnect(userId)
        }

        reply(message, "Disconnected from: ${session.connectedChatTitle ?: "Chat ${session.connectedChatId}"}")
        logger.info("Admin session disconnected: userId=$userId")
    }
}
