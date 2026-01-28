package ru.andvl.chatkeep.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.andvl.chatkeep.api.dto.ChatStatisticsResponse
import ru.andvl.chatkeep.domain.model.ChatType
import ru.andvl.chatkeep.infrastructure.repository.ChatSettingsRepository
import ru.andvl.chatkeep.infrastructure.repository.MessageRepository

@RestController
@RequestMapping("/api/v1/admin/chats")
@Tag(name = "Admin - Chats", description = "Admin chat statistics")
@SecurityRequirement(name = "BearerAuth")
class AdminChatsController(
    private val chatSettingsRepository: ChatSettingsRepository,
    private val messageRepository: MessageRepository
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping
    @Operation(summary = "Get chat statistics", description = "Returns list of all chats with message counts for today and yesterday. Channels are excluded by default.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Success"),
        ApiResponse(responseCode = "401", description = "Unauthorized")
    )
    fun getChats(
        @RequestParam(required = false, defaultValue = "false") includeChannels: Boolean
    ): ResponseEntity<List<ChatStatisticsResponse>> {
        val allChats = if (includeChannels) {
            chatSettingsRepository.findAll()
        } else {
            chatSettingsRepository.findAll().filter { it.chatType != ChatType.CHANNEL }
        }

        val response = allChats.map { chat ->
            val messagesToday = messageRepository.countMessagesTodayByChatId(chat.chatId).toInt()
            val messagesYesterday = messageRepository.countMessagesYesterdayByChatId(chat.chatId).toInt()

            ChatStatisticsResponse(
                chatId = chat.chatId,
                chatTitle = chat.chatTitle,
                totalMessages = 0L,
                uniqueUsers = 0L,
                collectionEnabled = chat.collectionEnabled,
                chatType = chat.chatType.name,
                messagesToday = messagesToday,
                messagesYesterday = messagesYesterday
            )
        }.sortedByDescending { it.messagesToday }

        logger.debug("Retrieved statistics for ${response.size} chats (includeChannels=$includeChannels)")
        return ResponseEntity.ok(response)
    }
}
