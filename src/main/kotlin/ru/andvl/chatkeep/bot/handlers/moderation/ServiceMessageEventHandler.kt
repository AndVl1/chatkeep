package ru.andvl.chatkeep.bot.handlers.moderation

import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onLeftChatMember
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onNewChatMembers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.bot.handlers.Handler
import ru.andvl.chatkeep.infrastructure.repository.moderation.ModerationConfigRepository

@Component
class ServiceMessageEventHandler(
    private val moderationConfigRepository: ModerationConfigRepository
) : Handler {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun BehaviourContext.register() {
        onNewChatMembers { message ->
            val chatId = message.chat.id.chatId.long

            val config = withContext(Dispatchers.IO) {
                moderationConfigRepository.findByChatId(chatId)
            }

            if (config?.cleanServiceEnabled == true) {
                try {
                    delete(message)
                    logger.debug("Deleted join service message in chat $chatId")
                } catch (e: Exception) {
                    logger.warn("Failed to delete join service message in chat $chatId: ${e.message}")
                }
            }
        }

        onLeftChatMember { message ->
            val chatId = message.chat.id.chatId.long

            val config = withContext(Dispatchers.IO) {
                moderationConfigRepository.findByChatId(chatId)
            }

            if (config?.cleanServiceEnabled == true) {
                try {
                    delete(message)
                    logger.debug("Deleted leave service message in chat $chatId")
                } catch (e: Exception) {
                    logger.warn("Failed to delete leave service message in chat $chatId: ${e.message}")
                }
            }
        }
    }
}
