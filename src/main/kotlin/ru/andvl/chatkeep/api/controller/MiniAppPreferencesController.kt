package ru.andvl.chatkeep.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*
import ru.andvl.chatkeep.api.auth.TelegramAuthFilter
import ru.andvl.chatkeep.api.auth.TelegramAuthService
import ru.andvl.chatkeep.api.dto.UpdatePreferencesRequest
import ru.andvl.chatkeep.api.dto.UserPreferencesResponse
import ru.andvl.chatkeep.api.exception.UnauthorizedException
import ru.andvl.chatkeep.domain.service.i18n.LocaleService

@RestController
@RequestMapping("/api/v1/miniapp/preferences")
@Tag(name = "Mini App - User Preferences", description = "User preferences management")
@SecurityRequirement(name = "TelegramAuth")
class MiniAppPreferencesController(
    private val localeService: LocaleService
) {

    private fun getUserFromRequest(request: HttpServletRequest): TelegramAuthService.TelegramUser {
        return request.getAttribute(TelegramAuthFilter.USER_ATTR) as? TelegramAuthService.TelegramUser
            ?: throw UnauthorizedException("User not authenticated")
    }

    @GetMapping
    @Operation(summary = "Get user preferences")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Success"),
        ApiResponse(responseCode = "401", description = "Unauthorized")
    )
    fun getUserPreferences(request: HttpServletRequest): UserPreferencesResponse {
        val user = getUserFromRequest(request)
        val locale = localeService.getUserLocale(user.id)

        return UserPreferencesResponse(
            userId = user.id,
            locale = locale
        )
    }

    @PutMapping
    @Operation(summary = "Update user preferences")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Success"),
        ApiResponse(responseCode = "400", description = "Validation error"),
        ApiResponse(responseCode = "401", description = "Unauthorized")
    )
    fun updateUserPreferences(
        @Valid @RequestBody updateRequest: UpdatePreferencesRequest,
        request: HttpServletRequest
    ): UserPreferencesResponse {
        val user = getUserFromRequest(request)
        val updatedLocale = localeService.setUserLocale(user.id, updateRequest.locale)

        return UserPreferencesResponse(
            userId = user.id,
            locale = updatedLocale
        )
    }
}
