package ru.andvl.chatkeep.bot.handlers

import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onContentMessage
import dev.inmo.tgbotapi.types.chat.GroupChat
import dev.inmo.tgbotapi.types.chat.SupergroupChat
import dev.inmo.tgbotapi.types.message.abstracts.FromUserMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.utils.RiskFeature
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.domain.service.MessageService
import ru.andvl.chatkeep.domain.service.moderation.UsernameCacheService
import java.time.Instant

@Component
class GroupMessageHandler(
    private val messageService: MessageService,
    private val usernameCacheService: UsernameCacheService
) : Handler {

    private val logger = LoggerFactory.getLogger(javaClass)

    @OptIn(RiskFeature::class)
    override suspend fun BehaviourContext.register() {
        onContentMessage(
            initialFilter = { message ->
                val chat = message.chat
                (chat is GroupChat || chat is SupergroupChat) && message.content is TextContent
            },
            markerFactory = null // Enable parallel processing of all messages
        ) { message ->
            val textContent = message.content as TextContent
            val user = (message as? FromUserMessage)?.from ?: return@onContentMessage

            withContext(Dispatchers.IO) {
                try {
                    val userId = user.id.chatId.long
                    val username = user.username?.withoutAt

                    messageService.saveMessage(
                        telegramMessageId = message.messageId.long,
                        chatId = message.chat.id.chatId.long,
                        userId = userId,
                        username = username,
                        firstName = user.firstName,
                        lastName = user.lastName,
                        text = textContent.text,
                        messageDate = Instant.ofEpochSecond(message.date.unixMillisLong / 1000)
                    )

                    // Cache username -> userId mapping for @username resolution
                    if (username != null) {
                        usernameCacheService.cacheUsername(
                            username = username,
                            userId = userId,
                            firstName = user.firstName,
                            lastName = user.lastName
                        )
                    }
                } catch (e: Exception) {
                    logger.error("Failed to save message: ${e.message}", e)
                }
            }
        }
    }
}
