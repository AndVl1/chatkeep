package ru.andvl.chatkeep.domain.service.logs

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import ru.andvl.chatkeep.api.dto.LogEntry
import ru.andvl.chatkeep.api.dto.LogsResponse
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.streams.asSequence

@Service
class LogService {

    private val logger = LoggerFactory.getLogger(javaClass)

    // Regex patterns to match sensitive data
    private val sensitivePatterns = listOf(
        // JWT tokens (format: eyJ...eyJ...signature)
        Regex("eyJ[A-Za-z0-9_-]+\\.eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+"),
        // Telegram bot tokens (format: digits:base64)
        Regex("\\d{8,10}:[A-Za-z0-9_-]{35}"),
        // GitHub Personal Access Tokens
        Regex("ghp_[A-Za-z0-9]{36}"),
        // Generic password patterns
        Regex("(?i)(password|passwd|pwd)\\s*[=:]\\s*[^\\s,;\"']+"),
        // Database password in connection strings
        Regex("(?i)DB_PASSWORD\\s*[=:]\\s*[^\\s,;\"']+"),
        // Generic API keys and secrets (case insensitive)
        Regex("(?i)(api[_-]?key|apikey|secret|token)\\s*[=:]\\s*[^\\s,;\"']+"),
        // Authorization headers
        Regex("(?i)Authorization\\s*[=:]\\s*[^\\s,;\"']+"),
        // Bearer tokens
        Regex("(?i)Bearer\\s+[A-Za-z0-9_-]+"),
        // Base64 encoded strings that look like tokens (long base64 strings)
        Regex("\\b[A-Za-z0-9+/]{40,}={0,2}\\b")
    )

    /**
     * Gets structured logs with filtering support.
     *
     * @param minutes Number of minutes to look back (default: 60)
     * @param level Minimum log level to include (INFO, WARN, ERROR)
     * @param filter Optional text filter to search in log messages
     * @return LogsResponse with structured log entries
     */
    fun getLogs(minutes: Int = 60, level: String = "INFO", filter: String? = null): LogsResponse {
        val rawLines = getRecentLogs(5000)
        val toTime = Instant.now()
        val fromTime = toTime.minusSeconds(minutes * 60L)

        val entries = rawLines
            .mapNotNull { parseLogLine(it) }
            .filter { it.timestamp.isAfter(fromTime) }
            .filter { filterByLevel(it.level, level) }
            .filter { filter == null || it.message.contains(filter, ignoreCase = true) }
            .sortedByDescending { it.timestamp }

        return LogsResponse(
            entries = entries,
            totalCount = entries.size,
            fromTime = fromTime,
            toTime = toTime
        )
    }

    /**
     * Parses a log line into a structured LogEntry.
     * Expected format: 2024-01-17 10:30:45.123 INFO  [logger.name] - Log message
     */
    private fun parseLogLine(line: String): LogEntry? {
        return try {
            // Pattern: timestamp level [logger] - message
            val timestampPattern = Regex("^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3})")
            val timestampMatch = timestampPattern.find(line) ?: return null

            val timestampStr = timestampMatch.groupValues[1]
            val timestamp = ZonedDateTime.parse(
                timestampStr.replace(" ", "T") + "Z",
                DateTimeFormatter.ISO_ZONED_DATE_TIME
            ).toInstant()

            val rest = line.substring(timestampStr.length).trim()
            val parts = rest.split(Regex("\\s+"), limit = 3)

            if (parts.size < 3) return null

            val level = parts[0]
            val loggerPart = parts[1]
            val message = parts[2].removePrefix("- ").trim()

            val logger = loggerPart.removeSurrounding("[", "]")

            LogEntry(
                timestamp = timestamp,
                level = level,
                logger = logger,
                message = message
            )
        } catch (e: Exception) {
            logger.debug("Failed to parse log line: $line - ${e.message}")
            null
        }
    }

    /**
     * Filters log level based on minimum level.
     * Order: TRACE < DEBUG < INFO < WARN < ERROR
     */
    private fun filterByLevel(entryLevel: String, minLevel: String): Boolean {
        val levels = listOf("TRACE", "DEBUG", "INFO", "WARN", "ERROR")
        val entryIndex = levels.indexOf(entryLevel.uppercase())
        val minIndex = levels.indexOf(minLevel.uppercase())

        if (entryIndex == -1 || minIndex == -1) return true
        return entryIndex >= minIndex
    }

    /**
     * Gets the most recent N lines from application logs.
     * Attempts to read from multiple possible log sources:
     * 1. Docker logs (if running in container)
     * 2. Log file at /var/log/chatkeep/app.log
     * 3. Log file at /tmp/chatkeep/app.log
     * 4. Fallback to runtime logs if available
     *
     * All logs are sanitized to remove sensitive data before being returned.
     */
    fun getRecentLogs(lines: Int = 100): List<String> {
        // Try Docker logs first
        val dockerLogs = tryDockerLogs(lines)
        if (dockerLogs.isNotEmpty()) {
            logger.debug("Retrieved $lines lines from Docker logs")
            return sanitizeLogs(dockerLogs)
        }

        // Try log files
        val logPaths = listOf(
            "/var/log/chatkeep/app.log",
            "/tmp/chatkeep/app.log",
            "logs/spring.log",
            "spring.log"
        )

        for (logPath in logPaths) {
            val fileLogs = tryReadLogFile(logPath, lines)
            if (fileLogs.isNotEmpty()) {
                logger.debug("Retrieved $lines lines from log file: $logPath")
                return sanitizeLogs(fileLogs)
            }
        }

        // Fallback
        logger.warn("No log source available, returning empty list")
        return listOf("No log data available. Logs may not be configured or accessible.")
    }

    /**
     * Sanitizes log lines by redacting sensitive data patterns.
     *
     * @param lines Raw log lines that may contain sensitive data
     * @return Sanitized log lines with sensitive data replaced by [REDACTED]
     */
    private fun sanitizeLogs(lines: List<String>): List<String> {
        return lines.map { line ->
            var sanitized = line
            sensitivePatterns.forEach { pattern ->
                sanitized = pattern.replace(sanitized, "[REDACTED]")
            }
            sanitized
        }
    }

    private fun tryDockerLogs(lines: Int): List<String> {
        return try {
            val process = ProcessBuilder("docker", "logs", "--tail", lines.toString(), "chatkeep-app")
                .redirectErrorStream(true)
                .start()

            val logs = process.inputStream.bufferedReader().readLines()
            process.waitFor()

            if (process.exitValue() == 0) {
                logs
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            logger.debug("Failed to read Docker logs: ${e.message}")
            emptyList()
        }
    }

    private fun tryReadLogFile(filePath: String, lines: Int): List<String> {
        return try {
            val file = File(filePath)
            if (!file.exists() || !file.canRead()) {
                return emptyList()
            }

            // Read last N lines efficiently
            Files.lines(Paths.get(filePath)).use { stream ->
                val allLines = stream.asSequence().toList()
                allLines.takeLast(lines)
            }
        } catch (e: Exception) {
            logger.debug("Failed to read log file $filePath: ${e.message}")
            emptyList()
        }
    }
}
