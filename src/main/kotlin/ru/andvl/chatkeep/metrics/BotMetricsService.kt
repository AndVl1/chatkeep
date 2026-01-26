package ru.andvl.chatkeep.metrics

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

@Service
class BotMetricsService(
    private val meterRegistry: MeterRegistry
) {

    companion object {
        private const val PREFIX = "chatkeep.bot"
    }

    private val activeChatsCount = AtomicLong(0)

    init {
        Gauge.builder("$PREFIX.chats.active") { activeChatsCount.get().toDouble() }
            .register(meterRegistry)
    }

    // Messages
    fun recordMessageReceived(chatType: String) {
        Counter.builder("$PREFIX.messages.received")
            .tag("chat_type", chatType)
            .register(meterRegistry)
            .increment()
    }

    fun recordMessageSaved(success: Boolean) {
        Counter.builder("$PREFIX.messages.saved")
            .tag("status", if (success) "success" else "failure")
            .register(meterRegistry)
            .increment()
    }

    fun recordMessageProcessingTime(handler: String, durationMs: Long) {
        Timer.builder("$PREFIX.message.processing.time")
            .tag("handler", handler)
            .register(meterRegistry)
            .record(durationMs, TimeUnit.MILLISECONDS)
    }

    // Moderation
    fun recordWarningIssued(reason: String) {
        Counter.builder("$PREFIX.warnings.issued")
            .tag("reason", reason)
            .register(meterRegistry)
            .increment()
    }

    fun recordPunishmentExecuted(type: String) {
        Counter.builder("$PREFIX.punishments.executed")
            .tag("type", type)
            .register(meterRegistry)
            .increment()
    }

    fun recordBlocklistMatch() {
        Counter.builder("$PREFIX.blocklist.matches")
            .register(meterRegistry)
            .increment()
    }

    fun recordLockViolation(lockType: String) {
        Counter.builder("$PREFIX.lock.violations")
            .tag("lock_type", lockType)
            .register(meterRegistry)
            .increment()
    }

    // Commands
    fun recordAdminCommand(command: String) {
        Counter.builder("$PREFIX.commands.admin")
            .tag("command", command)
            .register(meterRegistry)
            .increment()
    }

    fun recordModerationCommand(command: String) {
        Counter.builder("$PREFIX.commands.moderation")
            .tag("command", command)
            .register(meterRegistry)
            .increment()
    }

    // Errors
    fun recordHandlerError(handler: String) {
        Counter.builder("$PREFIX.handler.errors")
            .tag("handler", handler)
            .register(meterRegistry)
            .increment()
    }

    fun recordServiceError(service: String) {
        Counter.builder("$PREFIX.service.errors")
            .tag("service", service)
            .register(meterRegistry)
            .increment()
    }

    /**
     * Records detailed error metrics for Grafana dashboards.
     *
     * @param source Where the error occurred: "handler", "service", or "bot"
     * @param name Specific handler/service name
     * @param errorType Classification of the error (timeout, rate_limit, bad_request, etc.)
     * @param isExpected Whether this is an expected error (timeouts, rate limits)
     */
    fun recordErrorWithDetails(
        source: String,
        name: String,
        errorType: String,
        isExpected: Boolean
    ) {
        Counter.builder("$PREFIX.errors.detailed")
            .tag("source", source)
            .tag("name", name)
            .tag("error_type", errorType)
            .tag("expected", isExpected.toString())
            .register(meterRegistry)
            .increment()
    }

    // Gauge for active chats
    fun setActiveChats(count: Long) {
        activeChatsCount.set(count)
    }
}
