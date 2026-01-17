package ru.andvl.chatkeep.api.controller

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpServletRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
    private var cachedBotId: Long? = null

    @PostConstruct
    fun init() {
        runBlocking(Dispatchers.IO) {
            try {
                cachedBotId = bot.getMe().id.chatId.long
                logger.info("Cached bot ID: $cachedBotId")
            } catch (e: Exception) {
                logger.error("Failed to cache bot ID on startup", e)
            }
        }
    }

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
        val botId = cachedBotId

        // Check user admin status in parallel
        return runBlocking(Dispatchers.IO) {
            val adminCheckResults = allChats.map { chat ->
                async {
                    val isUserAdmin = try {
                        adminCacheService.isAdmin(user.id, chat.chatId, forceRefresh = false)
                    } catch (e: Exception) {
                        logger.warn("Failed to check user admin status for chat ${chat.chatId}", e)
                        false
                    }
                    chat to isUserAdmin
                }
            }.awaitAll()

            val userAdminChats = adminCheckResults.filter { it.second }.map { it.first }

            // Check bot admin status in parallel for user's admin chats only
            userAdminChats.map { chat ->
                async {
                    val isBotAdmin = if (botId != null) {
                        try {
                            adminCacheService.isAdmin(botId, chat.chatId, forceRefresh = false)
                        } catch (e: Exception) {
                            logger.warn("Failed to check bot admin status for chat ${chat.chatId}", e)
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
            }.awaitAll()
        }
    }
}
