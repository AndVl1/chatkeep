package ru.andvl.chatkeep.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import ru.andvl.chatkeep.api.auth.TelegramAuthService
import ru.andvl.chatkeep.api.dto.AddBlocklistPatternRequest
import ru.andvl.chatkeep.api.dto.BlocklistPatternResponse
import ru.andvl.chatkeep.api.exception.ResourceNotFoundException
import ru.andvl.chatkeep.api.exception.ValidationException
import ru.andvl.chatkeep.domain.model.moderation.ActionType
import ru.andvl.chatkeep.domain.model.moderation.MatchType
import ru.andvl.chatkeep.domain.model.moderation.PunishmentSource
import ru.andvl.chatkeep.domain.model.moderation.PunishmentType
import ru.andvl.chatkeep.domain.service.ChatService
import ru.andvl.chatkeep.domain.service.logchannel.LogChannelService
import ru.andvl.chatkeep.domain.service.logchannel.dto.ModerationLogEntry
import ru.andvl.chatkeep.domain.service.moderation.AdminCacheService
import ru.andvl.chatkeep.domain.service.moderation.BlocklistService

@RestController
@RequestMapping("/api/v1/miniapp/chats/{chatId}/blocklist")
@Tag(name = "Mini App - Blocklist", description = "Blocklist pattern management")
@SecurityRequirement(name = "TelegramAuth")
class MiniAppBlocklistController(
    private val blocklistService: BlocklistService,
    adminCacheService: AdminCacheService,
    private val chatService: ChatService,
    private val logChannelService: LogChannelService
) : BaseMiniAppController(adminCacheService) {

    @GetMapping
    @Operation(summary = "Get blocklist patterns")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Success"),
        ApiResponse(responseCode = "403", description = "Forbidden - not admin")
    )
    suspend fun getPatterns(
        @PathVariable chatId: Long,
        request: HttpServletRequest
    ): List<BlocklistPatternResponse> {
        requireAdmin(request, chatId)

        val patterns = blocklistService.listPatterns(chatId)

        return patterns.map {
            BlocklistPatternResponse(
                id = it.id ?: 0L,
                pattern = it.pattern,
                matchType = it.matchType,
                action = it.action,
                actionDurationMinutes = it.actionDurationMinutes,
                severity = it.severity,
                createdAt = it.createdAt
            )
        }
    }

    @PostMapping
    @Transactional
    @Operation(summary = "Add blocklist pattern")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Created"),
        ApiResponse(responseCode = "400", description = "Validation error"),
        ApiResponse(responseCode = "403", description = "Forbidden - not admin")
    )
    suspend fun addPattern(
        @PathVariable chatId: Long,
        @Valid @RequestBody addRequest: AddBlocklistPatternRequest,
        request: HttpServletRequest
    ): ResponseEntity<BlocklistPatternResponse> {
        val user = requireAdmin(request, chatId, forceRefresh = true)

        // Detect match type from pattern if not provided
        val matchType = if (addRequest.matchType != null) {
            try {
                MatchType.valueOf(addRequest.matchType)
            } catch (e: IllegalArgumentException) {
                throw ValidationException("Invalid match type: ${addRequest.matchType}")
            }
        } else {
            // Auto-detect: if pattern contains * or ?, use WILDCARD, else EXACT
            if (addRequest.pattern.contains('*') || addRequest.pattern.contains('?')) {
                MatchType.WILDCARD
            } else {
                MatchType.EXACT
            }
        }

        // Get default action if not provided
        val action = if (addRequest.action != null) {
            try {
                PunishmentType.valueOf(addRequest.action)
            } catch (e: IllegalArgumentException) {
                throw ValidationException("Invalid action: ${addRequest.action}")
            }
        } else {
            blocklistService.getDefaultAction(chatId)
        }

        val result = blocklistService.addPattern(
            chatId = chatId,
            pattern = addRequest.pattern,
            matchType = matchType,
            action = action,
            durationMinutes = addRequest.actionDurationMinutes,
            severity = addRequest.severity ?: 5
        )

        val response = BlocklistPatternResponse(
            id = result.pattern.id ?: 0L,
            pattern = result.pattern.pattern,
            matchType = result.pattern.matchType,
            action = result.pattern.action,
            actionDurationMinutes = result.pattern.actionDurationMinutes,
            severity = result.pattern.severity,
            createdAt = result.pattern.createdAt
        )

        // Log blocklist addition to admin channel
        val chatSettings = chatService.getSettings(chatId)
        logChannelService.logModerationAction(
            ModerationLogEntry(
                chatId = chatId,
                chatTitle = chatSettings?.chatTitle,
                adminId = user.id,
                adminFirstName = user.firstName,
                adminLastName = user.lastName,
                adminUserName = user.username,
                actionType = ActionType.BLOCKLIST_ADDED,
                reason = "Added pattern: ${result.pattern.pattern} (${result.pattern.matchType}, action: ${result.pattern.action})",
                source = PunishmentSource.MANUAL
            )
        )

        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @DeleteMapping("/{patternId}")
    @Transactional
    @Operation(summary = "Delete blocklist pattern")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Deleted"),
        ApiResponse(responseCode = "403", description = "Forbidden - not admin"),
        ApiResponse(responseCode = "404", description = "Pattern not found")
    )
    suspend fun deletePattern(
        @PathVariable chatId: Long,
        @PathVariable patternId: Long,
        request: HttpServletRequest
    ): ResponseEntity<Void> {
        val user = requireAdmin(request, chatId, forceRefresh = true)

        // Find pattern by ID to verify it belongs to this chat (authorization check)
        val patterns = blocklistService.listPatterns(chatId)
        val pattern = patterns.find { it.id == patternId }
            ?: throw ResourceNotFoundException("Blocklist pattern", patternId)

        // Delete by ID to avoid deleting duplicate patterns with same text
        val deleted = blocklistService.deletePatternById(patternId)
        if (!deleted) {
            throw ResourceNotFoundException("Blocklist pattern", patternId)
        }

        // Get chat settings for title
        val chatSettings = chatService.getSettings(chatId)
            ?: throw ResourceNotFoundException("Chat", chatId)

        // Log blocklist removal to admin channel
        logChannelService.logModerationAction(
            ModerationLogEntry(
                chatId = chatId,
                chatTitle = chatSettings.chatTitle,
                adminId = user.id,
                adminFirstName = user.firstName,
                adminLastName = user.lastName,
                adminUserName = user.username,
                actionType = ActionType.BLOCKLIST_REMOVED,
                reason = "Removed pattern: ${pattern.pattern}",
                source = PunishmentSource.MANUAL
            )
        )

        return ResponseEntity.noContent().build()
    }
}
