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
import ru.andvl.chatkeep.api.dto.*
import ru.andvl.chatkeep.api.exception.AccessDeniedException
import ru.andvl.chatkeep.api.exception.UnauthorizedException
import ru.andvl.chatkeep.domain.model.moderation.PunishmentSource
import ru.andvl.chatkeep.domain.model.moderation.PunishmentType
import ru.andvl.chatkeep.domain.service.ChatService
import ru.andvl.chatkeep.domain.service.moderation.AdminCacheService
import ru.andvl.chatkeep.domain.service.moderation.PunishmentService
import ru.andvl.chatkeep.domain.service.moderation.WarningService
import kotlin.time.Duration.Companion.minutes

@RestController
@RequestMapping("/api/v1/miniapp/chats/{chatId}/moderation")
@Tag(name = "Mini App - Moderation", description = "Moderation actions")
@SecurityRequirement(name = "TelegramAuth")
class MiniAppModerationController(
    private val warningService: WarningService,
    private val punishmentService: PunishmentService,
    private val adminCacheService: AdminCacheService,
    private val chatService: ChatService
) {

    private fun getUserFromRequest(request: HttpServletRequest): TelegramAuthService.TelegramUser {
        return request.getAttribute(TelegramAuthFilter.USER_ATTR) as? TelegramAuthService.TelegramUser
            ?: throw UnauthorizedException("User not authenticated")
    }

    private suspend fun checkAdminAccess(userId: Long, chatId: Long) {
        val isAdmin = adminCacheService.isAdmin(userId, chatId, forceRefresh = true)
        if (!isAdmin) {
            throw AccessDeniedException("You are not an admin in this chat")
        }
    }

    @PostMapping("/warn")
    @Operation(summary = "Issue a warning to a user")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Success"),
        ApiResponse(responseCode = "403", description = "Forbidden - not admin")
    )
    fun warnUser(
        @PathVariable chatId: Long,
        @Valid @RequestBody warnRequest: WarnRequest,
        request: HttpServletRequest
    ): ModerationActionResponse = runBlocking(Dispatchers.IO) {
        val user = getUserFromRequest(request)
        checkAdminAccess(user.id, chatId)

        val chatTitle = chatService.getSettings(chatId)?.chatTitle

        val result = warningService.issueWarningWithThreshold(
            chatId = chatId,
            userId = warnRequest.userId,
            issuedById = user.id,
            reason = warnRequest.reason,
            chatTitle = chatTitle
        )

        // If threshold triggered, execute punishment
        if (result.thresholdTriggered && result.thresholdAction != null) {
            val duration = result.thresholdDurationMinutes?.minutes
            punishmentService.executePunishment(
                chatId = chatId,
                userId = warnRequest.userId,
                issuedById = user.id,
                type = result.thresholdAction,
                duration = duration,
                reason = "Warning threshold reached",
                source = PunishmentSource.THRESHOLD,
                chatTitle = chatTitle
            )
        }

        ModerationActionResponse(
            success = true,
            message = "Warning issued. Count: ${result.warningResult.activeCount}/${result.warningResult.maxWarnings}"
        )
    }

    @PostMapping("/mute")
    @Operation(summary = "Mute a user")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Success"),
        ApiResponse(responseCode = "403", description = "Forbidden - not admin")
    )
    fun muteUser(
        @PathVariable chatId: Long,
        @Valid @RequestBody muteRequest: MuteRequest,
        request: HttpServletRequest
    ): ModerationActionResponse = runBlocking(Dispatchers.IO) {
        val user = getUserFromRequest(request)
        checkAdminAccess(user.id, chatId)

        val chatTitle = chatService.getSettings(chatId)?.chatTitle
        val duration = muteRequest.durationMinutes?.minutes

        val success = punishmentService.executePunishment(
            chatId = chatId,
            userId = muteRequest.userId,
            issuedById = user.id,
            type = PunishmentType.MUTE,
            duration = duration,
            reason = muteRequest.reason,
            source = PunishmentSource.MANUAL,
            chatTitle = chatTitle
        )

        ModerationActionResponse(
            success = success,
            message = if (success) "User muted successfully" else "Failed to mute user"
        )
    }

    @PostMapping("/ban")
    @Operation(summary = "Ban a user")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Success"),
        ApiResponse(responseCode = "403", description = "Forbidden - not admin")
    )
    fun banUser(
        @PathVariable chatId: Long,
        @Valid @RequestBody banRequest: BanRequest,
        request: HttpServletRequest
    ): ModerationActionResponse = runBlocking(Dispatchers.IO) {
        val user = getUserFromRequest(request)
        checkAdminAccess(user.id, chatId)

        val chatTitle = chatService.getSettings(chatId)?.chatTitle
        val duration = banRequest.durationMinutes?.minutes

        val success = punishmentService.executePunishment(
            chatId = chatId,
            userId = banRequest.userId,
            issuedById = user.id,
            type = PunishmentType.BAN,
            duration = duration,
            reason = banRequest.reason,
            source = PunishmentSource.MANUAL,
            chatTitle = chatTitle
        )

        ModerationActionResponse(
            success = success,
            message = if (success) "User banned successfully" else "Failed to ban user"
        )
    }

    @PostMapping("/kick")
    @Operation(summary = "Kick a user")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Success"),
        ApiResponse(responseCode = "403", description = "Forbidden - not admin")
    )
    fun kickUser(
        @PathVariable chatId: Long,
        @Valid @RequestBody kickRequest: KickRequest,
        request: HttpServletRequest
    ): ModerationActionResponse = runBlocking(Dispatchers.IO) {
        val user = getUserFromRequest(request)
        checkAdminAccess(user.id, chatId)

        val chatTitle = chatService.getSettings(chatId)?.chatTitle

        val success = punishmentService.executePunishment(
            chatId = chatId,
            userId = kickRequest.userId,
            issuedById = user.id,
            type = PunishmentType.KICK,
            duration = null,
            reason = kickRequest.reason,
            source = PunishmentSource.MANUAL,
            chatTitle = chatTitle
        )

        ModerationActionResponse(
            success = success,
            message = if (success) "User kicked successfully" else "Failed to kick user"
        )
    }

    @DeleteMapping("/warnings/{userId}")
    @Operation(summary = "Remove warnings from a user")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Success"),
        ApiResponse(responseCode = "403", description = "Forbidden - not admin")
    )
    fun removeWarnings(
        @PathVariable chatId: Long,
        @PathVariable userId: Long,
        request: HttpServletRequest
    ): ModerationActionResponse = runBlocking(Dispatchers.IO) {
        val user = getUserFromRequest(request)
        checkAdminAccess(user.id, chatId)

        val chatTitle = chatService.getSettings(chatId)?.chatTitle

        warningService.removeWarnings(chatId, userId, user.id, chatTitle)

        ModerationActionResponse(
            success = true,
            message = "Warnings removed successfully"
        )
    }

    @DeleteMapping("/mute/{userId}")
    @Operation(summary = "Unmute a user")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Success"),
        ApiResponse(responseCode = "403", description = "Forbidden - not admin")
    )
    fun unmuteUser(
        @PathVariable chatId: Long,
        @PathVariable userId: Long,
        request: HttpServletRequest
    ): ModerationActionResponse = runBlocking(Dispatchers.IO) {
        val user = getUserFromRequest(request)
        checkAdminAccess(user.id, chatId)

        val success = punishmentService.unmute(chatId, userId, user.id)

        ModerationActionResponse(
            success = success,
            message = if (success) "User unmuted successfully" else "Failed to unmute user"
        )
    }

    @DeleteMapping("/ban/{userId}")
    @Operation(summary = "Unban a user")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Success"),
        ApiResponse(responseCode = "403", description = "Forbidden - not admin")
    )
    fun unbanUser(
        @PathVariable chatId: Long,
        @PathVariable userId: Long,
        request: HttpServletRequest
    ): ModerationActionResponse = runBlocking(Dispatchers.IO) {
        val user = getUserFromRequest(request)
        checkAdminAccess(user.id, chatId)

        val success = punishmentService.unban(chatId, userId, user.id)

        ModerationActionResponse(
            success = success,
            message = if (success) "User unbanned successfully" else "Failed to unban user"
        )
    }
}
