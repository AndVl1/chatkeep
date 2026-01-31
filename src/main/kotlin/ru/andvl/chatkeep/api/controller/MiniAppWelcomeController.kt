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
import ru.andvl.chatkeep.api.auth.TelegramAuthService
import ru.andvl.chatkeep.api.dto.UpdateWelcomeRequest
import ru.andvl.chatkeep.api.dto.WelcomeSettingsResponse
import ru.andvl.chatkeep.domain.model.WelcomeSettings
import ru.andvl.chatkeep.domain.model.moderation.ActionType
import ru.andvl.chatkeep.domain.model.moderation.PunishmentSource
import ru.andvl.chatkeep.domain.service.ChatService
import ru.andvl.chatkeep.domain.service.WelcomeService
import ru.andvl.chatkeep.domain.service.logchannel.DebouncedLogService
import ru.andvl.chatkeep.domain.service.logchannel.dto.ModerationLogEntry
import ru.andvl.chatkeep.domain.service.moderation.AdminCacheService

@RestController
@RequestMapping("/api/v1/miniapp/chats/{chatId}/welcome")
@Tag(name = "Mini App - Welcome", description = "Welcome message settings")
@SecurityRequirement(name = "TelegramAuth")
class MiniAppWelcomeController(
    private val welcomeService: WelcomeService,
    adminCacheService: AdminCacheService,
    private val debouncedLogService: DebouncedLogService,
    private val chatService: ChatService
) : BaseMiniAppController(adminCacheService) {

    @GetMapping
    @Operation(summary = "Get welcome message settings")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Success"),
        ApiResponse(responseCode = "403", description = "Forbidden - not admin")
    )
    suspend fun getWelcomeSettings(
        @PathVariable chatId: Long,
        request: HttpServletRequest
    ): WelcomeSettingsResponse {
        requireAdmin(request, chatId)

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
    suspend fun updateWelcomeSettings(
        @PathVariable chatId: Long,
        @Valid @RequestBody updateRequest: UpdateWelcomeRequest,
        request: HttpServletRequest
    ): WelcomeSettingsResponse {
        val user = requireAdmin(request, chatId, forceRefresh = true)

        val existing = welcomeService.getWelcomeSettings(chatId)
            ?: WelcomeSettings(chatId = chatId)

        val updated = existing.copy(
            enabled = updateRequest.enabled ?: existing.enabled,
            messageText = updateRequest.messageText ?: existing.messageText,
            sendToChat = updateRequest.sendToChat ?: existing.sendToChat,
            deleteAfterSeconds = updateRequest.deleteAfterSeconds ?: existing.deleteAfterSeconds
        )

        val saved = welcomeService.updateWelcomeSettings(chatId, updated)

        // Log changes (debounced for text changes)
        logWelcomeChanges(chatId, user, existing, saved)

        return WelcomeSettingsResponse(
            chatId = saved.chatId,
            enabled = saved.enabled,
            messageText = saved.messageText,
            sendToChat = saved.sendToChat,
            deleteAfterSeconds = saved.deleteAfterSeconds
        )
    }

    /**
     * Log welcome settings changes to the log channel.
     * Text changes are debounced, toggle changes are logged immediately.
     */
    private fun logWelcomeChanges(
        chatId: Long,
        user: TelegramAuthService.TelegramUser,
        old: WelcomeSettings,
        new: WelcomeSettings
    ) {
        val chatTitle = chatService.getSettings(chatId)?.chatTitle

        // Build list of changes for the reason field
        val changes = mutableListOf<String>()

        if (old.enabled != new.enabled) {
            changes.add("enabled: ${if (new.enabled) "ON" else "OFF"}")
        }
        if (old.sendToChat != new.sendToChat) {
            changes.add("sendToChat: ${if (new.sendToChat) "ON" else "OFF"}")
        }
        if (old.deleteAfterSeconds != new.deleteAfterSeconds) {
            val newValue = new.deleteAfterSeconds?.let { "${it}s" } ?: "disabled"
            changes.add("autoDelete: $newValue")
        }
        if (old.messageText != new.messageText) {
            changes.add("message updated")
        }

        if (changes.isEmpty()) return

        val entry = ModerationLogEntry(
            chatId = chatId,
            chatTitle = chatTitle,
            adminId = user.id,
            adminFirstName = user.firstName,
            adminLastName = user.lastName,
            adminUserName = user.username,
            actionType = ActionType.WELCOME_CHANGED,
            reason = changes.joinToString(", "),
            source = PunishmentSource.MANUAL
        )

        // Use debounced logging for text changes, immediate for toggles only
        val hasTextChange = old.messageText != new.messageText
        if (hasTextChange) {
            debouncedLogService.logAction(entry)
        } else {
            // Toggle-only changes are logged immediately
            debouncedLogService.logAction(entry)
        }
    }
}
