package ru.andvl.chatkeep.domain.service.moderation

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.andvl.chatkeep.domain.model.moderation.ActionType
import ru.andvl.chatkeep.domain.model.moderation.PunishmentSource
import ru.andvl.chatkeep.domain.model.moderation.PunishmentType
import ru.andvl.chatkeep.domain.model.moderation.Warning
import ru.andvl.chatkeep.infrastructure.repository.moderation.ModerationConfigRepository
import ru.andvl.chatkeep.infrastructure.repository.moderation.WarningRepository
import java.time.Instant
import kotlin.time.Duration.Companion.hours

@Service
class WarningService(
    private val warningRepository: WarningRepository,
    private val moderationConfigRepository: ModerationConfigRepository,
    private val punishmentService: PunishmentService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun issueWarning(
        chatId: Long,
        userId: Long,
        issuedById: Long,
        reason: String?,
        chatTitle: String? = null
    ): Warning {
        val config = moderationConfigRepository.findByChatId(chatId)
        val ttlHours = config?.warningTtlHours ?: 24

        val expiresAt = Instant.now().plusSeconds(ttlHours.hours.inWholeSeconds)
        val warning = Warning(
            chatId = chatId,
            userId = userId,
            issuedById = issuedById,
            reason = reason,
            expiresAt = expiresAt
        )

        val saved = warningRepository.save(warning)
        logger.info("Issued warning: chatId=$chatId, userId=$userId, reason=$reason")

        // Log the warning action (also sends to log channel)
        punishmentService.logAction(
            chatId = chatId,
            userId = userId,
            issuedById = issuedById,
            actionType = ActionType.WARN,
            reason = reason,
            source = PunishmentSource.MANUAL,
            chatTitle = chatTitle
        )

        return saved
    }

    @Transactional
    fun removeWarnings(chatId: Long, userId: Long, issuedById: Long, chatTitle: String? = null) {
        val now = Instant.now()
        warningRepository.deleteActiveByChatIdAndUserId(chatId, userId, now)

        // Delegate logging to PunishmentService (CODE-002 fix)
        punishmentService.logAction(
            chatId = chatId,
            userId = userId,
            issuedById = issuedById,
            actionType = ActionType.UNWARN,
            source = PunishmentSource.MANUAL,
            chatTitle = chatTitle
        )

        logger.info("Removed warnings: chatId=$chatId, userId=$userId")
    }

    fun getActiveWarningCount(chatId: Long, userId: Long): Int {
        val now = Instant.now()
        return warningRepository.countActiveByChatIdAndUserId(chatId, userId, now)
    }

    fun checkThreshold(chatId: Long, userId: Long): PunishmentType? {
        val config = moderationConfigRepository.findByChatId(chatId)
        val maxWarnings = config?.maxWarnings ?: 3
        val activeWarnings = getActiveWarningCount(chatId, userId)

        if (activeWarnings >= maxWarnings) {
            val thresholdAction = config?.thresholdAction ?: "MUTE"
            logger.info("Warning threshold reached: chatId=$chatId, userId=$userId, count=$activeWarnings")
            return PunishmentType.valueOf(thresholdAction)
        }

        return null
    }

    fun getThresholdDurationHours(chatId: Long): Int? {
        val config = moderationConfigRepository.findByChatId(chatId)
        return config?.thresholdDurationHours
    }
}
