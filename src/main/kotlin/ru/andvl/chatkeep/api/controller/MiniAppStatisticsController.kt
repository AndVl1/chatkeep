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
import ru.andvl.chatkeep.api.dto.ChatStatisticsResponse
import ru.andvl.chatkeep.api.exception.ResourceNotFoundException
import ru.andvl.chatkeep.domain.service.AdminService
import ru.andvl.chatkeep.domain.service.moderation.AdminCacheService

@RestController
@RequestMapping("/api/v1/miniapp/chats/{chatId}")
@Tag(name = "Mini App - Statistics", description = "Chat statistics")
@SecurityRequirement(name = "TelegramAuth")
class MiniAppStatisticsController(
    private val adminService: AdminService,
    adminCacheService: AdminCacheService
) : BaseMiniAppController(adminCacheService) {

    @GetMapping("/stats")
    @Operation(summary = "Get chat statistics")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Success"),
        ApiResponse(responseCode = "403", description = "Forbidden - not admin"),
        ApiResponse(responseCode = "404", description = "Chat not found")
    )
    suspend fun getStatistics(
        @PathVariable chatId: Long,
        request: HttpServletRequest
    ): ChatStatisticsResponse {
        requireAdmin(request, chatId)

        val stats = adminService.getStatistics(chatId)
            ?: throw ResourceNotFoundException("Chat", chatId)

        return ChatStatisticsResponse(
            chatId = stats.chatId,
            chatTitle = stats.chatTitle,
            totalMessages = stats.totalMessages,
            uniqueUsers = stats.uniqueUsers,
            collectionEnabled = stats.collectionEnabled
        )
    }
}
