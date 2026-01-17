package ru.andvl.chatkeep.api.controller

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.andvl.chatkeep.api.auth.TelegramAuthFilter
import ru.andvl.chatkeep.api.auth.TelegramAuthService
import ru.andvl.chatkeep.api.dto.ChatSummaryResponse
import ru.andvl.chatkeep.api.exception.UnauthorizedException
import ru.andvl.chatkeep.domain.service.ChatService
import ru.andvl.chatkeep.domain.service.moderation.AdminCacheService

@RestController
@RequestMapping("/api/v1/miniapp/chats")
@Tag(name = "Mini App - Chats", description = "Chat management for Telegram Mini App")
@SecurityRequirement(name = "TelegramAuth")
class MiniAppChatsController(
    private val chatService: ChatService,
    private val adminCacheService: AdminCacheService,
    private val bot: TelegramBot
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private fun getUserFromRequest(request: HttpServletRequest): TelegramAuthService.TelegramUser {
        return request.getAttribute(TelegramAuthFilter.USER_ATTR) as? TelegramAuthService.TelegramUser
            ?: throw UnauthorizedException("User not authenticated")
    }

    @GetMapping
    @Operation(summary = "Get user's admin chats", description = "Returns list of chats where user is admin and bot is present")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Success"),
        ApiResponse(responseCode = "401", description = "Unauthorized")
    )
    fun getChats(request: HttpServletRequest): List<ChatSummaryResponse> {
        val user = getUserFromRequest(request)

        val allChats = chatService.getAllChats()

        // Get bot ID for checking bot admin status
        val botId = runBlocking(Dispatchers.IO) {
            try {
                bot.getMe().id.chatId.long
            } catch (e: Exception) {
                logger.error("Failed to get bot ID: ${e.message}")
                null
            }
        }

        // Filter chats where user is admin and check bot admin status
        return runBlocking(Dispatchers.IO) {
            allChats.filter { chat ->
                try {
                    adminCacheService.isAdmin(user.id, chat.chatId, forceRefresh = false)
                } catch (e: Exception) {
                    logger.debug("Failed to check admin status for chat ${chat.chatId}: ${e.message}")
                    false
                }
            }.map { chat ->
                // Check if bot is admin in this chat
                val isBotAdmin = if (botId != null) {
                    try {
                        adminCacheService.isAdmin(botId, chat.chatId, forceRefresh = false)
                    } catch (e: Exception) {
                        logger.debug("Failed to check bot admin status for chat ${chat.chatId}: ${e.message}")
                        false
                    }
                } else {
                    false
                }

                ChatSummaryResponse(
                    chatId = chat.chatId,
                    chatTitle = chat.chatTitle,
                    memberCount = null,
                    isBotAdmin = isBotAdmin
                )
            }
        }
    }
}
