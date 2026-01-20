package ru.andvl.chatkeep.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.springframework.web.bind.annotation.*
import ru.andvl.chatkeep.api.auth.TelegramAuthFilter
import ru.andvl.chatkeep.api.auth.TelegramAuthService
import ru.andvl.chatkeep.api.dto.ChatStatisticsResponse
import ru.andvl.chatkeep.api.exception.AccessDeniedException
import ru.andvl.chatkeep.api.exception.ResourceNotFoundException
import ru.andvl.chatkeep.api.exception.UnauthorizedException
import ru.andvl.chatkeep.domain.service.AdminService
import ru.andvl.chatkeep.domain.service.moderation.AdminCacheService

@RestController
@RequestMapping("/api/v1/miniapp/chats/{chatId}")
@Tag(name = "Mini App - Statistics", description = "Chat statistics")
@SecurityRequirement(name = "TelegramAuth")
class MiniAppStatisticsController(
    private val adminService: AdminService,
    private val adminCacheService: AdminCacheService
) {

    private fun getUserFromRequest(request: HttpServletRequest): TelegramAuthService.TelegramUser {
        return request.getAttribute(TelegramAuthFilter.USER_ATTR) as? TelegramAuthService.TelegramUser
            ?: throw UnauthorizedException("User not authenticated")
    }

    @GetMapping("/stats")
    @Operation(summary = "Get chat statistics")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Success"),
        ApiResponse(responseCode = "403", description = "Forbidden - not admin"),
        ApiResponse(responseCode = "404", description = "Chat not found")
    )
    fun getStatistics(
        @PathVariable chatId: Long,
        request: HttpServletRequest
    ): ChatStatisticsResponse {
        val user = getUserFromRequest(request)

        // Check admin permission
        val isAdmin = runBlocking(Dispatchers.IO) {
            adminCacheService.isAdmin(user.id, chatId, forceRefresh = false)
        }
        if (!isAdmin) {
            throw AccessDeniedException("You are not an admin in this chat")
        }

        val stats = adminService.getStatistics(chatId)
            ?: throw ResourceNotFoundException("Chat", chatId)

        return ChatStatisticsResponse(
            chatId = stats.chatId,
            chatTitle = stats.chatTitle,
            totalMessages = stats.totalMessages,
            uniqueUsers = stats.uniqueUsers,
            collectionEnabled = stats.collectionEnabled,
            messagesToday = 0,
            messagesYesterday = 0
        )
    }
}
