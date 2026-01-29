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
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.multipart.MultipartFile
import ru.andvl.chatkeep.api.dto.ButtonDto
import ru.andvl.chatkeep.api.dto.ChannelReplyResponse
import ru.andvl.chatkeep.api.dto.MediaUploadResponse
import ru.andvl.chatkeep.api.dto.UpdateChannelReplyRequest
import ru.andvl.chatkeep.api.service.MediaUploadService
import ru.andvl.chatkeep.domain.model.channelreply.ChannelReplySettings
import ru.andvl.chatkeep.domain.model.channelreply.MediaType
import ru.andvl.chatkeep.domain.model.channelreply.ReplyButton
import ru.andvl.chatkeep.domain.model.moderation.ActionType
import ru.andvl.chatkeep.domain.model.moderation.PunishmentSource
import ru.andvl.chatkeep.domain.service.ChatService
import ru.andvl.chatkeep.domain.service.channelreply.ChannelReplyService
import ru.andvl.chatkeep.domain.service.logchannel.DebouncedLogService
import ru.andvl.chatkeep.domain.service.logchannel.dto.ModerationLogEntry
import ru.andvl.chatkeep.domain.service.moderation.AdminCacheService

@RestController
@RequestMapping("/api/v1/miniapp/chats/{chatId}/channel-reply")
@Tag(name = "Mini App - Channel Reply", description = "Channel reply settings management")
@SecurityRequirement(name = "TelegramAuth")
class MiniAppChannelReplyController(
    private val channelReplyService: ChannelReplyService,
    adminCacheService: AdminCacheService,
    private val mediaUploadService: MediaUploadService,
    private val mediaStorageService: ru.andvl.chatkeep.domain.service.media.MediaStorageService,
    private val linkedChannelService: ru.andvl.chatkeep.domain.service.channelreply.LinkedChannelService,
    private val debouncedLogService: DebouncedLogService,
    private val chatService: ChatService
) : BaseMiniAppController(adminCacheService) {

    @GetMapping
    @Operation(summary = "Get channel reply settings")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Success"),
        ApiResponse(responseCode = "403", description = "Forbidden - not admin")
    )
    suspend fun getChannelReply(
        @PathVariable chatId: Long,
        request: HttpServletRequest
    ): ChannelReplyResponse {
        requireAdmin(request, chatId)

        val settings = channelReplyService.getSettings(chatId)
            ?: ChannelReplySettings(chatId = chatId)

        val buttons = channelReplyService.parseButtons(settings.buttonsJson)

        // Get linked channel info
        val linkedChannel = runBlocking(Dispatchers.IO) {
            linkedChannelService.getLinkedChannel(chatId)?.let {
                ru.andvl.chatkeep.api.dto.LinkedChannelDto(it.id, it.title)
            }
        }

        val hasMedia = !settings.mediaHash.isNullOrBlank() || !settings.mediaFileId.isNullOrBlank()

        return ChannelReplyResponse(
            enabled = settings.enabled,
            replyText = settings.replyText,
            mediaFileId = settings.mediaFileId,
            mediaType = settings.mediaType,
            mediaHash = settings.mediaHash,
            hasMedia = hasMedia,
            buttons = buttons.map { ButtonDto(it.text, it.url) },
            linkedChannel = linkedChannel
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
    suspend fun updateChannelReply(
        @PathVariable chatId: Long,
        @Valid @RequestBody updateRequest: UpdateChannelReplyRequest,
        request: HttpServletRequest
    ): ChannelReplyResponse {
        val user = requireAdmin(request, chatId, forceRefresh = true)

        val existing = channelReplyService.getSettings(chatId)
            ?: ChannelReplySettings(chatId = chatId)

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

        val updated = channelReplyService.getSettings(chatId)
            ?: ChannelReplySettings(chatId = chatId)

        // Log changes
        logChannelReplyChanges(chatId, user, existing, updated)

        return getChannelReply(chatId, request)
    }

    @PostMapping("/media", consumes = ["multipart/form-data"])
    @Transactional
    @Operation(summary = "Upload media for channel reply")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Media uploaded successfully"),
        ApiResponse(responseCode = "400", description = "Validation error (file too large, invalid type, etc.)"),
        ApiResponse(responseCode = "403", description = "Forbidden - not admin")
    )
    suspend fun uploadMedia(
        @PathVariable chatId: Long,
        @RequestParam("file") file: MultipartFile,
        request: HttpServletRequest
    ): ResponseEntity<MediaUploadResponse> {
        requireAdmin(request, chatId, forceRefresh = true)

        // Store file as blob and get hash
        val hash = mediaStorageService.storeMedia(file)

        // Determine media type from MIME type
        val mediaType = when {
            file.contentType?.startsWith("image/") == true -> MediaType.PHOTO
            file.contentType?.startsWith("video/") == true -> MediaType.VIDEO
            else -> MediaType.DOCUMENT
        }

        // Save media hash to database
        channelReplyService.setMediaByHash(chatId, hash, mediaType)

        return ResponseEntity.ok(MediaUploadResponse(hash, mediaType.name))
    }

    @DeleteMapping("/media")
    @Transactional
    @Operation(summary = "Delete media from channel reply")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Media deleted successfully"),
        ApiResponse(responseCode = "403", description = "Forbidden - not admin")
    )
    suspend fun deleteMedia(
        @PathVariable chatId: Long,
        request: HttpServletRequest
    ): ResponseEntity<Void> {
        requireAdmin(request, chatId, forceRefresh = true)

        // Clear media from database
        channelReplyService.clearMedia(chatId)

        return ResponseEntity.noContent().build()
    }

    /**
     * Log channel reply settings changes to the log channel.
     * Text changes are debounced, toggle and media changes are logged immediately.
     */
    private fun logChannelReplyChanges(
        chatId: Long,
        user: TelegramAuthService.TelegramUser,
        old: ChannelReplySettings,
        new: ChannelReplySettings
    ) {
        val chatTitle = chatService.getSettings(chatId)?.chatTitle

        val changes = mutableListOf<String>()

        if (old.enabled != new.enabled) {
            changes.add("enabled: ${if (new.enabled) "ON" else "OFF"}")
        }
        if (old.replyText != new.replyText) {
            changes.add("reply text updated")
        }
        if (old.buttonsJson != new.buttonsJson) {
            changes.add("buttons updated")
        }
        if (old.mediaFileId != new.mediaFileId || old.mediaHash != new.mediaHash) {
            val mediaChange = if (new.mediaFileId.isNullOrBlank() && new.mediaHash.isNullOrBlank()) {
                "media removed"
            } else {
                "media updated"
            }
            changes.add(mediaChange)
        }

        if (changes.isEmpty()) return

        val entry = ModerationLogEntry(
            chatId = chatId,
            chatTitle = chatTitle,
            adminId = user.id,
            adminFirstName = user.firstName,
            adminLastName = user.lastName,
            adminUserName = user.username,
            actionType = ActionType.CHANNEL_REPLY_CHANGED,
            reason = "channel reply: ${changes.joinToString(", ")}",
            source = PunishmentSource.MANUAL
        )

        debouncedLogService.logAction(entry)
    }
}
