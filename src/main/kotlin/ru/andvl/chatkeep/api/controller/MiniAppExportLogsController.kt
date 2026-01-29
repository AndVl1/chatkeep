package ru.andvl.chatkeep.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.springframework.core.io.FileSystemResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import ru.andvl.chatkeep.domain.service.adminlogs.AdminLogExportService
import ru.andvl.chatkeep.domain.service.moderation.AdminCacheService

@RestController
@RequestMapping("/api/v1/miniapp/chats/{chatId}/logs")
@Tag(name = "Mini App - Logs", description = "Admin log export")
@SecurityRequirement(name = "TelegramAuth")
class MiniAppExportLogsController(
    private val adminLogExportService: AdminLogExportService,
    adminCacheService: AdminCacheService
) : BaseMiniAppController(adminCacheService) {

    @GetMapping
    @Operation(summary = "Export admin logs as JSON file")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Success - returns JSON file"),
        ApiResponse(responseCode = "403", description = "Forbidden - not admin")
    )
    fun exportLogs(
        @PathVariable chatId: Long,
        request: HttpServletRequest
    ): ResponseEntity<FileSystemResource> {
        requireAdmin(request, chatId)

        val file = adminLogExportService.exportLogs(chatId)

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${file.name}\"")
            .contentType(MediaType.APPLICATION_JSON)
            .body(FileSystemResource(file))
    }
}
