package ru.andvl.chatkeep.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.springframework.web.bind.annotation.*
import ru.andvl.chatkeep.api.auth.TelegramAuthFilter
import ru.andvl.chatkeep.api.auth.TelegramAuthService
import ru.andvl.chatkeep.api.dto.UpdateWelcomeRequest
import ru.andvl.chatkeep.api.dto.WelcomeSettingsResponse
import ru.andvl.chatkeep.api.exception.AccessDeniedException
import ru.andvl.chatkeep.api.exception.UnauthorizedException
import ru.andvl.chatkeep.domain.model.WelcomeSettings
import ru.andvl.chatkeep.domain.service.WelcomeService
import ru.andvl.chatkeep.domain.service.moderation.AdminCacheService

@RestController
@RequestMapping("/api/v1/miniapp/chats/{chatId}/welcome")
@Tag(name = "Mini App - Welcome", description = "Welcome message settings")
@SecurityRequirement(name = "TelegramAuth")
class MiniAppWelcomeController(
    private val welcomeService: WelcomeService,
    private val adminCacheService: AdminCacheService
) {

    private fun getUserFromRequest(request: HttpServletRequest): TelegramAuthService.TelegramUser {
        return request.getAttribute(TelegramAuthFilter.USER_ATTR) as? TelegramAuthService.TelegramUser
            ?: throw UnauthorizedException("User not authenticated")
    }

    @GetMapping
    @Operation(summary = "Get welcome message settings")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Success"),
        ApiResponse(responseCode = "403", description = "Forbidden - not admin")
    )
    fun getWelcomeSettings(
        @PathVariable chatId: Long,
        request: HttpServletRequest
    ): WelcomeSettingsResponse {
        val user = getUserFromRequest(request)

        val isAdmin = runBlocking(Dispatchers.IO) {
            adminCacheService.isAdmin(user.id, chatId, forceRefresh = false)
        }
        if (!isAdmin) {
            throw AccessDeniedException("You are not an admin in this chat")
        }

        val settings = welcomeService.getWelcomeSettings(chatId)
            ?: WelcomeSettings(chatId = chatId)

        return WelcomeSettingsResponse(
            chatId = settings.chatId,
            enabled = settings.enabled,
            messageText = settings.messageText,
            sendToChat = settings.sendToChat,
            deleteAfterSeconds = settings.deleteAfterSeconds
        )
    }

    @PutMapping
    @Operation(summary = "Update welcome message settings")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Success"),
        ApiResponse(responseCode = "403", description = "Forbidden - not admin")
    )
    fun updateWelcomeSettings(
        @PathVariable chatId: Long,
        @Valid @RequestBody updateRequest: UpdateWelcomeRequest,
        request: HttpServletRequest
    ): WelcomeSettingsResponse {
        val user = getUserFromRequest(request)

        val isAdmin = runBlocking(Dispatchers.IO) {
            adminCacheService.isAdmin(user.id, chatId, forceRefresh = true)
        }
        if (!isAdmin) {
            throw AccessDeniedException("You are not an admin in this chat")
        }

        val existing = welcomeService.getWelcomeSettings(chatId)
            ?: WelcomeSettings(chatId = chatId)

        val updated = existing.copy(
            enabled = updateRequest.enabled ?: existing.enabled,
            messageText = updateRequest.messageText ?: existing.messageText,
            sendToChat = updateRequest.sendToChat ?: existing.sendToChat,
            deleteAfterSeconds = updateRequest.deleteAfterSeconds ?: existing.deleteAfterSeconds
        )

        val saved = welcomeService.updateWelcomeSettings(chatId, updated)

        return WelcomeSettingsResponse(
            chatId = saved.chatId,
            enabled = saved.enabled,
            messageText = saved.messageText,
            sendToChat = saved.sendToChat,
            deleteAfterSeconds = saved.deleteAfterSeconds
        )
    }
}
