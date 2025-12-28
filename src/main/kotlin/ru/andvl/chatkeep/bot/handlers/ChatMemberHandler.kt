package ru.andvl.chatkeep.bot.handlers

import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onChatMemberUpdated
import dev.inmo.tgbotapi.types.chat.GroupChat
import dev.inmo.tgbotapi.types.chat.SupergroupChat
import dev.inmo.tgbotapi.types.chat.member.AdministratorChatMember
import dev.inmo.tgbotapi.types.chat.member.MemberChatMember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.domain.service.ChatService

@Component
class ChatMemberHandler(
    private val chatService: ChatService
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
                        val chatTitle = when (chat) {
                            is GroupChat -> chat.title
                            is SupergroupChat -> chat.title
                            else -> null
                        }

                        withContext(Dispatchers.IO) {
                            try {
                                chatService.registerChat(
                                    chatId = chat.id.chatId.long,
                                    chatTitle = chatTitle
                                )
                                logger.info("Bot added to chat: ${chat.id.chatId.long} ($chatTitle)")
                            } catch (e: Exception) {
                                logger.error("Failed to register chat: ${e.message}", e)
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
