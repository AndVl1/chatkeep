package ru.andvl.chatkeep.bot.handlers.locks

import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onContentMessage
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.CallbackDataInlineKeyboardButton
import dev.inmo.tgbotapi.types.chat.Bot
import dev.inmo.tgbotapi.types.chat.GroupChat
import dev.inmo.tgbotapi.types.chat.SupergroupChat
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.abstracts.FromChannelGroupContentMessage
import dev.inmo.tgbotapi.types.message.abstracts.FromUserMessage
import dev.inmo.tgbotapi.utils.RiskFeature
import dev.inmo.tgbotapi.utils.matrix
import dev.inmo.tgbotapi.utils.row
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.bot.handlers.Handler
import ru.andvl.chatkeep.domain.model.locks.AllowlistType
import ru.andvl.chatkeep.domain.model.locks.ExemptionType
import ru.andvl.chatkeep.domain.model.locks.LockType
import ru.andvl.chatkeep.domain.model.moderation.PunishmentSource
import ru.andvl.chatkeep.domain.model.moderation.PunishmentType
import ru.andvl.chatkeep.domain.service.locks.DetectionContext
import ru.andvl.chatkeep.domain.service.locks.LockDetectorRegistry
import ru.andvl.chatkeep.domain.service.locks.LockSettingsService
import ru.andvl.chatkeep.domain.service.moderation.AdminCacheService
import ru.andvl.chatkeep.domain.service.moderation.PunishmentService
import ru.andvl.chatkeep.domain.service.moderation.WarningService
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Component
class LockEnforcementHandler(
    private val lockSettingsService: LockSettingsService,
    private val lockDetectorRegistry: LockDetectorRegistry,
    private val adminCacheService: AdminCacheService,
    private val warningService: WarningService,
    private val punishmentService: PunishmentService
) : Handler {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private val AUTO_DELETE_DELAY = 60.seconds
    }

    @OptIn(RiskFeature::class)
    override suspend fun BehaviourContext.register() {
        // Filter for group/supergroup messages only
        onContentMessage(
            initialFilter = { message ->
                message.chat is GroupChat || message.chat is SupergroupChat
            },
            markerFactory = null  // Allow parallel processing
        ) { message ->
            handleMessage(message)
        }
    }

    private suspend fun BehaviourContext.handleMessage(message: ContentMessage<*>) {
        try {
            val chatId = message.chat.id.chatId.long
            val userId = (message as? FromUserMessage)?.from?.id?.chatId?.long

            // Check exemptions first
            if (isExempt(message, chatId, userId)) {
                return
            }

            // Get active locks for chat
            val locks = withContext(Dispatchers.IO) {
                lockSettingsService.getAllLocks(chatId)
            }

            if (locks.isEmpty()) return

            // Build detection context
            val context = buildDetectionContext(chatId)

            // Check each active lock
            for ((lockType, config) in locks) {
                if (!config.locked) continue

                val detector = lockDetectorRegistry.getDetector(lockType) ?: continue

                if (detector.detect(message, context)) {
                    // Lock violated - handle violation
                    handleViolation(message, chatId, userId, lockType, config.reason)
                    return  // Stop after first violation
                }
            }
        } catch (e: Exception) {
            logger.error("Lock enforcement error: ${e.message}", e)
        }
    }

    private suspend fun isExempt(message: ContentMessage<*>, chatId: Long, userId: Long?): Boolean {
        // Fetch exemptions once for reuse
        val exemptions = withContext(Dispatchers.IO) {
            lockSettingsService.getExemptions(chatId)
        }

        // Admin exemption
        if (userId != null) {
            val isAdmin = withContext(Dispatchers.IO) {
                adminCacheService.isAdmin(userId, chatId)
            }
            if (isAdmin) return true
        }

        // Bot exemption
        val from = (message as? FromUserMessage)?.from
        if (from is Bot) {
            // Check if bot is exempt
            val botUsername = from.username?.withoutAt
            if (botUsername != null) {
                val isExempt = exemptions.any {
                    it.exemptionType == ExemptionType.BOT.name &&
                    it.exemptionValue == botUsername
                }
                if (isExempt) return true
            }
        }

        // Channel post exemption (linked channel) - only if ANONCHANNEL lock is not enabled
        if (message is FromChannelGroupContentMessage<*>) {
            val anonChannelLock = withContext(Dispatchers.IO) {
                lockSettingsService.getLock(chatId, LockType.ANONCHANNEL)
            }
            // Exempt only if ANONCHANNEL lock is not active
            if (anonChannelLock == null || !anonChannelLock.locked) {
                return true
            }
        }

        // User exemption
        if (userId != null) {
            val isExempt = exemptions.any {
                it.exemptionType == ExemptionType.USER.name &&
                it.exemptionValue == userId.toString()
            }
            if (isExempt) return true
        }

        return false
    }

    private suspend fun buildDetectionContext(chatId: Long): DetectionContext {
        val allowlist = withContext(Dispatchers.IO) {
            lockSettingsService.getAllowlist(chatId, AllowlistType.URL) +
            lockSettingsService.getAllowlist(chatId, AllowlistType.DOMAIN)
        }

        // Parse URLs and domains from allowlist
        return DetectionContext(
            chatId = chatId,
            allowlistedUrls = allowlist.filter { it.contains("://") }.toSet(),
            allowlistedDomains = allowlist.filter { !it.contains("://") }.toSet(),
            allowlistedCommands = withContext(Dispatchers.IO) {
                lockSettingsService.getAllowlist(chatId, AllowlistType.COMMAND).toSet()
            }
        )
    }

    private suspend fun BehaviourContext.handleViolation(
        message: ContentMessage<*>,
        chatId: Long,
        userId: Long?,
        lockType: LockType,
        reason: String?
    ) {
        // Delete the message
        try {
            delete(message)
            logger.info("Deleted message violating lock ${lockType.name} in chat $chatId")
        } catch (e: Exception) {
            logger.error("Failed to delete message: ${e.message}")
        }

        // Warn user if lock_warns enabled
        if (userId != null) {
            val warnsEnabled = withContext(Dispatchers.IO) {
                lockSettingsService.isLockWarnsEnabled(chatId)
            }

            if (warnsEnabled) {
                val warnReason = reason ?: "Lock: ${lockType.name.lowercase()}"

                // Issue warning with threshold check (atomic operation)
                val thresholdResult = withContext(Dispatchers.IO) {
                    warningService.issueWarningWithThreshold(
                        chatId = chatId,
                        userId = userId,
                        issuedById = 0, // System-issued warning
                        reason = warnReason
                    )
                }
                val warningResult = thresholdResult.warningResult

                logger.info(
                    "Issued lock warning to user $userId in chat $chatId: " +
                    "${warningResult.activeCount}/${warningResult.maxWarnings} warnings"
                )

                // Apply threshold punishment if triggered
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

                // Send notification with warning count and admin undo button
                sendLockNotification(
                    chatId = chatId,
                    userId = userId,
                    lockType = lockType,
                    reason = warnReason,
                    warningCount = warningResult.activeCount,
                    maxWarnings = warningResult.maxWarnings,
                    thresholdAction = warningResult.thresholdAction
                )
            }
        }
    }

    private suspend fun BehaviourContext.sendLockNotification(
        chatId: Long,
        userId: Long,
        lockType: LockType,
        reason: String,
        warningCount: Int,
        maxWarnings: Int,
        thresholdAction: PunishmentType?
    ) {
        val notificationMessage = buildString {
            appendLine("🔒 Lock: ${lockType.name.lowercase()}")
            appendLine()
            appendLine("Причина: $reason")
            appendLine("Варнов: $warningCount/$maxWarnings")
            if (thresholdAction != null) {
                appendLine("При $maxWarnings варнах: ${thresholdAction.name.lowercase()}")
            }
        }

        // Create inline keyboard with undo button
        val keyboard = InlineKeyboardMarkup(
            keyboard = matrix {
                row {
                    +CallbackDataInlineKeyboardButton(
                        "Удалить варн (только админ)",
                        "lock_undo:$chatId:$userId:WARN"
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

            // Auto-delete after delay
            launch {
                delay(AUTO_DELETE_DELAY)
                try {
                    delete(sentMessage)
                    logger.debug("Auto-deleted lock notification in chat $chatId")
                } catch (e: Exception) {
                    logger.warn("Failed to auto-delete lock notification in chat $chatId: ${e.message}")
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to send lock notification in chat $chatId: ${e.message}")
        }
    }
}
