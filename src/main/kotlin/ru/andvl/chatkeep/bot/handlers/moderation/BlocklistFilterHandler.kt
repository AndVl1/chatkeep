package ru.andvl.chatkeep.bot.handlers.moderation

import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onText
import dev.inmo.tgbotapi.types.chat.GroupChat
import dev.inmo.tgbotapi.types.chat.SupergroupChat
import dev.inmo.tgbotapi.types.message.abstracts.FromUserMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.bot.handlers.Handler
import ru.andvl.chatkeep.domain.model.moderation.PunishmentSource
import ru.andvl.chatkeep.domain.service.moderation.AdminCacheService
import ru.andvl.chatkeep.domain.service.moderation.BlocklistService
import ru.andvl.chatkeep.domain.service.moderation.PunishmentService
import kotlin.time.Duration.Companion.hours

@Component
class BlocklistFilterHandler(
    private val blocklistService: BlocklistService,
    private val punishmentService: PunishmentService,
    private val adminCacheService: AdminCacheService
) : Handler {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun BehaviourContext.register() {
        onText(initialFilter = { msg ->
            msg.chat is GroupChat || msg.chat is SupergroupChat
        }) { message ->
            val textContent = message.content as? TextContent ?: return@onText
            val text = textContent.text
            val chatId = message.chat.id.chatId.long
            val userId = (message as? FromUserMessage)?.from?.id?.chatId?.long ?: return@onText

            // Admins are exempt from blocklist
            val isAdmin = withContext(Dispatchers.IO) {
                adminCacheService.isAdmin(userId, chatId)
            }
            if (isAdmin) {
                return@onText
            }

            // Check message against blocklist
            val match = withContext(Dispatchers.IO) {
                blocklistService.checkMessage(chatId, text)
            }

            if (match != null) {
                logger.info("Blocklist match detected: chatId=$chatId, userId=$userId, pattern='${match.pattern.pattern}'")

                // Delete the message
                try {
                    delete(message)
                } catch (e: Exception) {
                    logger.warn("Failed to delete message: ${e.message}")
                }

                // Apply punishment
                val duration = match.durationHours?.let { it.hours }
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
                } else {
                    logger.error("Failed to apply blocklist punishment: action=${match.action}, userId=$userId, chatId=$chatId")
                }
            }
        }
    }
}
