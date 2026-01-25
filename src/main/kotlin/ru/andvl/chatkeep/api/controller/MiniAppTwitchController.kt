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
import ru.andvl.chatkeep.api.exception.AccessDeniedException
import ru.andvl.chatkeep.api.exception.UnauthorizedException
import ru.andvl.chatkeep.domain.service.moderation.AdminCacheService
import ru.andvl.chatkeep.domain.service.twitch.TwitchChannelService
import ru.andvl.chatkeep.domain.service.twitch.TwitchNotificationService
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
    private val adminCacheService: AdminCacheService
) {

    private fun getUserFromRequest(request: HttpServletRequest): TelegramAuthService.TelegramUser {
        return request.getAttribute(TelegramAuthFilter.USER_ATTR) as? TelegramAuthService.TelegramUser
            ?: throw UnauthorizedException("User not authenticated")
    }

    @GetMapping("/channels")
    @Operation(summary = "Get subscribed Twitch channels for a chat")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Success"),
        ApiResponse(responseCode = "403", description = "Forbidden - not admin")
    )
    fun getChannels(
        @PathVariable chatId: Long,
        request: HttpServletRequest
    ): List<TwitchChannelDto> {
        val user = getUserFromRequest(request)

        val isAdmin = runBlocking(Dispatchers.IO) {
            adminCacheService.isAdmin(user.id, chatId, forceRefresh = false)
        }
        if (!isAdmin) {
            throw AccessDeniedException("You are not an admin in this chat")
        }

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
                isLive = sub.id in liveStreamIds
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
    fun addChannel(
        @PathVariable chatId: Long,
        @Valid @RequestBody addRequest: AddTwitchChannelRequest,
        request: HttpServletRequest
    ): TwitchChannelDto {
        val user = getUserFromRequest(request)

        val isAdmin = runBlocking(Dispatchers.IO) {
            adminCacheService.isAdmin(user.id, chatId, forceRefresh = true)
        }
        if (!isAdmin) {
            throw AccessDeniedException("You are not an admin in this chat")
        }

        val subscription = channelService.subscribeToChannel(chatId, addRequest.twitchLogin, user.id)
            ?: throw IllegalArgumentException("Failed to subscribe: limit reached or channel already added")

        // Check if the channel is currently live
        val isLive = streamRepo.findAllActive()
            .any { it.subscriptionId == subscription.id }

        return TwitchChannelDto(
            id = subscription.id!!,
            twitchChannelId = subscription.twitchChannelId,
            twitchLogin = subscription.twitchLogin,
            displayName = subscription.displayName,
            avatarUrl = subscription.avatarUrl,
            isLive = isLive
        )
    }

    @DeleteMapping("/channels/{subscriptionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Unsubscribe from a Twitch channel")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Deleted"),
        ApiResponse(responseCode = "403", description = "Forbidden - not admin")
    )
    fun removeChannel(
        @PathVariable chatId: Long,
        @PathVariable subscriptionId: Long,
        request: HttpServletRequest
    ) {
        val user = getUserFromRequest(request)

        val isAdmin = runBlocking(Dispatchers.IO) {
            adminCacheService.isAdmin(user.id, chatId, forceRefresh = true)
        }
        if (!isAdmin) {
            throw AccessDeniedException("You are not an admin in this chat")
        }

        channelService.unsubscribeFromChannel(subscriptionId)
    }

    @GetMapping("/settings")
    @Operation(summary = "Get Twitch notification settings")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Success"),
        ApiResponse(responseCode = "403", description = "Forbidden - not admin")
    )
    fun getSettings(
        @PathVariable chatId: Long,
        request: HttpServletRequest
    ): TwitchSettingsDto {
        val user = getUserFromRequest(request)

        val isAdmin = runBlocking(Dispatchers.IO) {
            adminCacheService.isAdmin(user.id, chatId, forceRefresh = false)
        }
        if (!isAdmin) {
            throw AccessDeniedException("You are not an admin in this chat")
        }

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
    fun updateSettings(
        @PathVariable chatId: Long,
        @Valid @RequestBody updateRequest: UpdateTwitchSettingsRequest,
        request: HttpServletRequest
    ): TwitchSettingsDto {
        val user = getUserFromRequest(request)

        val isAdmin = runBlocking(Dispatchers.IO) {
            adminCacheService.isAdmin(user.id, chatId, forceRefresh = true)
        }
        if (!isAdmin) {
            throw AccessDeniedException("You are not an admin in this chat")
        }

        val settings = notificationService.updateNotificationSettings(
            chatId,
            updateRequest.messageTemplate,
            updateRequest.endedMessageTemplate,
            updateRequest.buttonText
        )
        return TwitchSettingsDto(
            messageTemplate = settings.messageTemplate,
            endedMessageTemplate = settings.endedMessageTemplate,
            buttonText = settings.buttonText
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
