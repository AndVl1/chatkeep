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
import org.springframework.web.bind.annotation.RestController
import ru.andvl.chatkeep.api.dto.DashboardResponse
import ru.andvl.chatkeep.api.dto.DeployInfo
import ru.andvl.chatkeep.api.dto.QuickStats
import ru.andvl.chatkeep.api.dto.ServiceStatus
import ru.andvl.chatkeep.infrastructure.repository.ChatSettingsRepository
import ru.andvl.chatkeep.infrastructure.repository.MessageRepository
import java.lang.management.ManagementFactory

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@Tag(name = "Admin - Dashboard", description = "Admin dashboard overview")
@SecurityRequirement(name = "BearerAuth")
class AdminDashboardController(
    private val messageRepository: MessageRepository,
    private val chatSettingsRepository: ChatSettingsRepository
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping
    @Operation(summary = "Get dashboard data", description = "Returns service status, deploy info, and quick statistics")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Success"),
        ApiResponse(responseCode = "401", description = "Unauthorized")
    )
    fun getDashboard(): ResponseEntity<DashboardResponse> {
        // Service is running if this endpoint is reachable
        val isRunning = true

        // Get uptime from JVM
        val uptime = ManagementFactory.getRuntimeMXBean().uptime / 1000 // seconds

        val serviceStatus = ServiceStatus(
            running = isRunning,
            uptime = uptime
        )

        // Get deploy info from environment variables
        val deployInfo = DeployInfo(
            commitSha = System.getenv("GIT_COMMIT"),
            deployedAt = System.getenv("DEPLOY_TIME"),
            imageVersion = System.getenv("IMAGE_VERSION")
        )

        // Get quick stats
        val totalChats = chatSettingsRepository.count().toInt()
        val messagesToday = messageRepository.countMessagesToday().toInt()
        val messagesYesterday = messageRepository.countMessagesYesterday().toInt()

        val quickStats = QuickStats(
            totalChats = totalChats,
            messagesToday = messagesToday,
            messagesYesterday = messagesYesterday
        )

        val response = DashboardResponse(
            serviceStatus = serviceStatus,
            deployInfo = deployInfo,
            quickStats = quickStats
        )

        logger.debug("Dashboard data retrieved: $response")
        return ResponseEntity.ok(response)
    }
}
