package ru.andvl.chatkeep.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.andvl.chatkeep.api.auth.AdminAuthFilter
import ru.andvl.chatkeep.api.auth.JwtService.JwtUser
import ru.andvl.chatkeep.api.dto.ActionResponse
import java.time.Instant

@RestController
@RequestMapping("/api/v1/admin/actions")
@Tag(name = "Admin - Actions", description = "Quick admin actions")
@SecurityRequirement(name = "BearerAuth")
class AdminActionsController {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val auditLogger = LoggerFactory.getLogger("ADMIN_AUDIT")

    @PostMapping("/restart")
    @Operation(summary = "Restart bot", description = "Restarts the Telegram bot Docker container")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Restart successful"),
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "500", description = "Restart failed")
    )
    fun restartBot(request: HttpServletRequest): ResponseEntity<ActionResponse> {
        // Extract admin user from request attribute
        val adminUser = request.getAttribute(AdminAuthFilter.ADMIN_USER_ATTR) as? JwtUser
        val adminId = adminUser?.id ?: 0L
        val adminUsername = adminUser?.username ?: "unknown"

        return try {
            logAudit("restart_bot", adminId, adminUsername, "initiated")
            logger.info("Admin $adminId ($adminUsername) attempting to restart bot container")

            val process = ProcessBuilder("docker", "restart", "chatkeep-app")
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                logAudit("restart_bot", adminId, adminUsername, "success")
                logger.info("Bot container restarted successfully by admin $adminId ($adminUsername)")
                ResponseEntity.ok(
                    ActionResponse(
                        success = true,
                        message = "Bot restarted successfully"
                    )
                )
            } else {
                logAudit("restart_bot", adminId, adminUsername, "failed", "exit_code=$exitCode")
                logger.error("Failed to restart bot container. Exit code: $exitCode, Output: $output")
                ResponseEntity.ok(
                    ActionResponse(
                        success = false,
                        message = "Failed to restart bot: $output"
                    )
                )
            }
        } catch (e: Exception) {
            logAudit("restart_bot", adminId, adminUsername, "error", e.message ?: "unknown_error")
            logger.error("Error restarting bot: ${e.message}", e)
            ResponseEntity.ok(
                ActionResponse(
                    success = false,
                    message = "Error restarting bot: ${e.message}"
                )
            )
        }
    }

    /**
     * Logs an audit entry for admin actions.
     * Format: ADMIN_AUDIT | action=<action> | admin_id=<id> | admin_username=<username> | status=<status> | timestamp=<iso8601> | details=<optional>
     */
    private fun logAudit(
        action: String,
        adminId: Long,
        adminUsername: String,
        status: String,
        details: String? = null
    ) {
        val timestamp = Instant.now()
        val detailsPart = details?.let { " | details=$it" } ?: ""
        auditLogger.info(
            "ADMIN_AUDIT | action=$action | admin_id=$adminId | admin_username=$adminUsername | status=$status | timestamp=$timestamp$detailsPart"
        )
    }
}
