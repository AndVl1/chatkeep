package ru.andvl.chatkeep.bot.handlers.moderation

import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.CallbackDataInlineKeyboardButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.chat.ExtendedGroupChat
import dev.inmo.tgbotapi.types.chat.GroupChat
import dev.inmo.tgbotapi.types.chat.SupergroupChat
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.abstracts.FromUserMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.utils.matrix
import dev.inmo.tgbotapi.utils.row
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.bot.handlers.Handler
import ru.andvl.chatkeep.bot.util.DurationParser
import ru.andvl.chatkeep.bot.util.MentionExtractor
import ru.andvl.chatkeep.bot.util.UserIdSource
import ru.andvl.chatkeep.domain.model.moderation.PunishmentSource
import ru.andvl.chatkeep.domain.model.moderation.PunishmentType
import ru.andvl.chatkeep.domain.service.moderation.AdminCacheService
import ru.andvl.chatkeep.domain.service.moderation.PunishmentService
import ru.andvl.chatkeep.domain.service.moderation.UsernameCacheService
import ru.andvl.chatkeep.domain.service.moderation.WarningService
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.minutes

@Component
class ModerationCommandHandler(
    private val adminCacheService: AdminCacheService,
    private val warningService: WarningService,
    private val punishmentService: PunishmentService,
    private val usernameCacheService: UsernameCacheService
) : Handler {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Context for moderation commands containing all extracted data.
     *
     * @property chatId The chat where the command was issued
     * @property chatTitle The title of the chat for logging
     * @property adminId The user ID of the admin issuing the command
     * @property targetUserId The user ID of the target being moderated
     * @property args Remaining command arguments (excluding command and user identifier)
     */
    private data class ModerationContext(
        val chatId: Long,
        val chatTitle: String?,
        val adminId: Long,
        val targetUserId: Long,
        val args: List<String>
    )

    /**
     * Extract message text from reply if present.
     */
    private fun extractReplyMessageText(message: CommonMessage<*>): String? {
        val replyMessage = message.replyTo as? CommonMessage<*> ?: return null
        val textContent = replyMessage.content as? TextContent
        return textContent?.text
    }

    private suspend fun BehaviourContext.withModerationAuth(
        message: CommonMessage<*>,
        usage: String,
        block: suspend (ModerationContext) -> Unit
    ) {
        val chatId = message.chat.id.chatId.long
        val chatTitle = (message.chat as? GroupChat)?.title
            ?: (message.chat as? SupergroupChat)?.title
        val adminId = (message as? FromUserMessage)?.from?.id?.chatId?.long ?: return

        // Check admin status
        if (!withContext(Dispatchers.IO) { adminCacheService.isAdmin(adminId, chatId) }) {
            reply(message, "You must be an admin to use this command.")
            return
        }

        // Extract target user and arguments
        val extraction = MentionExtractor.extractUserAndArgs(message)
        var targetUserId = extraction.userId
        val arguments = extraction.arguments

        // If no user ID found but we have a username, try to resolve it from cache
        if (targetUserId == null && extraction.username != null) {
            val resolved = withContext(Dispatchers.IO) {
                usernameCacheService.resolveUsername(extraction.username)
            }
            if (resolved != null) {
                targetUserId = resolved
            } else {
                reply(
                    message,
                    "Could not find user @${extraction.username}. " +
                        "The user must have sent a message in this chat first. " +
                        "Alternatively, reply to their message or use their numeric ID."
                )
                return
            }
        }

        if (targetUserId == null) {
            reply(message, usage)
            return
        }

        // Prevent self-moderation
        if (targetUserId == adminId) {
            reply(message, "You cannot moderate yourself.")
            return
        }

        // Prevent admin-on-admin
        val isTargetAdmin = withContext(Dispatchers.IO) {
            adminCacheService.isAdmin(targetUserId, chatId)
        }
        if (isTargetAdmin) {
            reply(message, "You cannot moderate other admins.")
            return
        }

        block(ModerationContext(chatId, chatTitle, adminId, targetUserId, arguments))
    }

    override suspend fun BehaviourContext.register() {
        val groupFilter = { msg: CommonMessage<*> ->
            msg.chat is GroupChat || msg.chat is SupergroupChat
        }

        // requireOnlyCommandInMessage = false allows commands with arguments like "/warn @username reason"
        // Without this, onCommand only triggers when message contains ONLY the command (e.g., "/warn")

        onCommand("warn", requireOnlyCommandInMessage = false, initialFilter = groupFilter) { message ->
            handleWarn(message)
        }

        onCommand("unwarn", requireOnlyCommandInMessage = false, initialFilter = groupFilter) { message ->
            handleUnwarn(message)
        }

        onCommand("mute", requireOnlyCommandInMessage = false, initialFilter = groupFilter) { message ->
            handleMute(message)
        }

        onCommand("unmute", requireOnlyCommandInMessage = false, initialFilter = groupFilter) { message ->
            handleUnmute(message)
        }

        onCommand("ban", requireOnlyCommandInMessage = false, initialFilter = groupFilter) { message ->
            handleBan(message)
        }

        onCommand("unban", requireOnlyCommandInMessage = false, initialFilter = groupFilter) { message ->
            handleUnban(message)
        }

        onCommand("kick", requireOnlyCommandInMessage = false, initialFilter = groupFilter) { message ->
            handleKick(message)
        }
    }

    private suspend fun BehaviourContext.handleWarn(message: CommonMessage<*>) {
        withModerationAuth(message, "Usage: /warn <reply to user or user_id> [reason]") { ctx ->
            // args[0:] is the reason (user ID already extracted)
            val reason = ctx.args.takeIf { it.isNotEmpty() }?.joinToString(" ")

            // Use atomic issueWarningWithThreshold to prevent race conditions
            // when multiple /warn commands are issued concurrently
            val thresholdResult = withContext(Dispatchers.IO) {
                warningService.issueWarningWithThreshold(
                    ctx.chatId,
                    ctx.targetUserId,
                    ctx.adminId,
                    reason,
                    ctx.chatTitle
                )
            }
            val result = thresholdResult.warningResult

            // Format expiry time
            val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                .withZone(ZoneId.of("Europe/Moscow"))
            val expiresAtFormatted = formatter.format(result.expiresAt)

            // Build notification message
            val notificationMessage = buildString {
                appendLine("⚠️ Предупреждение")
                appendLine()
                if (reason != null) {
                    appendLine("Причина: $reason")
                }
                appendLine("Варнов: ${result.activeCount}/${result.maxWarnings}")
                appendLine("Истекает: $expiresAtFormatted")
                appendLine("При ${result.maxWarnings} варнах: ${result.thresholdAction.name.lowercase()}")
            }

            // Create inline keyboard with delete button
            val keyboard = InlineKeyboardMarkup(
                keyboard = matrix {
                    row {
                        CallbackDataInlineKeyboardButton(
                            "Удалить варн (только админ)",
                            "warn_del:${ctx.chatId}:${ctx.targetUserId}"
                        )
                    }
                }
            )

            // Send notification to chat with inline keyboard
            val sentMessage = send(
                ChatId(RawChatId(ctx.chatId)),
                notificationMessage,
                replyMarkup = keyboard
            )

            // Auto-delete after delay (launch runs in BehaviourContext scope, doesn't block)
            launch {
                delay(60.seconds)
                try {
                    delete(sentMessage)
                    logger.debug("Auto-deleted warning notification in chat ${ctx.chatId}")
                } catch (e: Exception) {
                    logger.warn("Failed to auto-delete warning notification in chat ${ctx.chatId}: ${e.message}")
                }
            }

            // Execute threshold punishment if triggered (atomically determined above)
            if (thresholdResult.thresholdTriggered && thresholdResult.thresholdAction != null) {
                val duration = thresholdResult.thresholdDurationMinutes?.minutes

                withContext(Dispatchers.IO) {
                    punishmentService.executePunishment(
                        chatId = ctx.chatId,
                        userId = ctx.targetUserId,
                        issuedById = ctx.adminId,
                        type = thresholdResult.thresholdAction,
                        duration = duration,
                        reason = "Warning threshold reached",
                        source = PunishmentSource.THRESHOLD,
                        chatTitle = ctx.chatTitle
                    )
                }

                reply(message, "Warning threshold reached - applied ${thresholdResult.thresholdAction.name.lowercase()}.")
            }
        }
    }

    private suspend fun BehaviourContext.handleUnwarn(message: CommonMessage<*>) {
        withModerationAuth(message, "Usage: /unwarn <reply to user or user_id>") { ctx ->
            withContext(Dispatchers.IO) {
                warningService.removeWarnings(ctx.chatId, ctx.targetUserId, ctx.adminId, ctx.chatTitle)
            }

            reply(message, "User warnings removed.")
        }
    }

    private suspend fun BehaviourContext.handleMute(message: CommonMessage<*>) {
        withModerationAuth(message, "Usage: /mute <reply to user or user_id> [duration] [reason]") { ctx ->
            // args[0] = duration (optional), args[1:] = reason (optional)
            val duration = ctx.args.firstOrNull()?.let { DurationParser.parse(it) }
            val reason = if (duration != null && ctx.args.size > 1) {
                ctx.args.drop(1).joinToString(" ")
            } else if (duration == null && ctx.args.isNotEmpty()) {
                // First arg wasn't a valid duration, treat all as reason
                ctx.args.joinToString(" ")
            } else {
                null
            }

            // Extract message text from reply if present
            val messageText = extractReplyMessageText(message)

            val success = withContext(Dispatchers.IO) {
                punishmentService.executePunishment(
                    chatId = ctx.chatId,
                    userId = ctx.targetUserId,
                    issuedById = ctx.adminId,
                    type = PunishmentType.MUTE,
                    duration = duration,
                    reason = reason,
                    source = PunishmentSource.MANUAL,
                    messageText = messageText,
                    chatTitle = ctx.chatTitle
                )
            }

            if (success) {
                val durationText = duration?.let { " for ${it.inWholeHours}h" } ?: " indefinitely"
                reply(message, "User muted$durationText.")
            } else {
                reply(message, "Failed to mute user.")
            }
        }
    }

    private suspend fun BehaviourContext.handleUnmute(message: CommonMessage<*>) {
        withModerationAuth(message, "Usage: /unmute <reply to user or user_id>") { ctx ->
            val success = withContext(Dispatchers.IO) {
                punishmentService.unmute(ctx.chatId, ctx.targetUserId, ctx.adminId)
            }

            if (success) {
                reply(message, "User unmuted.")
            } else {
                reply(message, "Failed to unmute user.")
            }
        }
    }

    private suspend fun BehaviourContext.handleBan(message: CommonMessage<*>) {
        withModerationAuth(message, "Usage: /ban <reply to user or user_id> [duration] [reason]") { ctx ->
            // args[0] = duration (optional), args[1:] = reason (optional)
            val duration = ctx.args.firstOrNull()?.let { DurationParser.parse(it) }
            val reason = if (duration != null && ctx.args.size > 1) {
                ctx.args.drop(1).joinToString(" ")
            } else if (duration == null && ctx.args.isNotEmpty()) {
                ctx.args.joinToString(" ")
            } else {
                null
            }

            // Extract message text from reply if present
            val messageText = extractReplyMessageText(message)

            val success = withContext(Dispatchers.IO) {
                punishmentService.executePunishment(
                    chatId = ctx.chatId,
                    userId = ctx.targetUserId,
                    issuedById = ctx.adminId,
                    type = PunishmentType.BAN,
                    duration = duration,
                    reason = reason,
                    source = PunishmentSource.MANUAL,
                    messageText = messageText,
                    chatTitle = ctx.chatTitle
                )
            }

            if (success) {
                val durationText = duration?.let { " for ${it.inWholeHours}h" } ?: " permanently"
                reply(message, "User banned$durationText.")
            } else {
                reply(message, "Failed to ban user.")
            }
        }
    }

    private suspend fun BehaviourContext.handleUnban(message: CommonMessage<*>) {
        withModerationAuth(message, "Usage: /unban <reply to user or user_id>") { ctx ->
            val success = withContext(Dispatchers.IO) {
                punishmentService.unban(ctx.chatId, ctx.targetUserId, ctx.adminId)
            }

            if (success) {
                reply(message, "User unbanned.")
            } else {
                reply(message, "Failed to unban user.")
            }
        }
    }

    private suspend fun BehaviourContext.handleKick(message: CommonMessage<*>) {
        withModerationAuth(message, "Usage: /kick <reply to user or user_id> [reason]") { ctx ->
            // args[0:] = reason (user ID already extracted)
            val reason = ctx.args.takeIf { it.isNotEmpty() }?.joinToString(" ")

            // Extract message text from reply if present
            val messageText = extractReplyMessageText(message)

            val success = withContext(Dispatchers.IO) {
                punishmentService.executePunishment(
                    chatId = ctx.chatId,
                    userId = ctx.targetUserId,
                    issuedById = ctx.adminId,
                    type = PunishmentType.KICK,
                    duration = null,
                    reason = reason,
                    source = PunishmentSource.MANUAL,
                    messageText = messageText,
                    chatTitle = ctx.chatTitle
                )
            }

            if (success) {
                reply(message, "User kicked.")
            } else {
                reply(message, "Failed to kick user.")
            }
        }
    }
}
