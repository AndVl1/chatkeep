package ru.andvl.chatkeep.bot.handlers

import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onContentMessage
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.types.chat.GroupChat
import dev.inmo.tgbotapi.types.chat.SupergroupChat
import dev.inmo.tgbotapi.types.message.abstracts.FromUserMessage
import dev.inmo.tgbotapi.utils.RiskFeature
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.domain.model.moderation.PunishmentSource
import ru.andvl.chatkeep.domain.model.moderation.PunishmentType
import ru.andvl.chatkeep.domain.service.AntifloodService
import ru.andvl.chatkeep.domain.service.ChatService
import ru.andvl.chatkeep.domain.service.moderation.AdminCacheService
import ru.andvl.chatkeep.domain.service.moderation.PunishmentService
import kotlin.time.Duration.Companion.minutes

@Component
class AntifloodHandler(
    private val antifloodService: AntifloodService,
    private val punishmentService: PunishmentService,
    private val adminCacheService: AdminCacheService,
    private val chatService: ChatService
) : Handler {

    private val logger = LoggerFactory.getLogger(javaClass)

    @OptIn(RiskFeature::class)
    override suspend fun BehaviourContext.register() {
        onContentMessage(
            initialFilter = { message ->
                val chat = message.chat
                chat is GroupChat || chat is SupergroupChat
            }
        ) { message ->
            val user = (message as? FromUserMessage)?.from ?: return@onContentMessage
            val userId = user.id.chatId.long
            val chatId = message.chat.id.chatId.long

            // Skip admins
            val isAdmin = withContext(Dispatchers.IO) {
                adminCacheService.isAdmin(userId, chatId, forceRefresh = false)
            }
            if (isAdmin) return@onContentMessage

            withContext(Dispatchers.IO) {
                try {
                    // Check if user is flooding
                    val isFlooding = antifloodService.checkFlood(chatId, userId)

                    if (isFlooding) {
                        val settings = antifloodService.getSettings(chatId) ?: return@withContext

                        // Delete the message
                        try {
                            deleteMessage(message)
                        } catch (e: Exception) {
                            logger.debug("Failed to delete flood message: ${e.message}")
                        }

                        // Execute punishment
                        val action = try {
                            PunishmentType.valueOf(settings.action)
                        } catch (e: IllegalArgumentException) {
                            PunishmentType.MUTE
                        }

                        val duration = settings.actionDurationMinutes?.minutes
                        val chatTitle = chatService.getSettings(chatId)?.chatTitle

                        punishmentService.executePunishment(
                            chatId = chatId,
                            userId = userId,
                            issuedById = 0, // System/bot
                            type = action,
                            duration = duration,
                            reason = "Anti-flood: exceeded ${settings.maxMessages} messages in ${settings.timeWindowSeconds}s",
                            source = PunishmentSource.THRESHOLD,
                            chatTitle = chatTitle
                        )

                        // Clear flood tracking for this user
                        antifloodService.clearFloodTracking(chatId, userId)

                        logger.info("Anti-flood triggered: chatId=$chatId, userId=$userId, action=$action")
                    }
                } catch (e: Exception) {
                    logger.error("Anti-flood handler error: ${e.message}", e)
                }
            }
        }
    }
}
