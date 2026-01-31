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
import ru.andvl.chatkeep.api.dto.AntifloodSettingsResponse
import ru.andvl.chatkeep.api.dto.UpdateAntifloodRequest
import ru.andvl.chatkeep.api.exception.ValidationException
import ru.andvl.chatkeep.domain.model.AntifloodSettings
import ru.andvl.chatkeep.domain.model.moderation.ActionType
import ru.andvl.chatkeep.domain.model.moderation.PunishmentSource
import ru.andvl.chatkeep.domain.model.moderation.PunishmentType
import ru.andvl.chatkeep.domain.service.AntifloodService
import ru.andvl.chatkeep.domain.service.ChatService
import ru.andvl.chatkeep.domain.service.logchannel.LogChannelService
import ru.andvl.chatkeep.domain.service.logchannel.dto.ModerationLogEntry
import ru.andvl.chatkeep.domain.service.moderation.AdminCacheService

@RestController
@RequestMapping("/api/v1/miniapp/chats/{chatId}/antiflood")
@Tag(name = "Mini App - Anti-flood", description = "Anti-flood protection settings")
@SecurityRequirement(name = "TelegramAuth")
class MiniAppAntifloodController(
    private val antifloodService: AntifloodService,
    adminCacheService: AdminCacheService,
    private val logChannelService: LogChannelService,
    private val chatService: ChatService
) : BaseMiniAppController(adminCacheService) {

    @GetMapping
    @Operation(summary = "Get anti-flood settings")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Success"),
        ApiResponse(responseCode = "403", description = "Forbidden - not admin")
    )
    suspend fun getAntifloodSettings(
        @PathVariable chatId: Long,
        request: HttpServletRequest
    ): AntifloodSettingsResponse {
        requireAdmin(request, chatId)

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
    suspend fun updateAntifloodSettings(
        @PathVariable chatId: Long,
        @Valid @RequestBody updateRequest: UpdateAntifloodRequest,
        request: HttpServletRequest
    ): AntifloodSettingsResponse {
        val user = requireAdmin(request, chatId, forceRefresh = true)

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

        // Log changes
        logAntifloodChanges(chatId, user, existing, saved)

        return AntifloodSettingsResponse(
            chatId = saved.chatId,
            enabled = saved.enabled,
            maxMessages = saved.maxMessages,
            timeWindowSeconds = saved.timeWindowSeconds,
            action = saved.action,
            actionDurationMinutes = saved.actionDurationMinutes
        )
    }

    /**
     * Log antiflood settings changes to the log channel.
     * All changes are logged immediately (not debounced).
     */
    private fun logAntifloodChanges(
        chatId: Long,
        user: TelegramAuthService.TelegramUser,
        old: AntifloodSettings,
        new: AntifloodSettings
    ) {
        val chatTitle = chatService.getSettings(chatId)?.chatTitle

        val changes = mutableListOf<String>()

        if (old.enabled != new.enabled) {
            changes.add("enabled: ${if (new.enabled) "ON" else "OFF"}")
        }
        if (old.maxMessages != new.maxMessages) {
            changes.add("max messages: ${new.maxMessages}")
        }
        if (old.timeWindowSeconds != new.timeWindowSeconds) {
            changes.add("time window: ${new.timeWindowSeconds}s")
        }
        if (old.action != new.action) {
            changes.add("action: ${new.action}")
        }
        if (old.actionDurationMinutes != new.actionDurationMinutes) {
            val duration = new.actionDurationMinutes?.let { "${it}m" } ?: "permanent"
            changes.add("duration: $duration")
        }

        if (changes.isEmpty()) return

        val entry = ModerationLogEntry(
            chatId = chatId,
            chatTitle = chatTitle,
            adminId = user.id,
            adminFirstName = user.firstName,
            adminLastName = user.lastName,
            adminUserName = user.username,
            actionType = ActionType.ANTIFLOOD_CHANGED,
            reason = "antiflood: ${changes.joinToString(", ")}",
            source = PunishmentSource.MANUAL
        )

        logChannelService.logModerationAction(entry)
    }
}
