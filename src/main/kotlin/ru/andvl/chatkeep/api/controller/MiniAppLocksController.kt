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
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import ru.andvl.chatkeep.api.auth.TelegramAuthService
import ru.andvl.chatkeep.api.dto.LockDto
import ru.andvl.chatkeep.api.dto.LocksResponse
import ru.andvl.chatkeep.api.dto.UpdateLocksRequest
import ru.andvl.chatkeep.api.exception.ValidationException
import ru.andvl.chatkeep.domain.model.locks.LockType
import ru.andvl.chatkeep.domain.model.moderation.ActionType
import ru.andvl.chatkeep.domain.model.moderation.PunishmentSource
import ru.andvl.chatkeep.domain.service.ChatService
import ru.andvl.chatkeep.domain.service.locks.LockSettingsService
import ru.andvl.chatkeep.domain.service.logchannel.LogChannelService
import ru.andvl.chatkeep.domain.service.logchannel.dto.ModerationLogEntry
import ru.andvl.chatkeep.domain.service.moderation.AdminCacheService

@RestController
@RequestMapping("/api/v1/miniapp/chats/{chatId}/locks")
@Tag(name = "Mini App - Locks", description = "Lock settings management")
@SecurityRequirement(name = "TelegramAuth")
class MiniAppLocksController(
    private val lockSettingsService: LockSettingsService,
    adminCacheService: AdminCacheService,
    private val logChannelService: LogChannelService,
    private val chatService: ChatService
) : BaseMiniAppController(adminCacheService) {

    @GetMapping
    @Operation(summary = "Get lock settings")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Success"),
        ApiResponse(responseCode = "403", description = "Forbidden - not admin")
    )
    suspend fun getLocks(
        @PathVariable chatId: Long,
        request: HttpServletRequest
    ): LocksResponse {
        requireAdmin(request, chatId)

        val locks = lockSettingsService.getAllLocks(chatId)
        val lockWarnsEnabled = lockSettingsService.isLockWarnsEnabled(chatId)

        val locksMap = locks.map { (lockType, config) ->
            lockType.name to LockDto(
                locked = config.locked,
                reason = config.reason
            )
        }.toMap()

        return LocksResponse(
            chatId = chatId,
            locks = locksMap,
            lockWarnsEnabled = lockWarnsEnabled
        )
    }

    @PutMapping
    @Transactional
    @Operation(summary = "Update lock settings")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Success"),
        ApiResponse(responseCode = "400", description = "Validation error"),
        ApiResponse(responseCode = "403", description = "Forbidden - not admin")
    )
    suspend fun updateLocks(
        @PathVariable chatId: Long,
        @Valid @RequestBody updateRequest: UpdateLocksRequest,
        request: HttpServletRequest
    ): LocksResponse {
        val user = requireAdmin(request, chatId, forceRefresh = true)

        // Fetch current state once before updates (avoid N+1 queries)
        val currentLocks = lockSettingsService.getAllLocks(chatId)
        val chatSettings = chatService.getSettings(chatId)

        // Update each lock and log changes
        updateRequest.locks.forEach { (lockTypeName, lockDto) ->
            val lockType = try {
                LockType.valueOf(lockTypeName)
            } catch (e: IllegalArgumentException) {
                throw ValidationException("Invalid lock type: $lockTypeName")
            }

            // Get old state to check if it changed
            val oldConfig = currentLocks[lockType]
            val oldLocked = oldConfig?.locked ?: false

            lockSettingsService.setLock(
                chatId = chatId,
                lockType = lockType,
                locked = lockDto.locked,
                reason = lockDto.reason
            )

            // Log lock change if state changed
            if (oldLocked != lockDto.locked) {
                logChannelService.logModerationAction(
                    ModerationLogEntry(
                        chatId = chatId,
                        chatTitle = chatSettings?.chatTitle,
                        adminId = user.id,
                        adminFirstName = user.firstName,
                        adminLastName = user.lastName,
                        adminUserName = user.username,
                        actionType = if (lockDto.locked) ActionType.LOCK_ENABLED else ActionType.LOCK_DISABLED,
                        reason = lockType.name,
                        source = PunishmentSource.MANUAL
                    )
                )
            }
        }

        // Update lock warns if provided and log the change
        updateRequest.lockWarnsEnabled?.let { newValue ->
            val oldValue = lockSettingsService.isLockWarnsEnabled(chatId)
            if (oldValue != newValue) {
                lockSettingsService.setLockWarns(chatId, newValue)

                // Log lock warns change
                logChannelService.logModerationAction(
                    ModerationLogEntry(
                        chatId = chatId,
                        chatTitle = chatSettings?.chatTitle,
                        adminId = user.id,
                        adminFirstName = user.firstName,
                        adminLastName = user.lastName,
                        adminUserName = user.username,
                        actionType = if (newValue) ActionType.LOCK_WARNS_ON else ActionType.LOCK_WARNS_OFF,
                        source = PunishmentSource.MANUAL
                    )
                )
            }
        }

        return getLocks(chatId, request)
    }
}
