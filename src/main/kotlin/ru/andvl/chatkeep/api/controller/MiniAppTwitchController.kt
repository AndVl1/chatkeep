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
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import ru.andvl.chatkeep.api.auth.TelegramAuthFilter
import ru.andvl.chatkeep.api.auth.TelegramAuthService
import ru.andvl.chatkeep.api.dto.*
import ru.andvl.chatkeep.api.exception.UnauthorizedException
import ru.andvl.chatkeep.domain.model.moderation.ActionType
import ru.andvl.chatkeep.domain.model.moderation.PunishmentSource
import ru.andvl.chatkeep.domain.service.ChatService
import ru.andvl.chatkeep.domain.service.logchannel.DebouncedLogService
import ru.andvl.chatkeep.domain.service.logchannel.LogChannelService
import ru.andvl.chatkeep.domain.service.logchannel.dto.ModerationLogEntry
import ru.andvl.chatkeep.domain.service.moderation.AdminCacheService
import ru.andvl.chatkeep.domain.service.twitch.TwitchChannelService
import ru.andvl.chatkeep.domain.service.twitch.TwitchNotificationService
import ru.andvl.chatkeep.infrastructure.repository.twitch.TwitchChannelSubscriptionRepository
import ru.andvl.chatkeep.infrastructure.repository.twitch.TwitchStreamRepository
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@RestController
@RequestMapping("/api/v1/miniapp/chats/{chatId}/twitch")
@Tag(name = "Mini App - Twitch Integration", description = "Manage Twitch channel subscriptions")
@SecurityRequirement(name = "TelegramAuth")
class MiniAppTwitchController(
    private val channelService: TwitchChannelService,
    private val notificationService: TwitchNotificationService,
    private val streamRepo: TwitchStreamRepository,
    private val subscriptionRepo: TwitchChannelSubscriptionRepository,
    adminCacheService: AdminCacheService,
    private val logChannelService: LogChannelService,
    private val debouncedLogService: DebouncedLogService,
    private val chatService: ChatService
) : BaseMiniAppController(adminCacheService) {

    @GetMapping("/channels")
    @Operation(summary = "Get subscribed Twitch channels for a chat")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Success"),
        ApiResponse(responseCode = "403", description = "Forbidden - not admin")
    )
    suspend fun getChannels(
        @PathVariable chatId: Long,
        request: HttpServletRequest
    ): List<TwitchChannelDto> {
        requireAdmin(request, chatId)

        val subscriptions = channelService.getChannelSubscriptions(chatId)
        val subscriptionIds = subscriptions.mapNotNull { it.id }

        // Check which channels are currently live
        val liveStreamIds = streamRepo.findAllActive()
            .filter { it.subscriptionId in subscriptionIds }
            .map { it.subscriptionId }
            .toSet()

        return subscriptions.map { sub ->
            TwitchChannelDto(
                id = sub.id!!,
                twitchChannelId = sub.twitchChannelId,
                twitchLogin = sub.twitchLogin,
                displayName = sub.displayName,
                avatarUrl = sub.avatarUrl,
                isLive = sub.id in liveStreamIds,
                isPinned = sub.isPinned,
                pinSilently = sub.pinSilently
            )
        }
    }

    @PostMapping("/channels")
    @Operation(summary = "Subscribe to a Twitch channel")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Created"),
        ApiResponse(responseCode = "400", description = "Limit reached or invalid channel"),
        ApiResponse(responseCode = "403", description = "Forbidden - not admin")
    )
    suspend fun addChannel(
        @PathVariable chatId: Long,
        @Valid @RequestBody addRequest: AddTwitchChannelRequest,
        request: HttpServletRequest
    ): TwitchChannelDto {
        val user = requireAdmin(request, chatId, forceRefresh = true)

        val subscription = channelService.subscribeToChannel(chatId, addRequest.twitchLogin, user.id)
            ?: throw IllegalArgumentException("Failed to subscribe: limit reached or channel already added")

        // Log the channel addition (immediate, not debounced)
        val chatTitle = chatService.getSettings(chatId)?.chatTitle
        logChannelService.logModerationAction(
            ModerationLogEntry(
                chatId = chatId,
                chatTitle = chatTitle,
                adminId = user.id,
                adminFirstName = user.firstName,
                adminLastName = user.lastName,
                adminUserName = user.username,
                actionType = ActionType.TWITCH_CHANNEL_ADDED,
                reason = "Twitch: ${subscription.displayName} (@${subscription.twitchLogin})",
                source = PunishmentSource.MANUAL
            )
        )

        // Check if the channel is currently live
        val isLive = streamRepo.findAllActive()
            .any { it.subscriptionId == subscription.id }

        return TwitchChannelDto(
            id = subscription.id!!,
            twitchChannelId = subscription.twitchChannelId,
            twitchLogin = subscription.twitchLogin,
            displayName = subscription.displayName,
            avatarUrl = subscription.avatarUrl,
            isLive = isLive,
            isPinned = subscription.isPinned,
            pinSilently = subscription.pinSilently
        )
    }

    @DeleteMapping("/channels/{subscriptionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Unsubscribe from a Twitch channel")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Deleted"),
        ApiResponse(responseCode = "403", description = "Forbidden - not admin")
    )
    suspend fun removeChannel(
        @PathVariable chatId: Long,
        @PathVariable subscriptionId: Long,
        request: HttpServletRequest
    ) {
        val user = requireAdmin(request, chatId, forceRefresh = true)

        // Get subscription info before deleting for logging
        val subscription = channelService.getChannelSubscriptions(chatId)
            .find { it.id == subscriptionId }

        channelService.unsubscribeFromChannel(subscriptionId)

        // Log the channel removal (immediate, not debounced)
        if (subscription != null) {
            val chatTitle = chatService.getSettings(chatId)?.chatTitle
            logChannelService.logModerationAction(
                ModerationLogEntry(
                    chatId = chatId,
                    chatTitle = chatTitle,
                    adminId = user.id,
                    adminFirstName = user.firstName,
                    adminLastName = user.lastName,
                    adminUserName = user.username,
                    actionType = ActionType.TWITCH_CHANNEL_REMOVED,
                    reason = "Twitch: ${subscription.displayName} (@${subscription.twitchLogin})",
                    source = PunishmentSource.MANUAL
                )
            )
        }
    }

    @PutMapping("/channels/{subscriptionId}/pin")
    @Operation(summary = "Pin a Twitch channel (only one channel can be pinned per chat)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Success"),
        ApiResponse(responseCode = "403", description = "Forbidden - not admin"),
        ApiResponse(responseCode = "404", description = "Channel not found")
    )
    suspend fun pinChannel(
        @PathVariable chatId: Long,
        @PathVariable subscriptionId: Long,
        @Valid @RequestBody pinRequest: PinChannelRequest,
        request: HttpServletRequest
    ): TwitchChannelDto {
        val user = requireAdmin(request, chatId, forceRefresh = true)

        // Find subscription
        val subscription = subscriptionRepo.findById(subscriptionId).orElse(null)
            ?: throw IllegalArgumentException("Subscription not found")

        if (subscription.chatId != chatId) {
            throw IllegalArgumentException("Subscription does not belong to this chat")
        }

        // Unpin all other channels for this chat
        subscriptionRepo.unpinAllForChat(chatId)

        // Pin this channel
        val updated = subscriptionRepo.save(
            subscription.copy(
                isPinned = true,
                pinSilently = pinRequest.pinSilently
            )
        )

        // Log the pin action
        val chatTitle = chatService.getSettings(chatId)?.chatTitle
        logChannelService.logModerationAction(
            ModerationLogEntry(
                chatId = chatId,
                chatTitle = chatTitle,
                adminId = user.id,
                adminFirstName = user.firstName,
                adminLastName = user.lastName,
                adminUserName = user.username,
                actionType = ActionType.TWITCH_CHANNEL_PINNED,
                reason = "Twitch: ${subscription.displayName} (@${subscription.twitchLogin})",
                source = PunishmentSource.MANUAL
            )
        )

        // Check if live
        val isLive = streamRepo.findAllActive()
            .any { it.subscriptionId == subscriptionId }

        return TwitchChannelDto(
            id = updated.id!!,
            twitchChannelId = updated.twitchChannelId,
            twitchLogin = updated.twitchLogin,
            displayName = updated.displayName,
            avatarUrl = updated.avatarUrl,
            isLive = isLive,
            isPinned = updated.isPinned,
            pinSilently = updated.pinSilently
        )
    }

    @DeleteMapping("/channels/{subscriptionId}/pin")
    @Operation(summary = "Unpin a Twitch channel")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Success"),
        ApiResponse(responseCode = "403", description = "Forbidden - not admin"),
        ApiResponse(responseCode = "404", description = "Channel not found")
    )
    suspend fun unpinChannel(
        @PathVariable chatId: Long,
        @PathVariable subscriptionId: Long,
        request: HttpServletRequest
    ): TwitchChannelDto {
        val user = requireAdmin(request, chatId, forceRefresh = true)

        // Find subscription
        val subscription = subscriptionRepo.findById(subscriptionId).orElse(null)
            ?: throw IllegalArgumentException("Subscription not found")

        if (subscription.chatId != chatId) {
            throw IllegalArgumentException("Subscription does not belong to this chat")
        }

        // Unpin this channel
        val updated = subscriptionRepo.save(
            subscription.copy(isPinned = false)
        )

        // Log the unpin action
        val chatTitle = chatService.getSettings(chatId)?.chatTitle
        logChannelService.logModerationAction(
            ModerationLogEntry(
                chatId = chatId,
                chatTitle = chatTitle,
                adminId = user.id,
                adminFirstName = user.firstName,
                adminLastName = user.lastName,
                adminUserName = user.username,
                actionType = ActionType.TWITCH_CHANNEL_UNPINNED,
                reason = "Twitch: ${subscription.displayName} (@${subscription.twitchLogin})",
                source = PunishmentSource.MANUAL
            )
        )

        // Check if live
        val isLive = streamRepo.findAllActive()
            .any { it.subscriptionId == subscriptionId }

        return TwitchChannelDto(
            id = updated.id!!,
            twitchChannelId = updated.twitchChannelId,
            twitchLogin = updated.twitchLogin,
            displayName = updated.displayName,
            avatarUrl = updated.avatarUrl,
            isLive = isLive,
            isPinned = updated.isPinned,
            pinSilently = updated.pinSilently
        )
    }

    @GetMapping("/settings")
    @Operation(summary = "Get Twitch notification settings")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Success"),
        ApiResponse(responseCode = "403", description = "Forbidden - not admin")
    )
    suspend fun getSettings(
        @PathVariable chatId: Long,
        request: HttpServletRequest
    ): TwitchSettingsDto {
        requireAdmin(request, chatId)

        val settings = notificationService.getNotificationSettings(chatId)
        return TwitchSettingsDto(
            messageTemplate = settings.messageTemplate,
            endedMessageTemplate = settings.endedMessageTemplate,
            buttonText = settings.buttonText
        )
    }

    @PutMapping("/settings")
    @Operation(summary = "Update Twitch notification settings")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Success"),
        ApiResponse(responseCode = "403", description = "Forbidden - not admin")
    )
    suspend fun updateSettings(
        @PathVariable chatId: Long,
        @Valid @RequestBody updateRequest: UpdateTwitchSettingsRequest,
        request: HttpServletRequest
    ): TwitchSettingsDto {
        val user = requireAdmin(request, chatId, forceRefresh = true)

        // Get existing settings for comparison
        val existing = notificationService.getNotificationSettings(chatId)

        val settings = notificationService.updateNotificationSettings(
            chatId,
            updateRequest.messageTemplate,
            updateRequest.endedMessageTemplate,
            updateRequest.buttonText
        )

        // Log template changes (debounced since templates are text fields)
        logTwitchSettingsChanges(chatId, user, existing, settings)

        return TwitchSettingsDto(
            messageTemplate = settings.messageTemplate,
            endedMessageTemplate = settings.endedMessageTemplate,
            buttonText = settings.buttonText
        )
    }

    /**
     * Log Twitch notification settings changes.
     * All template changes are debounced since they are text fields that can sync in real-time.
     */
    private fun logTwitchSettingsChanges(
        chatId: Long,
        user: TelegramAuthService.TelegramUser,
        old: ru.andvl.chatkeep.domain.model.twitch.TwitchNotificationSettings,
        new: ru.andvl.chatkeep.domain.model.twitch.TwitchNotificationSettings
    ) {
        val changes = mutableListOf<String>()

        if (old.messageTemplate != new.messageTemplate) {
            changes.add("stream start template")
        }
        if (old.endedMessageTemplate != new.endedMessageTemplate) {
            changes.add("stream end template")
        }
        if (old.buttonText != new.buttonText) {
            changes.add("button text")
        }

        if (changes.isEmpty()) return

        val chatTitle = chatService.getSettings(chatId)?.chatTitle
        debouncedLogService.logAction(
            ModerationLogEntry(
                chatId = chatId,
                chatTitle = chatTitle,
                adminId = user.id,
                adminFirstName = user.firstName,
                adminLastName = user.lastName,
                adminUserName = user.username,
                actionType = ActionType.TWITCH_SETTINGS_CHANGED,
                reason = "updated: ${changes.joinToString(", ")}",
                source = PunishmentSource.MANUAL
            )
        )
    }
}

@RestController
@RequestMapping("/api/v1/miniapp/twitch")
@Tag(name = "Mini App - Twitch Search", description = "Search Twitch channels")
@SecurityRequirement(name = "TelegramAuth")
class MiniAppTwitchSearchController(
    private val channelService: TwitchChannelService
) {

    private val searchRateLimiter = ConcurrentHashMap<Long, Instant>()

    @GetMapping("/search")
    @Operation(summary = "Search Twitch channels")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Success"),
        ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    )
    fun searchChannels(
        @RequestParam query: String,
        request: HttpServletRequest
    ): List<TwitchSearchResultDto> {
        // Get user from auth context
        val user = request.getAttribute(TelegramAuthFilter.USER_ATTR) as? TelegramAuthService.TelegramUser
            ?: throw UnauthorizedException("User not authenticated")

        // Rate limit: 1 request per 2 seconds per user
        val lastSearch = searchRateLimiter[user.id]
        if (lastSearch != null && Duration.between(lastSearch, Instant.now()).seconds < 2) {
            throw ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded. Please wait 2 seconds between searches.")
        }
        searchRateLimiter[user.id] = Instant.now()

        val results = channelService.searchChannels(query)
        return results.map { channel ->
            TwitchSearchResultDto(
                id = channel.id,
                login = channel.broadcaster_login,
                displayName = channel.display_name,
                avatarUrl = channel.thumbnail_url,
                isLive = channel.is_live
            )
        }
    }
}
