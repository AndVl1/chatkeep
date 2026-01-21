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
import ru.andvl.chatkeep.api.dto.AntifloodSettingsResponse
import ru.andvl.chatkeep.api.dto.UpdateAntifloodRequest
import ru.andvl.chatkeep.api.exception.AccessDeniedException
import ru.andvl.chatkeep.api.exception.UnauthorizedException
import ru.andvl.chatkeep.api.exception.ValidationException
import ru.andvl.chatkeep.domain.model.AntifloodSettings
import ru.andvl.chatkeep.domain.model.moderation.PunishmentType
import ru.andvl.chatkeep.domain.service.AntifloodService
import ru.andvl.chatkeep.domain.service.moderation.AdminCacheService

@RestController
@RequestMapping("/api/v1/miniapp/chats/{chatId}/antiflood")
@Tag(name = "Mini App - Anti-flood", description = "Anti-flood protection settings")
@SecurityRequirement(name = "TelegramAuth")
class MiniAppAntifloodController(
    private val antifloodService: AntifloodService,
    private val adminCacheService: AdminCacheService
) {

    private fun getUserFromRequest(request: HttpServletRequest): TelegramAuthService.TelegramUser {
        return request.getAttribute(TelegramAuthFilter.USER_ATTR) as? TelegramAuthService.TelegramUser
            ?: throw UnauthorizedException("User not authenticated")
    }

    @GetMapping
    @Operation(summary = "Get anti-flood settings")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Success"),
        ApiResponse(responseCode = "403", description = "Forbidden - not admin")
    )
    fun getAntifloodSettings(
        @PathVariable chatId: Long,
        request: HttpServletRequest
    ): AntifloodSettingsResponse {
        val user = getUserFromRequest(request)

        val isAdmin = runBlocking(Dispatchers.IO) {
            adminCacheService.isAdmin(user.id, chatId, forceRefresh = false)
        }
        if (!isAdmin) {
            throw AccessDeniedException("You are not an admin in this chat")
        }

        val settings = antifloodService.getSettings(chatId)
            ?: AntifloodSettings(chatId = chatId)

        return AntifloodSettingsResponse(
            chatId = settings.chatId,
            enabled = settings.enabled,
            maxMessages = settings.maxMessages,
            timeWindowSeconds = settings.timeWindowSeconds,
            action = settings.action,
            actionDurationMinutes = settings.actionDurationMinutes
        )
    }

    @PutMapping
    @Operation(summary = "Update anti-flood settings")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Success"),
        ApiResponse(responseCode = "400", description = "Validation error"),
        ApiResponse(responseCode = "403", description = "Forbidden - not admin")
    )
    fun updateAntifloodSettings(
        @PathVariable chatId: Long,
        @Valid @RequestBody updateRequest: UpdateAntifloodRequest,
        request: HttpServletRequest
    ): AntifloodSettingsResponse {
        val user = getUserFromRequest(request)

        val isAdmin = runBlocking(Dispatchers.IO) {
            adminCacheService.isAdmin(user.id, chatId, forceRefresh = true)
        }
        if (!isAdmin) {
            throw AccessDeniedException("You are not an admin in this chat")
        }

        // Validate action enum
        updateRequest.action?.let {
            try {
                PunishmentType.valueOf(it)
            } catch (e: IllegalArgumentException) {
                throw ValidationException("Invalid action: $it")
            }
        }

        val existing = antifloodService.getSettings(chatId)
            ?: AntifloodSettings(chatId = chatId)

        val updated = existing.copy(
            enabled = updateRequest.enabled ?: existing.enabled,
            maxMessages = updateRequest.maxMessages ?: existing.maxMessages,
            timeWindowSeconds = updateRequest.timeWindowSeconds ?: existing.timeWindowSeconds,
            action = updateRequest.action ?: existing.action,
            actionDurationMinutes = updateRequest.actionDurationMinutes ?: existing.actionDurationMinutes
        )

        val saved = antifloodService.updateSettings(chatId, updated)

        return AntifloodSettingsResponse(
            chatId = saved.chatId,
            enabled = saved.enabled,
            maxMessages = saved.maxMessages,
            timeWindowSeconds = saved.timeWindowSeconds,
            action = saved.action,
            actionDurationMinutes = saved.actionDurationMinutes
        )
    }
}
