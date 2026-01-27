package ru.andvl.chatkeep.bot.service

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.message.HTMLParseMode
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import ru.andvl.chatkeep.config.AdminProperties
import ru.andvl.chatkeep.metrics.BotMetricsService
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Service for notifying admins about bot errors.
 *
 * Features:
 * - Sends silent notifications to configured admin users
 * - Filters out expected errors (timeouts, rate limits)
 * - Debounces duplicate errors to prevent spam
 * - Records metrics for Grafana dashboards
 */
@Service
class AdminErrorNotificationService(
    private val bot: TelegramBot,
    private val adminProperties: AdminProperties,
    private val metricsService: BotMetricsService
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Debouncing: track last notification time per error key
    private val lastNotificationTime = ConcurrentHashMap<String, Instant>()
    private val mutex = Mutex()

    companion object {
        // Minimum interval between notifications for the same error type (5 minutes)
        private const val DEBOUNCE_INTERVAL_SECONDS = 300L

        // Maximum error message length
        private const val MAX_MESSAGE_LENGTH = 500
    }

    /**
     * Reports an error that occurred in a handler.
     * Sends notification to admins if it's a significant error (not a timeout/rate limit).
     *
     * @param handler Name of the handler where error occurred
     * @param error The exception that was thrown
     * @param context Optional context information (chat ID, user ID, command, etc.)
     */
    fun reportHandlerError(
        handler: String,
        error: Throwable,
        context: ErrorContext? = null
    ) {
        // Always record metric
        metricsService.recordHandlerError(handler)
        metricsService.recordErrorWithDetails(
            source = "handler",
            name = handler,
            errorType = classifyError(error),
            isExpected = isExpectedError(error)
        )

        // Skip notification for expected errors
        if (isExpectedError(error)) {
            logger.debug("Skipping notification for expected error in $handler: ${error.javaClass.simpleName}")
            return
        }

        // Send notification with debouncing
        val errorKey = buildErrorKey(handler, error)
        notifyAdminsDebounced(errorKey) {
            buildHandlerErrorMessage(handler, error, context)
        }
    }

    /**
     * Reports a service-level error (not tied to a specific handler).
     */
    fun reportServiceError(
        service: String,
        error: Throwable,
        context: ErrorContext? = null
    ) {
        metricsService.recordServiceError(service)
        metricsService.recordErrorWithDetails(
            source = "service",
            name = service,
            errorType = classifyError(error),
            isExpected = isExpectedError(error)
        )

        if (isExpectedError(error)) {
            logger.debug("Skipping notification for expected error in $service: ${error.javaClass.simpleName}")
            return
        }

        val errorKey = buildErrorKey(service, error)
        notifyAdminsDebounced(errorKey) {
            buildServiceErrorMessage(service, error, context)
        }
    }

    /**
     * Reports a bot-level error (from the main exception handler).
     */
    fun reportBotError(error: Throwable) {
        metricsService.recordErrorWithDetails(
            source = "bot",
            name = "global",
            errorType = classifyError(error),
            isExpected = isExpectedError(error)
        )

        if (isExpectedError(error)) {
            return
        }

        val errorKey = buildErrorKey("bot", error)
        notifyAdminsDebounced(errorKey) {
            buildBotErrorMessage(error)
        }
    }

    /**
     * Determines if an error is expected (timeout, connection issue, rate limit).
     * These errors should be logged but not trigger admin notifications.
     */
    private fun isExpectedError(error: Throwable): Boolean {
        return when (error) {
            // Network timeouts (normal for long polling)
            is HttpRequestTimeoutException,
            is SocketTimeoutException,
            is ConnectException -> true

            // Telegram API rate limit (429)
            is ClientRequestException -> {
                error.response.status == HttpStatusCode.TooManyRequests
            }

            // Check cause for wrapped exceptions
            else -> error.cause?.let { isExpectedError(it) } ?: false
        }
    }

    /**
     * Classifies error type for metrics.
     */
    private fun classifyError(error: Throwable): String {
        return when (error) {
            is HttpRequestTimeoutException -> "timeout_http"
            is SocketTimeoutException -> "timeout_socket"
            is ConnectException -> "connection_failed"
            is ClientRequestException -> {
                when (error.response.status) {
                    HttpStatusCode.TooManyRequests -> "rate_limit"
                    HttpStatusCode.BadRequest -> "bad_request"
                    HttpStatusCode.Forbidden -> "forbidden"
                    HttpStatusCode.NotFound -> "not_found"
                    else -> "client_error_${error.response.status.value}"
                }
            }
            is IllegalArgumentException -> "invalid_argument"
            is IllegalStateException -> "invalid_state"
            is NullPointerException -> "null_pointer"
            else -> error.javaClass.simpleName.lowercase()
        }
    }

    /**
     * Builds a unique key for error deduplication.
     */
    private fun buildErrorKey(source: String, error: Throwable): String {
        val errorType = error.javaClass.simpleName
        val messageHash = error.message?.take(100)?.hashCode() ?: 0
        return "$source:$errorType:$messageHash"
    }

    /**
     * Sends notification to admins with debouncing.
     */
    private fun notifyAdminsDebounced(errorKey: String, messageBuilder: () -> String) {
        scope.launch {
            mutex.withLock {
                val now = Instant.now()
                val lastTime = lastNotificationTime[errorKey]

                if (lastTime != null &&
                    now.epochSecond - lastTime.epochSecond < DEBOUNCE_INTERVAL_SECONDS) {
                    logger.debug("Debouncing notification for error: $errorKey")
                    return@withLock
                }

                lastNotificationTime[errorKey] = now

                // Clean up old entries (older than 1 hour)
                val oneHourAgo = now.minusSeconds(3600)
                lastNotificationTime.entries.removeIf { it.value.isBefore(oneHourAgo) }
            }

            // Send to all configured admins
            val message = messageBuilder()
            sendToAdmins(message)
        }
    }

    /**
     * Sends a silent message to all configured admin users.
     */
    private suspend fun sendToAdmins(message: String) {
        if (adminProperties.userIds.isEmpty()) {
            logger.warn("No admin user IDs configured - cannot send error notification")
            return
        }

        for (adminId in adminProperties.userIds) {
            try {
                bot.send(
                    chatId = ChatId(RawChatId(adminId)),
                    text = message,
                    parseMode = HTMLParseMode,
                    disableNotification = true // Silent notification
                )
                logger.debug("Sent error notification to admin $adminId")
            } catch (e: Exception) {
                // Don't recurse on notification failures
                logger.warn("Failed to send error notification to admin $adminId: ${e.message}")
            }
        }
    }

    private fun buildHandlerErrorMessage(
        handler: String,
        error: Throwable,
        context: ErrorContext?
    ): String {
        val sb = StringBuilder()
        sb.append("⚠️ <b>Handler Error</b>\n\n")
        sb.append("<b>Handler:</b> <code>$handler</code>\n")
        sb.append("<b>Error:</b> <code>${error.javaClass.simpleName}</code>\n")

        context?.let {
            it.chatId?.let { chatId -> sb.append("<b>Chat:</b> <code>$chatId</code>\n") }
            it.userId?.let { userId -> sb.append("<b>User:</b> <code>$userId</code>\n") }
            it.command?.let { cmd -> sb.append("<b>Command:</b> <code>$cmd</code>\n") }
        }

        sb.append("\n<b>Message:</b>\n<pre>")
        sb.append(escapeHtml(error.message?.take(MAX_MESSAGE_LENGTH) ?: "No message"))
        sb.append("</pre>")

        return sb.toString()
    }

    private fun buildServiceErrorMessage(
        service: String,
        error: Throwable,
        context: ErrorContext?
    ): String {
        val sb = StringBuilder()
        sb.append("⚠️ <b>Service Error</b>\n\n")
        sb.append("<b>Service:</b> <code>$service</code>\n")
        sb.append("<b>Error:</b> <code>${error.javaClass.simpleName}</code>\n")

        context?.let {
            it.chatId?.let { chatId -> sb.append("<b>Chat:</b> <code>$chatId</code>\n") }
            it.userId?.let { userId -> sb.append("<b>User:</b> <code>$userId</code>\n") }
        }

        sb.append("\n<b>Message:</b>\n<pre>")
        sb.append(escapeHtml(error.message?.take(MAX_MESSAGE_LENGTH) ?: "No message"))
        sb.append("</pre>")

        return sb.toString()
    }

    private fun buildBotErrorMessage(error: Throwable): String {
        val sb = StringBuilder()
        sb.append("🔴 <b>Bot Error</b>\n\n")
        sb.append("<b>Error:</b> <code>${error.javaClass.simpleName}</code>\n")
        sb.append("\n<b>Message:</b>\n<pre>")
        sb.append(escapeHtml(error.message?.take(MAX_MESSAGE_LENGTH) ?: "No message"))
        sb.append("</pre>")

        return sb.toString()
    }

    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
    }
}

/**
 * Context information for error reporting.
 */
data class ErrorContext(
    val chatId: Long? = null,
    val userId: Long? = null,
    val command: String? = null,
    val additionalInfo: Map<String, String> = emptyMap()
)
