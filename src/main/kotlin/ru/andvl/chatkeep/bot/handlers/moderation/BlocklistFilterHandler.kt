package ru.andvl.chatkeep.bot.handlers.moderation

import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onDice
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onSticker
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onText
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.CallbackDataInlineKeyboardButton
import dev.inmo.tgbotapi.types.chat.GroupChat
import dev.inmo.tgbotapi.types.chat.SupergroupChat
import dev.inmo.tgbotapi.types.message.abstracts.FromUserMessage
import dev.inmo.tgbotapi.types.message.content.DiceContent
import dev.inmo.tgbotapi.types.message.content.StickerContent
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.types.files.CustomEmojiSticker
import dev.inmo.tgbotapi.utils.matrix
import dev.inmo.tgbotapi.utils.row
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.bot.handlers.Handler
import ru.andvl.chatkeep.domain.model.moderation.PunishmentSource
import ru.andvl.chatkeep.domain.model.moderation.PunishmentType
import ru.andvl.chatkeep.domain.service.moderation.AdminCacheService
import ru.andvl.chatkeep.domain.service.moderation.BlocklistService
import ru.andvl.chatkeep.domain.service.moderation.PunishmentService
import ru.andvl.chatkeep.domain.service.moderation.WarningService
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Component
class BlocklistFilterHandler(
    private val blocklistService: BlocklistService,
    private val punishmentService: PunishmentService,
    private val warningService: WarningService,
    private val adminCacheService: AdminCacheService
) : Handler {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private val AUTO_DELETE_DELAY = 60.seconds
    }

    /**
     * Check if user is admin (exempt from blocklist).
     * Returns true if admin (should skip filtering), false otherwise.
     */
    private suspend fun isAdminExempt(userId: Long, chatId: Long): Boolean =
        withContext(Dispatchers.IO) { adminCacheService.isAdmin(userId, chatId) }

    /**
     * Check content against blocklist and handle match if found.
     */
    private suspend fun BehaviourContext.checkAndHandle(
        chatId: Long,
        userId: Long,
        content: String,
        message: dev.inmo.tgbotapi.types.message.abstracts.ContentMessage<*>
    ) {
        val match = withContext(Dispatchers.IO) {
            blocklistService.checkMessage(chatId, content)
        }
        if (match != null) {
            handleBlocklistMatch(chatId, userId, match, message)
        }
    }

    override suspend fun BehaviourContext.register() {
        onText(
            initialFilter = { msg ->
                msg.chat is GroupChat || msg.chat is SupergroupChat
            },
            markerFactory = null // Enable parallel processing of all messages
        ) { message ->
            val textContent = message.content as? TextContent ?: return@onText
            val text = textContent.text
            val chatId = message.chat.id.chatId.long
            val userId = (message as? FromUserMessage)?.from?.id?.chatId?.long ?: return@onText

            if (isAdminExempt(userId, chatId)) return@onText
            checkAndHandle(chatId, userId, text, message)
        }

        // Handle dice messages (🎲🎯🏀⚽🎰🎳)
        // Dice emoji can be blocked via blocklist
        onDice(
            initialFilter = { msg ->
                msg.chat is GroupChat || msg.chat is SupergroupChat
            },
            markerFactory = null
        ) { message ->
            val diceContent = message.content as? DiceContent ?: return@onDice
            val emoji = diceContent.dice.animationType.emoji
            val chatId = message.chat.id.chatId.long
            val userId = (message as? FromUserMessage)?.from?.id?.chatId?.long ?: return@onDice

            if (isAdminExempt(userId, chatId)) return@onDice
            checkAndHandle(chatId, userId, emoji, message)
        }

        // Handle custom emoji stickers (animated emojis sent via Telegram Premium)
        // Only CustomEmojiSticker - NOT regular stickers
        // StickerType.CustomEmoji = premium animated emojis that appear in-line
        // StickerType.Regular = normal sticker packs - should NOT be blocked
        onSticker(
            initialFilter = { msg ->
                (msg.chat is GroupChat || msg.chat is SupergroupChat) &&
                    (msg.content as? StickerContent)?.media is CustomEmojiSticker
            },
            markerFactory = null
        ) { message ->
            val stickerContent = message.content as? StickerContent ?: return@onSticker
            val sticker = stickerContent.media as? CustomEmojiSticker ?: return@onSticker

            // Get emoji from custom emoji sticker
            val emoji = sticker.emoji ?: return@onSticker
            val chatId = message.chat.id.chatId.long
            val userId = (message as? FromUserMessage)?.from?.id?.chatId?.long ?: return@onSticker

            if (isAdminExempt(userId, chatId)) return@onSticker
            checkAndHandle(chatId, userId, emoji, message)
        }
    }

    private suspend fun BehaviourContext.handleBlocklistMatch(
        chatId: Long,
        userId: Long,
        match: BlocklistService.BlocklistMatch,
        message: dev.inmo.tgbotapi.types.message.abstracts.ContentMessage<*>
    ) {
        logger.info("Blocklist match detected in chat $chatId for user $userId")

        // Delete the message
        try {
            delete(message)
        } catch (e: Exception) {
            logger.warn("Failed to delete message: ${e.message}")
        }

        // Skip notification for NOTHING action
        if (match.action == PunishmentType.NOTHING) {
            logger.debug("Blocklist action is NOTHING, skipping notification")
            return
        }

        // Apply punishment
        val duration = match.durationMinutes?.minutes

        // For WARN, use atomic issueWarningWithThreshold to prevent race conditions
        if (match.action == PunishmentType.WARN) {
            val thresholdResult = withContext(Dispatchers.IO) {
                warningService.issueWarningWithThreshold(
                    chatId = chatId,
                    userId = userId,
                    issuedById = 0, // System (blocklist)
                    reason = "Blocklist match"
                )
            }
            val warningResult = thresholdResult.warningResult

            logger.info("Issued blocklist warning: chatId=$chatId, userId=$userId, count=${warningResult.activeCount}/${warningResult.maxWarnings}")

            // Apply threshold punishment if triggered (atomically determined above)
            if (thresholdResult.thresholdTriggered && thresholdResult.thresholdAction != null) {
                withContext(Dispatchers.IO) {
                    punishmentService.executePunishment(
                        chatId = chatId,
                        userId = userId,
                        issuedById = 0,
                        type = thresholdResult.thresholdAction,
                        duration = thresholdResult.thresholdDurationMinutes?.minutes,
                        reason = "Warning threshold reached",
                        source = PunishmentSource.THRESHOLD
                    )
                }
                logger.info("Warning threshold reached, applied punishment: chatId=$chatId, userId=$userId")
            }

            // Send notification with warning count
            sendBlocklistNotification(
                chatId = chatId,
                userId = userId,
                action = PunishmentType.WARN,
                durationMinutes = null,
                warningCount = warningResult.activeCount,
                maxWarnings = warningResult.maxWarnings,
                thresholdAction = warningResult.thresholdAction
            )
        } else {
            // For other actions (MUTE, BAN, KICK), use PunishmentService
            val success = withContext(Dispatchers.IO) {
                punishmentService.executePunishment(
                    chatId = chatId,
                    userId = userId,
                    issuedById = 0, // System
                    type = match.action,
                    duration = duration,
                    reason = "Blocklist match: ${match.pattern.pattern}",
                    source = PunishmentSource.BLOCKLIST
                )
            }

            if (success) {
                logger.info("Applied blocklist punishment: action=${match.action}, userId=$userId, chatId=$chatId")
                sendBlocklistNotification(
                    chatId = chatId,
                    userId = userId,
                    action = match.action,
                    durationMinutes = match.durationMinutes
                )
            } else {
                logger.error("Failed to apply blocklist punishment: action=${match.action}, userId=$userId, chatId=$chatId")
            }
        }
    }

    private suspend fun BehaviourContext.sendBlocklistNotification(
        chatId: Long,
        userId: Long,
        action: PunishmentType,
        durationMinutes: Int?,
        warningCount: Int? = null,
        maxWarnings: Int? = null,
        thresholdAction: PunishmentType? = null
    ) {
        // Build notification message
        val actionText = when (action) {
            PunishmentType.WARN -> "Предупреждение"
            PunishmentType.MUTE -> "Мут"
            PunishmentType.BAN -> "Бан"
            PunishmentType.KICK -> "Кик"
            PunishmentType.NOTHING -> return // Should not reach here
        }

        val durationText = durationMinutes?.let { " на ${ru.andvl.chatkeep.bot.util.DurationParser.formatMinutes(it)}" } ?: ""

        val notificationMessage = buildString {
            appendLine("🚫 Блоклист")
            appendLine()
            appendLine("Действие: $actionText$durationText")

            // Add warning count info if available (only for WARN action)
            if (action == PunishmentType.WARN && warningCount != null && maxWarnings != null && thresholdAction != null) {
                appendLine("Варнов: $warningCount/$maxWarnings")
                appendLine("При $maxWarnings варнах: ${thresholdAction.name.lowercase()}")
            }
        }

        // Button text based on action
        val buttonText = when (action) {
            PunishmentType.WARN -> "Удалить варн"
            PunishmentType.MUTE -> "Снять мут"
            PunishmentType.BAN -> "Снять бан"
            PunishmentType.KICK -> "Закрыть"
            PunishmentType.NOTHING -> return
        }

        // Create inline keyboard with undo button
        val keyboard = InlineKeyboardMarkup(
            keyboard = matrix {
                row {
                    +CallbackDataInlineKeyboardButton(
                        "$buttonText (только админ)",
                        "block_undo:$chatId:$userId:${action.name}"
                    )
                }
            }
        )

        try {
            val sentMessage = send(
                ChatId(RawChatId(chatId)),
                notificationMessage,
                replyMarkup = keyboard
            )

            // Auto-delete after delay (launch runs in BehaviourContext scope, doesn't block)
            launch {
                delay(AUTO_DELETE_DELAY)
                try {
                    delete(sentMessage)
                    logger.debug("Auto-deleted blocklist notification in chat $chatId")
                } catch (e: Exception) {
                    logger.warn("Failed to auto-delete blocklist notification in chat $chatId: ${e.message}")
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to send blocklist notification in chat $chatId: ${e.message}")
        }
    }
}
