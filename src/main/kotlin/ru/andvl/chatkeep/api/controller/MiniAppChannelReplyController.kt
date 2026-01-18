package ru.andvl.chatkeep.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import ru.andvl.chatkeep.api.auth.TelegramAuthFilter
import ru.andvl.chatkeep.api.auth.TelegramAuthService
import ru.andvl.chatkeep.api.dto.ButtonDto
import ru.andvl.chatkeep.api.dto.ChannelReplyResponse
import ru.andvl.chatkeep.api.dto.UpdateChannelReplyRequest
import ru.andvl.chatkeep.api.exception.AccessDeniedException
import ru.andvl.chatkeep.api.exception.UnauthorizedException
import ru.andvl.chatkeep.domain.model.channelreply.ChannelReplySettings
import ru.andvl.chatkeep.domain.model.channelreply.ReplyButton
import ru.andvl.chatkeep.domain.service.channelreply.ChannelReplyService
import ru.andvl.chatkeep.domain.service.moderation.AdminCacheService

@RestController
@RequestMapping("/api/v1/miniapp/chats/{chatId}/channel-reply")
@Tag(name = "Mini App - Channel Reply", description = "Channel reply settings management")
@SecurityRequirement(name = "TelegramAuth")
class MiniAppChannelReplyController(
    private val channelReplyService: ChannelReplyService,
    private val adminCacheService: AdminCacheService
) {

    private fun getUserFromRequest(request: HttpServletRequest): TelegramAuthService.TelegramUser {
        return request.getAttribute(TelegramAuthFilter.USER_ATTR) as? TelegramAuthService.TelegramUser
            ?: throw UnauthorizedException("User not authenticated")
    }

    @GetMapping
    @Operation(summary = "Get channel reply settings")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Success"),
        ApiResponse(responseCode = "403", description = "Forbidden - not admin")
    )
    fun getChannelReply(
        @PathVariable chatId: Long,
        request: HttpServletRequest
    ): ChannelReplyResponse {
        val user = getUserFromRequest(request)

        // Check admin permission
        val isAdmin = adminCacheService.isAdminBlocking(user.id, chatId, forceRefresh = false)
        if (!isAdmin) {
            throw AccessDeniedException("You are not an admin in this chat")
        }

        val settings = channelReplyService.getSettings(chatId)
            ?: ChannelReplySettings(chatId = chatId)

        val buttons = channelReplyService.parseButtons(settings.buttonsJson)

        return ChannelReplyResponse(
            enabled = settings.enabled,
            replyText = settings.replyText,
            mediaFileId = settings.mediaFileId,
            buttons = buttons.map { ButtonDto(it.text, it.url) }
        )
    }

    @PutMapping
    @Transactional
    @Operation(summary = "Update channel reply settings")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Success"),
        ApiResponse(responseCode = "400", description = "Validation error"),
        ApiResponse(responseCode = "403", description = "Forbidden - not admin")
    )
    fun updateChannelReply(
        @PathVariable chatId: Long,
        @Valid @RequestBody updateRequest: UpdateChannelReplyRequest,
        request: HttpServletRequest
    ): ChannelReplyResponse {
        val user = getUserFromRequest(request)

        // Check admin permission (force refresh for write operation)
        val isAdmin = adminCacheService.isAdminBlocking(user.id, chatId, forceRefresh = true)
        if (!isAdmin) {
            throw AccessDeniedException("You are not an admin in this chat")
        }

        // Update enabled status
        updateRequest.enabled?.let {
            channelReplyService.setEnabled(chatId, it)
        }

        // Update reply text
        updateRequest.replyText?.let {
            channelReplyService.setText(chatId, it)
        }

        // Update buttons
        updateRequest.buttons?.let { buttonDtos ->
            val buttons = buttonDtos.map { ReplyButton(it.text, it.url) }
            channelReplyService.setButtons(chatId, buttons)
        }

        return getChannelReply(chatId, request)
    }
}
