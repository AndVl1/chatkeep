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
import ru.andvl.chatkeep.api.auth.TelegramAuthFilter
import ru.andvl.chatkeep.api.auth.TelegramAuthService
import ru.andvl.chatkeep.api.dto.SettingsResponse
import ru.andvl.chatkeep.api.dto.UpdateSettingsRequest
import ru.andvl.chatkeep.api.exception.AccessDeniedException
import ru.andvl.chatkeep.api.exception.ResourceNotFoundException
import ru.andvl.chatkeep.api.exception.UnauthorizedException
import ru.andvl.chatkeep.api.exception.ValidationException
import ru.andvl.chatkeep.domain.model.moderation.ActionType
import ru.andvl.chatkeep.domain.model.moderation.ModerationConfig
import ru.andvl.chatkeep.domain.model.moderation.PunishmentSource
import ru.andvl.chatkeep.domain.model.moderation.PunishmentType
import ru.andvl.chatkeep.domain.service.ChatService
import ru.andvl.chatkeep.domain.service.locks.LockSettingsService
import ru.andvl.chatkeep.domain.service.logchannel.LogChannelService
import ru.andvl.chatkeep.domain.service.logchannel.dto.ModerationLogEntry
import ru.andvl.chatkeep.domain.service.moderation.AdminCacheService
import ru.andvl.chatkeep.infrastructure.repository.moderation.ModerationConfigRepository
import java.time.Instant

@RestController
@RequestMapping("/api/v1/miniapp/chats/{chatId}/settings")
@Tag(name = "Mini App - Settings", description = "Chat settings management")
@SecurityRequirement(name = "TelegramAuth")
class MiniAppSettingsController(
    private val chatService: ChatService,
    private val moderationConfigRepository: ModerationConfigRepository,
    private val lockSettingsService: LockSettingsService,
    private val adminCacheService: AdminCacheService,
    private val logChannelService: LogChannelService
) {

    private fun getUserFromRequest(request: HttpServletRequest): TelegramAuthService.TelegramUser {
        return request.getAttribute(TelegramAuthFilter.USER_ATTR) as? TelegramAuthService.TelegramUser
            ?: throw UnauthorizedException("User not authenticated")
    }

    @GetMapping
    @Operation(summary = "Get chat settings")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Success"),
        ApiResponse(responseCode = "403", description = "Forbidden - not admin"),
        ApiResponse(responseCode = "404", description = "Chat not found")
    )
    fun getSettings(
        @PathVariable chatId: Long,
        request: HttpServletRequest
    ): SettingsResponse {
        val user = getUserFromRequest(request)

        // Check admin permission (use IO dispatcher to avoid blocking main thread pool)
        val isAdmin = runBlocking(Dispatchers.IO) {
            adminCacheService.isAdmin(user.id, chatId, forceRefresh = false)
        }
        if (!isAdmin) {
            throw AccessDeniedException("You are not an admin in this chat")
        }

        // Get chat settings
        val chatSettings = chatService.getSettings(chatId)
            ?: throw ResourceNotFoundException("Chat", chatId)

        // Get moderation config
        val moderationConfig = moderationConfigRepository.findByChatId(chatId)
            ?: ModerationConfig(chatId = chatId)

        // Get lock warns setting
        val lockWarnsEnabled = lockSettingsService.isLockWarnsEnabled(chatId)

        return SettingsResponse(
            chatId = chatSettings.chatId,
            chatTitle = chatSettings.chatTitle,
            collectionEnabled = chatSettings.collectionEnabled,
            cleanServiceEnabled = moderationConfig.cleanServiceEnabled,
            maxWarnings = moderationConfig.maxWarnings,
            warningTtlHours = moderationConfig.warningTtlHours,
            thresholdAction = moderationConfig.thresholdAction,
            thresholdDurationMinutes = moderationConfig.thresholdDurationMinutes,
            defaultBlocklistAction = moderationConfig.defaultBlocklistAction,
            logChannelId = moderationConfig.logChannelId,
            lockWarnsEnabled = lockWarnsEnabled,
            locale = chatSettings.locale
        )
    }

    @PutMapping
    @Transactional
    @Operation(summary = "Update chat settings")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Success"),
        ApiResponse(responseCode = "400", description = "Validation error"),
        ApiResponse(responseCode = "403", description = "Forbidden - not admin"),
        ApiResponse(responseCode = "404", description = "Chat not found")
    )
    fun updateSettings(
        @PathVariable chatId: Long,
        @Valid @RequestBody updateRequest: UpdateSettingsRequest,
        request: HttpServletRequest
    ): SettingsResponse {
        val user = getUserFromRequest(request)

        // Check admin permission (force refresh for write operation, use IO dispatcher)
        val isAdmin = runBlocking(Dispatchers.IO) {
            adminCacheService.isAdmin(user.id, chatId, forceRefresh = true)
        }
        if (!isAdmin) {
            throw AccessDeniedException("You are not an admin in this chat")
        }

        // Validate chat exists
        val chatSettings = chatService.getSettings(chatId)
            ?: throw ResourceNotFoundException("Chat", chatId)

        // Validate enums
        updateRequest.thresholdAction?.let {
            try {
                PunishmentType.valueOf(it)
            } catch (e: IllegalArgumentException) {
                throw ValidationException("Invalid threshold action: $it")
            }
        }

        updateRequest.defaultBlocklistAction?.let {
            try {
                PunishmentType.valueOf(it)
            } catch (e: IllegalArgumentException) {
                throw ValidationException("Invalid default blocklist action: $it")
            }
        }

        // Track old settings for logging
        val oldCollectionEnabled = chatSettings.collectionEnabled
        val oldLockWarnsEnabled = lockSettingsService.isLockWarnsEnabled(chatId)

        // Update chat settings
        updateRequest.collectionEnabled?.let {
            chatService.setCollectionEnabled(chatId, it)
        }

        // Update lock warns setting
        updateRequest.lockWarnsEnabled?.let {
            lockSettingsService.setLockWarns(chatId, it)
        }

        // Update moderation config
        val existingConfig = moderationConfigRepository.findByChatId(chatId)
            ?: moderationConfigRepository.save(ModerationConfig(chatId = chatId))

        val updatedConfig = existingConfig.copy(
            cleanServiceEnabled = updateRequest.cleanServiceEnabled ?: existingConfig.cleanServiceEnabled,
            maxWarnings = updateRequest.maxWarnings ?: existingConfig.maxWarnings,
            warningTtlHours = updateRequest.warningTtlHours ?: existingConfig.warningTtlHours,
            thresholdAction = updateRequest.thresholdAction ?: existingConfig.thresholdAction,
            thresholdDurationMinutes = updateRequest.thresholdDurationMinutes ?: existingConfig.thresholdDurationMinutes,
            defaultBlocklistAction = updateRequest.defaultBlocklistAction ?: existingConfig.defaultBlocklistAction,
            logChannelId = updateRequest.logChannelId ?: existingConfig.logChannelId,
            updatedAt = Instant.now()
        )

        moderationConfigRepository.save(updatedConfig)

        // Log collection toggle with specific action type
        updateRequest.collectionEnabled?.let { newValue ->
            if (oldCollectionEnabled != newValue) {
                logChannelService.logModerationAction(
                    ModerationLogEntry(
                        chatId = chatId,
                        chatTitle = chatSettings.chatTitle,
                        adminId = user.id,
                        adminFirstName = user.firstName,
                        adminLastName = user.lastName,
                        adminUserName = user.username,
                        actionType = ActionType.CONFIG_CHANGED,
                        reason = "collection: ${if (newValue) "ON" else "OFF"}",
                        source = PunishmentSource.MANUAL
                    )
                )
            }
        }

        // Log clean service toggle with specific action type
        updateRequest.cleanServiceEnabled?.let { newValue ->
            if (existingConfig.cleanServiceEnabled != newValue) {
                logChannelService.logModerationAction(
                    ModerationLogEntry(
                        chatId = chatId,
                        chatTitle = chatSettings.chatTitle,
                        adminId = user.id,
                        adminFirstName = user.firstName,
                        adminLastName = user.lastName,
                        adminUserName = user.username,
                        actionType = if (newValue) ActionType.CLEAN_SERVICE_ON else ActionType.CLEAN_SERVICE_OFF,
                        source = PunishmentSource.MANUAL
                    )
                )
            }
        }

        // Log lock warns toggle with specific action type
        updateRequest.lockWarnsEnabled?.let { newValue ->
            if (oldLockWarnsEnabled != newValue) {
                logChannelService.logModerationAction(
                    ModerationLogEntry(
                        chatId = chatId,
                        chatTitle = chatSettings.chatTitle,
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

        // Log other config changes to log channel if any settings changed
        val changes = buildConfigChangesList(existingConfig, updatedConfig, updateRequest)
        if (changes.isNotEmpty()) {
            logChannelService.logModerationAction(
                ModerationLogEntry(
                    chatId = chatId,
                    chatTitle = chatSettings.chatTitle,
                    adminId = user.id,
                    adminFirstName = user.firstName,
                    adminLastName = user.lastName,
                    adminUserName = user.username,
                    actionType = ActionType.CONFIG_CHANGED,
                    reason = changes.joinToString(", "),
                    source = PunishmentSource.MANUAL
                )
            )
        }

        // Get updated settings to return
        return getSettings(chatId, request)
    }

    /**
     * Build a list of config changes for logging.
     * Note: cleanServiceEnabled, lockWarnsEnabled, and collectionEnabled are logged separately
     * with specific ActionTypes, so they are excluded from this list.
     */
    private fun buildConfigChangesList(
        oldConfig: ModerationConfig,
        newConfig: ModerationConfig,
        request: UpdateSettingsRequest
    ): List<String> {
        val changes = mutableListOf<String>()

        // cleanServiceEnabled is logged separately with CLEAN_SERVICE_ON/OFF action type
        // lockWarnsEnabled is logged separately with LOCK_WARNS_ON/OFF action type
        // collectionEnabled is logged separately with CONFIG_CHANGED action type

        if (request.maxWarnings != null && oldConfig.maxWarnings != newConfig.maxWarnings) {
            changes.add("maxWarnings: ${newConfig.maxWarnings}")
        }
        if (request.warningTtlHours != null && oldConfig.warningTtlHours != newConfig.warningTtlHours) {
            changes.add("warningTtl: ${newConfig.warningTtlHours}h")
        }
        if (request.thresholdAction != null && oldConfig.thresholdAction != newConfig.thresholdAction) {
            changes.add("thresholdAction: ${newConfig.thresholdAction}")
        }
        if (request.thresholdDurationMinutes != null && oldConfig.thresholdDurationMinutes != newConfig.thresholdDurationMinutes) {
            changes.add("thresholdDuration: ${newConfig.thresholdDurationMinutes}min")
        }
        if (request.defaultBlocklistAction != null && oldConfig.defaultBlocklistAction != newConfig.defaultBlocklistAction) {
            changes.add("defaultBlocklistAction: ${newConfig.defaultBlocklistAction}")
        }
        if (request.logChannelId != null && oldConfig.logChannelId != newConfig.logChannelId) {
            changes.add("logChannel: ${newConfig.logChannelId ?: "removed"}")
        }

        return changes
    }
}
