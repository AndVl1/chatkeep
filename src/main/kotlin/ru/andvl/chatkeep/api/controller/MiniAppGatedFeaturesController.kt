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
import ru.andvl.chatkeep.api.dto.FeatureStatusDto
import ru.andvl.chatkeep.api.dto.SetFeatureRequest
import ru.andvl.chatkeep.api.exception.AccessDeniedException
import ru.andvl.chatkeep.api.exception.UnauthorizedException
import ru.andvl.chatkeep.domain.service.gated.GatedFeatureService
import ru.andvl.chatkeep.domain.service.moderation.AdminCacheService

@RestController
@RequestMapping("/api/v1/miniapp/chats/{chatId}/features")
@Tag(name = "Mini App - Gated Features", description = "Manage gated features for chats")
@SecurityRequirement(name = "TelegramAuth")
class MiniAppGatedFeaturesController(
    private val gatedFeatureService: GatedFeatureService,
    private val adminCacheService: AdminCacheService
) {

    private fun getUserFromRequest(request: HttpServletRequest): TelegramAuthService.TelegramUser {
        return request.getAttribute(TelegramAuthFilter.USER_ATTR) as? TelegramAuthService.TelegramUser
            ?: throw UnauthorizedException("User not authenticated")
    }

    @GetMapping
    @Operation(summary = "Get all features and their statuses for a chat")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Success"),
        ApiResponse(responseCode = "403", description = "Forbidden - not admin")
    )
    fun getFeatures(
        @PathVariable chatId: Long,
        request: HttpServletRequest
    ): List<FeatureStatusDto> {
        val user = getUserFromRequest(request)

        val isAdmin = runBlocking(Dispatchers.IO) {
            adminCacheService.isAdmin(user.id, chatId, forceRefresh = false)
        }
        if (!isAdmin) {
            throw AccessDeniedException("You are not an admin in this chat")
        }

        return gatedFeatureService.getFeatures(chatId).map { feature ->
            FeatureStatusDto(
                key = feature.key,
                enabled = feature.enabled,
                name = feature.name,
                description = feature.description,
                enabledAt = feature.enabledAt,
                enabledBy = feature.enabledBy
            )
        }
    }

    @PutMapping("/{featureKey}")
    @Operation(summary = "Enable or disable a specific feature")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Success"),
        ApiResponse(responseCode = "400", description = "Invalid feature key"),
        ApiResponse(responseCode = "403", description = "Forbidden - not admin")
    )
    fun setFeature(
        @PathVariable chatId: Long,
        @PathVariable featureKey: String,
        @Valid @RequestBody request: SetFeatureRequest,
        httpRequest: HttpServletRequest
    ): FeatureStatusDto {
        val user = getUserFromRequest(httpRequest)

        val isAdmin = runBlocking(Dispatchers.IO) {
            adminCacheService.isAdmin(user.id, chatId, forceRefresh = true)
        }
        if (!isAdmin) {
            throw AccessDeniedException("You are not an admin in this chat")
        }

        val feature = gatedFeatureService.setFeature(chatId, featureKey, request.enabled, user.id)

        return FeatureStatusDto(
            key = feature.key,
            enabled = feature.enabled,
            name = feature.name,
            description = feature.description,
            enabledAt = feature.enabledAt,
            enabledBy = feature.enabledBy
        )
    }
}
