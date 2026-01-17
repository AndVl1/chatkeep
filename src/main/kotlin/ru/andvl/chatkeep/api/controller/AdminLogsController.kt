package ru.andvl.chatkeep.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import ru.andvl.chatkeep.api.exception.ValidationException
import ru.andvl.chatkeep.api.dto.LogsResponse
import ru.andvl.chatkeep.domain.service.logs.LogService

@RestController
@RequestMapping("/api/v1/admin/logs")
@Tag(name = "Admin - Logs", description = "Application logs management")
@SecurityRequirement(name = "BearerAuth")
class AdminLogsController(
    private val logService: LogService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping
    @Operation(
        summary = "Get application logs",
        description = "Returns recent application logs with optional filtering by time, level, and text"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Success"),
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "400", description = "Invalid parameters")
    )
    fun getLogs(
        @RequestParam(defaultValue = "60") minutes: Int,
        @RequestParam(defaultValue = "INFO") level: String,
        @RequestParam(required = false) filter: String?
    ): ResponseEntity<LogsResponse> {
        // Validate minutes parameter (1 minute to 24 hours)
        if (minutes < 1 || minutes > 1440) {
            throw ValidationException("minutes must be between 1 and 1440 (24 hours)")
        }

        val logs = logService.getLogs(minutes, level, filter)
        logger.debug("Retrieved ${logs.entries.size} log entries")
        return ResponseEntity.ok(logs)
    }
}
