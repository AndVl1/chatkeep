package ru.andvl.chatkeep.bot.handlers.moderation

import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.media.sendDocument
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.requests.abstracts.InputFile
import dev.inmo.tgbotapi.types.chat.PrivateChat
import dev.inmo.tgbotapi.types.message.abstracts.FromUserMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.bot.handlers.Handler
import ru.andvl.chatkeep.domain.service.adminlogs.AdminLogExportService
import ru.andvl.chatkeep.domain.service.moderation.AdminCacheService

/**
 * Handler for /viewlogs command in private chats.
 * Exports admin action logs for a specific chat as JSON and sends via Telegram.
 */
@Component
class AdminLogCommandHandler(
    private val adminLogExportService: AdminLogExportService,
    private val adminCacheService: AdminCacheService
) : Handler {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun BehaviourContext.register() {
        val privateChatFilter = { msg: dev.inmo.tgbotapi.types.message.abstracts.CommonMessage<*> ->
            msg.chat is PrivateChat
        }

        onCommand("viewlogs", requireOnlyCommandInMessage = false, initialFilter = privateChatFilter) { message ->
            handleViewLogs(message)
        }
    }

    private suspend fun BehaviourContext.handleViewLogs(
        message: dev.inmo.tgbotapi.types.message.abstracts.CommonMessage<*>
    ) {
        val userId = (message as? FromUserMessage)?.from?.id?.chatId?.long ?: run {
            reply(message, "Unable to identify user.")
            return
        }

        val textContent = message.content as? TextContent
        val args = textContent?.text?.split(" ")?.drop(1) ?: emptyList()

        // Parse chatId argument
        val chatId = args.firstOrNull()?.toLongOrNull()
        if (chatId == null) {
            reply(message, "Usage: /viewlogs <chatId>\n\nExample: /viewlogs -1001234567890")
            return
        }

        // Check if user is admin in the specified chat
        // Force fresh check (bypass cache) for sensitive log export operation (SEC-003)
        val isAdmin = withContext(Dispatchers.IO) {
            adminCacheService.isAdmin(userId, chatId, forceRefresh = true)
        }

        if (!isAdmin) {
            reply(message, "You must be an admin in chat $chatId to view logs.")
            return
        }

        // Export logs
        val exportFile = try {
            withContext(Dispatchers.IO) {
                adminLogExportService.exportLogs(chatId)
            }
        } catch (e: Exception) {
            logger.error("Failed to export admin logs for chatId=$chatId", e)
            reply(message, "Failed to export logs. Please try again later.")
            return
        }

        // Send document
        try {
            sendDocument(
                chatId = message.chat.id,
                document = InputFile.fromFile(exportFile),
                text = "Admin action logs for chat $chatId"
            )
            logger.info("Sent admin logs for chatId=$chatId to userId=$userId")
        } catch (e: Exception) {
            logger.error("Failed to send admin logs document for chatId=$chatId", e)
            reply(message, "Failed to send logs file.")
        } finally {
            // Cleanup: delete temporary file
            withContext(Dispatchers.IO) {
                adminLogExportService.deleteExportedFile(exportFile)
            }
        }
    }
}
