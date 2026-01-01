package ru.andvl.chatkeep.domain.service.adminlogs

import tools.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import ru.andvl.chatkeep.config.AdminLogsProperties
import ru.andvl.chatkeep.domain.service.adminlogs.dto.AdminActionLogDto
import ru.andvl.chatkeep.domain.service.adminlogs.dto.AdminLogExportDto
import ru.andvl.chatkeep.infrastructure.filesystem.FileExportPort
import ru.andvl.chatkeep.infrastructure.repository.moderation.PunishmentRepository
import java.io.File
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * Service for exporting admin action logs.
 * Orchestrates data retrieval and file export using Clean Architecture.
 */
@Service
class AdminLogExportService(
    private val punishmentRepository: PunishmentRepository,
    private val fileExportPort: FileExportPort,
    private val objectMapper: ObjectMapper,
    private val adminLogsProperties: AdminLogsProperties
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Export admin logs for a specific chat to a JSON file.
     *
     * @param chatId Chat ID to export logs for
     * @return File containing the exported logs
     */
    fun exportLogs(chatId: Long): File {
        logger.info("Exporting admin logs for chatId=$chatId")

        // Retrieve punishments with LIMIT to prevent resource exhaustion (SEC-001)
        val punishments = punishmentRepository.findByChatIdOrderByCreatedAtDesc(
            chatId = chatId,
            limit = adminLogsProperties.maxExportLimit
        )

        // Map to DTOs with messageText redaction (SEC-002)
        val actions = punishments.map { punishment ->
            AdminActionLogDto(
                actionType = punishment.punishmentType,
                userId = punishment.userId,
                issuedById = punishment.issuedById,
                durationSeconds = punishment.durationSeconds,
                reason = punishment.reason,
                messageText = redactMessageText(punishment.messageText),
                source = punishment.source,
                timestamp = punishment.createdAt
            )
        }

        // Create export DTO
        val exportDto = AdminLogExportDto(
            chatId = chatId,
            exportedAt = Instant.now().toString(),
            totalActions = actions.size,
            actions = actions
        )

        // Serialize to JSON with pretty print
        val jsonContent = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(exportDto)

        // Generate file name with timestamp
        val timestamp = DateTimeFormatter.ISO_INSTANT
            .format(Instant.now())
            .replace(":", "-")
        val fileName = "admin_logs_${chatId}_${timestamp}.json"

        // Export file using port
        val file = fileExportPort.exportJson(fileName, jsonContent)

        logger.info("Exported ${actions.size} admin actions for chatId=$chatId to ${file.absolutePath}")

        return file
    }

    /**
     * Redact message text based on privacy settings (SEC-002).
     * Returns null if includeMessageText is disabled, or truncates if enabled.
     */
    private fun redactMessageText(messageText: String?): String? {
        if (messageText == null) return null

        return if (adminLogsProperties.includeMessageText) {
            messageText.take(adminLogsProperties.maxMessageTextLength)
        } else {
            null
        }
    }

    /**
     * Delete exported log file.
     *
     * @param file File to delete
     */
    fun deleteExportedFile(file: File) {
        fileExportPort.deleteFile(file)
    }
}
