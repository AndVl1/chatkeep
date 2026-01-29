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
import ru.andvl.chatkeep.api.auth.TelegramAuthService
import ru.andvl.chatkeep.api.dto.AdminSessionResponse
import ru.andvl.chatkeep.api.dto.ModerationActionResponse
import ru.andvl.chatkeep.api.exception.ResourceNotFoundException
import ru.andvl.chatkeep.domain.service.ChatService
import ru.andvl.chatkeep.domain.service.moderation.AdminCacheService
import ru.andvl.chatkeep.domain.service.moderation.AdminSessionService

@RestController
@RequestMapping("/api/v1/miniapp/chats/{chatId}/session")
@Tag(name = "Mini App - Admin Session", description = "Admin session management")
@SecurityRequirement(name = "TelegramAuth")
class MiniAppAdminSessionController(
    private val adminSessionService: AdminSessionService,
    adminCacheService: AdminCacheService,
    private val chatService: ChatService
) : BaseMiniAppController(adminCacheService) {

    @GetMapping
    @Operation(summary = "Get current admin session for a chat")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Success"),
        ApiResponse(responseCode = "403", description = "Forbidden - not admin"),
        ApiResponse(responseCode = "404", description = "No active session")
    )
    fun getSession(
        @PathVariable chatId: Long,
        request: HttpServletRequest
    ): AdminSessionResponse {
        val user = requireAdmin(request, chatId)

        val session = adminSessionService.getSession(user.id)
            ?: throw ResourceNotFoundException("AdminSession", user.id)

        return AdminSessionResponse(
            userId = session.userId,
            connectedChatId = session.connectedChatId,
            connectedChatTitle = session.connectedChatTitle
        )
    }

    @PostMapping("/connect")
    @Operation(summary = "Connect admin to chat (create session)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Success"),
        ApiResponse(responseCode = "403", description = "Forbidden - not admin")
    )
    fun connectSession(
        @PathVariable chatId: Long,
        request: HttpServletRequest
    ): AdminSessionResponse {
        val user = requireAdmin(request, chatId, forceRefresh = true)

        val chatTitle = chatService.getSettings(chatId)?.chatTitle

        val session = adminSessionService.connect(user.id, chatId, chatTitle)

        return AdminSessionResponse(
            userId = session.userId,
            connectedChatId = session.connectedChatId,
            connectedChatTitle = session.connectedChatTitle
        )
    }

    @DeleteMapping
    @Operation(summary = "Disconnect admin from chat (delete session)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Success")
    )
    fun disconnectSession(
        request: HttpServletRequest
    ): ModerationActionResponse {
        val user = getUserFromRequest(request)

        adminSessionService.disconnect(user.id)

        return ModerationActionResponse(
            success = true,
            message = "Session disconnected successfully"
        )
    }
}
