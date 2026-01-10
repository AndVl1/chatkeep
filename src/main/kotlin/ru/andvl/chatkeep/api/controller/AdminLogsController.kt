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
import ru.andvl.chatkeep.api.dto.LogsResponse
import ru.andvl.chatkeep.domain.service.logs.LogService
import java.time.Instant

@RestController
@RequestMapping("/api/v1/admin/logs")
@Tag(name = "Admin - Logs", description = "Application logs viewer")
@SecurityRequirement(name = "BearerAuth")
class AdminLogsController(
    private val logService: LogService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping
    @Operation(summary = "Get application logs", description = "Returns the most recent N lines from application logs")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Success"),
        ApiResponse(responseCode = "401", description = "Unauthorized")
    )
    fun getLogs(
        @RequestParam(defaultValue = "100") lines: Int
    ): ResponseEntity<LogsResponse> {
        val logLines = logService.getRecentLogs(lines.coerceIn(1, 1000))

        val response = LogsResponse(
            lines = logLines,
            timestamp = Instant.now()
        )

        logger.debug("Retrieved ${logLines.size} log lines")
        return ResponseEntity.ok(response)
    }
}
