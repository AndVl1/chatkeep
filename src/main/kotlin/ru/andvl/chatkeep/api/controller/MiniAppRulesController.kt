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
import ru.andvl.chatkeep.api.dto.RulesResponse
import ru.andvl.chatkeep.api.dto.UpdateRulesRequest
import ru.andvl.chatkeep.api.exception.AccessDeniedException
import ru.andvl.chatkeep.api.exception.ResourceNotFoundException
import ru.andvl.chatkeep.api.exception.UnauthorizedException
import ru.andvl.chatkeep.domain.service.RulesService
import ru.andvl.chatkeep.domain.service.moderation.AdminCacheService

@RestController
@RequestMapping("/api/v1/miniapp/chats/{chatId}/rules")
@Tag(name = "Mini App - Rules", description = "Chat rules management")
@SecurityRequirement(name = "TelegramAuth")
class MiniAppRulesController(
    private val rulesService: RulesService,
    private val adminCacheService: AdminCacheService
) {

    private fun getUserFromRequest(request: HttpServletRequest): TelegramAuthService.TelegramUser {
        return request.getAttribute(TelegramAuthFilter.USER_ATTR) as? TelegramAuthService.TelegramUser
            ?: throw UnauthorizedException("User not authenticated")
    }

    @GetMapping
    @Operation(summary = "Get chat rules")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Success"),
        ApiResponse(responseCode = "403", description = "Forbidden - not admin"),
        ApiResponse(responseCode = "404", description = "Rules not set")
    )
    fun getRules(
        @PathVariable chatId: Long,
        request: HttpServletRequest
    ): RulesResponse {
        val user = getUserFromRequest(request)

        val isAdmin = runBlocking(Dispatchers.IO) {
            adminCacheService.isAdmin(user.id, chatId, forceRefresh = false)
        }
        if (!isAdmin) {
            throw AccessDeniedException("You are not an admin in this chat")
        }

        val rules = rulesService.getRules(chatId)
            ?: throw ResourceNotFoundException("Rules", chatId)

        return RulesResponse(
            chatId = rules.chatId,
            rulesText = rules.rulesText
        )
    }

    @PutMapping
    @Operation(summary = "Update chat rules")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Success"),
        ApiResponse(responseCode = "403", description = "Forbidden - not admin")
    )
    fun updateRules(
        @PathVariable chatId: Long,
        @Valid @RequestBody updateRequest: UpdateRulesRequest,
        request: HttpServletRequest
    ): RulesResponse {
        val user = getUserFromRequest(request)

        val isAdmin = runBlocking(Dispatchers.IO) {
            adminCacheService.isAdmin(user.id, chatId, forceRefresh = true)
        }
        if (!isAdmin) {
            throw AccessDeniedException("You are not an admin in this chat")
        }

        val saved = rulesService.setRules(chatId, updateRequest.rulesText)

        return RulesResponse(
            chatId = saved.chatId,
            rulesText = saved.rulesText
        )
    }
}
