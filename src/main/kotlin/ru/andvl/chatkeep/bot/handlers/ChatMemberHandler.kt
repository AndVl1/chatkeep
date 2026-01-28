package ru.andvl.chatkeep.bot.handlers

import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onChatMemberUpdated
import dev.inmo.tgbotapi.types.chat.ChannelChat
import dev.inmo.tgbotapi.types.chat.GroupChat
import dev.inmo.tgbotapi.types.chat.SupergroupChat
import dev.inmo.tgbotapi.types.chat.member.AdministratorChatMember
import dev.inmo.tgbotapi.types.chat.member.MemberChatMember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.bot.service.AdminErrorNotificationService
import ru.andvl.chatkeep.bot.service.ErrorContext
import ru.andvl.chatkeep.domain.model.ChatType
import ru.andvl.chatkeep.domain.service.ChatService

@Component
class ChatMemberHandler(
    private val chatService: ChatService,
    private val errorNotificationService: AdminErrorNotificationService
) : Handler {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun BehaviourContext.register() {
        val botId = bot.getMe().id

        onChatMemberUpdated { update ->
            val newMember = update.newChatMemberState
            val chat = update.chat

            if (newMember.user.id == botId) {
                when (newMember) {
                    is MemberChatMember, is AdministratorChatMember -> {
                        val (chatTitle, chatType) = when (chat) {
                            is GroupChat -> chat.title to ChatType.GROUP
                            is SupergroupChat -> chat.title to ChatType.SUPERGROUP
                            is ChannelChat -> chat.title to ChatType.CHANNEL
                            else -> null to ChatType.GROUP
                        }

                        withContext(Dispatchers.IO) {
                            try {
                                chatService.registerChat(
                                    chatId = chat.id.chatId.long,
                                    chatTitle = chatTitle,
                                    chatType = chatType
                                )
                                logger.info("Bot added to $chatType: ${chat.id.chatId.long} ($chatTitle)")
                            } catch (e: Exception) {
                                logger.error("Failed to register chat: ${e.message}", e)
                                errorNotificationService.reportHandlerError(
                                    handler = "ChatMemberHandler",
                                    error = e,
                                    context = ErrorContext(chatId = chat.id.chatId.long)
                                )
                            }
                        }
                    }
                    else -> {
                        logger.info("Bot removed from chat: ${chat.id.chatId.long}")
                    }
                }
            }
        }
    }
}
