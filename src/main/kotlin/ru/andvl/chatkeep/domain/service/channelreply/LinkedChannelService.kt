package ru.andvl.chatkeep.domain.service.channelreply

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.chat.get.getChat
import dev.inmo.tgbotapi.types.toChatId
import dev.inmo.tgbotapi.types.chat.ExtendedSupergroupChat
import dev.inmo.tgbotapi.types.ChatId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class LinkedChannelService(
    private val bot: TelegramBot
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun getLinkedChannel(chatId: Long): LinkedChannelInfo? {
        return try {
            val chat = bot.getChat(chatId.toChatId())
            if (chat is ExtendedSupergroupChat) {
                chat.linkedChannelChatId?.let { linkedChatId ->
                    LinkedChannelInfo(
                        id = linkedChatId.chatId.long,
                        title = chat.title
                    )
                }
            } else {
                null
            }
        } catch (e: Exception) {
            logger.error("Failed to get linked channel for chat $chatId: ${e.message}", e)
            null
        }
    }
}

data class LinkedChannelInfo(
    val id: Long,
    val title: String
)
