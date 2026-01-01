package ru.andvl.chatkeep.bot.handlers.moderation

import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.types.chat.GroupChat
import dev.inmo.tgbotapi.types.chat.SupergroupChat
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.abstracts.FromUserMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.bot.handlers.Handler
import ru.andvl.chatkeep.domain.service.logchannel.LogChannelService
import ru.andvl.chatkeep.domain.service.moderation.AdminCacheService

/**
 * Handler for log channel configuration commands.
 *
 * Commands:
 * - /setlogchannel <channel_id> - Set the log channel for moderation actions
 * - /unsetlogchannel - Remove the log channel configuration
 * - /logchannel - Show current log channel configuration
 */
@Component
class LogChannelCommandHandler(
    private val logChannelService: LogChannelService,
    private val adminCacheService: AdminCacheService
) : Handler {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun BehaviourContext.register() {
        val groupFilter = { msg: CommonMessage<*> ->
            msg.chat is GroupChat || msg.chat is SupergroupChat
        }

        onCommand("setlogchannel", requireOnlyCommandInMessage = false, initialFilter = groupFilter) { message ->
            handleSetLogChannel(message)
        }

        onCommand("unsetlogchannel", initialFilter = groupFilter) { message ->
            handleUnsetLogChannel(message)
        }

        onCommand("logchannel", initialFilter = groupFilter) { message ->
            handleShowLogChannel(message)
        }
    }

    private suspend fun BehaviourContext.handleSetLogChannel(message: CommonMessage<*>) {
        val chatId = message.chat.id.chatId.long
        val adminId = (message as? FromUserMessage)?.from?.id?.chatId?.long ?: return

        // Check admin status
        val isAdmin = withContext(Dispatchers.IO) {
            adminCacheService.isAdmin(adminId, chatId)
        }

        if (!isAdmin) {
            reply(message, "You must be an admin to use this command.")
            return
        }

        // Parse channel ID from arguments
        val textContent = message.content as? TextContent
        val args = textContent?.text?.split(" ")?.drop(1) ?: emptyList()
        val channelId = args.firstOrNull()?.toLongOrNull()

        if (channelId == null) {
            reply(
                message,
                """
                Usage: /setlogchannel <channel_id>

                To find your channel ID:
                1. Add the bot to your log channel as admin
                2. Forward any message from the channel to @userinfobot
                3. Use the channel ID (starts with -100)

                Example: /setlogchannel -1001234567890
                """.trimIndent()
            )
            return
        }

        // Validate channel access
        val canAccess = logChannelService.validateChannel(channelId)
        if (!canAccess) {
            reply(
                message,
                """
                Cannot access channel $channelId.

                Make sure:
                1. The bot is added to the channel as admin
                2. The channel ID is correct (should start with -100)
                """.trimIndent()
            )
            return
        }

        // Set log channel
        withContext(Dispatchers.IO) {
            logChannelService.setLogChannel(chatId, channelId)
        }

        reply(message, "Log channel set to $channelId. All moderation actions will be logged there.")
        logger.info("Set log channel for chat $chatId to $channelId by admin $adminId")
    }

    private suspend fun BehaviourContext.handleUnsetLogChannel(message: CommonMessage<*>) {
        val chatId = message.chat.id.chatId.long
        val adminId = (message as? FromUserMessage)?.from?.id?.chatId?.long ?: return

        // Check admin status
        val isAdmin = withContext(Dispatchers.IO) {
            adminCacheService.isAdmin(adminId, chatId)
        }

        if (!isAdmin) {
            reply(message, "You must be an admin to use this command.")
            return
        }

        val removed = withContext(Dispatchers.IO) {
            logChannelService.unsetLogChannel(chatId)
        }

        if (removed) {
            reply(message, "Log channel removed. Moderation actions will no longer be logged to a channel.")
        } else {
            reply(message, "No log channel was configured for this chat.")
        }
    }

    private suspend fun BehaviourContext.handleShowLogChannel(message: CommonMessage<*>) {
        val chatId = message.chat.id.chatId.long
        val adminId = (message as? FromUserMessage)?.from?.id?.chatId?.long ?: return

        // Check admin status
        val isAdmin = withContext(Dispatchers.IO) {
            adminCacheService.isAdmin(adminId, chatId)
        }

        if (!isAdmin) {
            reply(message, "You must be an admin to use this command.")
            return
        }

        val logChannelId = withContext(Dispatchers.IO) {
            logChannelService.getLogChannel(chatId)
        }

        if (logChannelId != null) {
            reply(message, "Current log channel: $logChannelId")
        } else {
            reply(message, "No log channel configured. Use /setlogchannel <channel_id> to set one.")
        }
    }
}
