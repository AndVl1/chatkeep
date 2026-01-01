package ru.andvl.chatkeep.domain.service.moderation

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.chat.get.getChat
import dev.inmo.tgbotapi.extensions.api.chat.members.banChatMember
import dev.inmo.tgbotapi.extensions.api.chat.members.restrictChatMember
import dev.inmo.tgbotapi.extensions.api.chat.members.unbanChatMember
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.UserId
import dev.inmo.tgbotapi.types.TelegramDate
import dev.inmo.tgbotapi.types.chat.ExtendedSupergroupChat
import dev.inmo.tgbotapi.types.chat.LeftRestrictionsChatPermissions
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import ru.andvl.chatkeep.domain.model.moderation.ActionType
import ru.andvl.chatkeep.domain.model.moderation.Punishment
import ru.andvl.chatkeep.domain.model.moderation.PunishmentSource
import ru.andvl.chatkeep.domain.model.moderation.PunishmentType
import ru.andvl.chatkeep.infrastructure.repository.moderation.PunishmentRepository
import java.time.Instant
import kotlin.time.Duration

@Service
class PunishmentService(
    private val punishmentRepository: PunishmentRepository,
    private val bot: TelegramBot
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun executePunishment(
        chatId: Long,
        userId: Long,
        issuedById: Long,
        type: PunishmentType,
        duration: Duration?,
        reason: String?,
        source: PunishmentSource,
        messageText: String? = null
    ): Boolean {
        val chatIdWrapper = ChatId(RawChatId(chatId))
        val userIdWrapper = UserId(RawChatId(userId))

        val success = try {
            when (type) {
                PunishmentType.NOTHING -> {
                    // No action on user, just delete message (handled by caller)
                    // Logging still happens below
                    true
                }
                PunishmentType.WARN -> {
                    // Warnings are handled by WarningService
                    true
                }
                PunishmentType.MUTE -> {
                    val untilDate = duration?.let {
                        TelegramDate(Instant.now().plusSeconds(it.inWholeSeconds).epochSecond)
                    }
                    bot.restrictChatMember(
                        chatIdWrapper,
                        userIdWrapper,
                        untilDate = untilDate
                    )
                    true
                }
                PunishmentType.BAN -> {
                    val untilDate = duration?.let {
                        TelegramDate(Instant.now().plusSeconds(it.inWholeSeconds).epochSecond)
                    }
                    bot.banChatMember(chatIdWrapper, userIdWrapper, untilDate = untilDate)
                    true
                }
                PunishmentType.KICK -> {
                    // Kick = ban then immediately unban
                    bot.banChatMember(chatIdWrapper, userIdWrapper)
                    bot.unbanChatMember(chatIdWrapper, userIdWrapper)
                    true
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to execute punishment: type=$type, chatId=$chatId, userId=$userId", e)
            false
        }

        if (success) {
            // Map PunishmentType to ActionType for logging
            val actionType = ActionType.valueOf(type.name)
            logAction(chatId, userId, issuedById, actionType, duration, reason, source, messageText)
        }

        return success
    }

    /**
     * Unified method for logging all admin actions (CODE-001 fix).
     * Uses ActionType enum for type safety (CODE-003 fix).
     */
    fun logAction(
        chatId: Long,
        userId: Long,
        issuedById: Long,
        actionType: ActionType,
        duration: Duration? = null,
        reason: String? = null,
        source: PunishmentSource,
        messageText: String? = null
    ) {
        val punishment = Punishment(
            chatId = chatId,
            userId = userId,
            issuedById = issuedById,
            punishmentType = actionType.name,
            durationSeconds = duration?.inWholeSeconds,
            reason = reason,
            source = source.name,
            messageText = messageText
        )

        punishmentRepository.save(punishment)
        logger.info("Logged action: type=$actionType, chatId=$chatId, userId=$userId, source=$source")
    }

    suspend fun unmute(chatId: Long, userId: Long, issuedById: Long): Boolean {
        val chatIdWrapper = ChatId(RawChatId(chatId))
        val userIdWrapper = UserId(RawChatId(userId))

        return try {
            // Get chat's default permissions to restore user to chat-level defaults
            val chat = bot.getChat(chatIdWrapper)
            val chatPermissions = (chat as? ExtendedSupergroupChat)?.permissions
                ?: LeftRestrictionsChatPermissions

            // Restore user permissions to chat defaults
            bot.restrictChatMember(
                chatIdWrapper,
                userIdWrapper,
                permissions = chatPermissions
            )

            // Log the unmute action using consolidated method
            logAction(chatId, userId, issuedById, ActionType.UNMUTE, source = PunishmentSource.MANUAL)

            logger.info("Unmuted user: chatId=$chatId, userId=$userId")
            true
        } catch (e: Exception) {
            logger.error("Failed to unmute user: chatId=$chatId, userId=$userId", e)
            false
        }
    }

    suspend fun unban(chatId: Long, userId: Long, issuedById: Long): Boolean {
        val chatIdWrapper = ChatId(RawChatId(chatId))
        val userIdWrapper = UserId(RawChatId(userId))

        return try {
            bot.unbanChatMember(chatIdWrapper, userIdWrapper)

            // Log the unban action using consolidated method
            logAction(chatId, userId, issuedById, ActionType.UNBAN, source = PunishmentSource.MANUAL)

            logger.info("Unbanned user: chatId=$chatId, userId=$userId")
            true
        } catch (e: Exception) {
            logger.error("Failed to unban user: chatId=$chatId, userId=$userId", e)
            false
        }
    }
}
