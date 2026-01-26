package ru.andvl.chatkeep.bot

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.http.HttpStatusCode
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.bot.handlers.Handler
import ru.andvl.chatkeep.bot.service.AdminErrorNotificationService
import java.net.ConnectException
import java.net.SocketTimeoutException

@Component
@ConditionalOnProperty(name = ["telegram.bot.enabled"], havingValue = "true", matchIfMissing = true)
class ChatkeepBot(
    private val bot: TelegramBot,
    private val handlers: List<Handler>,
    private val errorNotificationService: AdminErrorNotificationService
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var botJob: Job? = null

    @PostConstruct
    fun start() {
        logger.info("Starting Chatkeep bot...")

        botJob = scope.launch {
            try {
                bot.buildBehaviourWithLongPolling(
                    defaultExceptionsHandler = { exception ->
                        when {
                            // Network timeouts (normal for long polling)
                            exception is HttpRequestTimeoutException ||
                            exception is SocketTimeoutException ||
                            exception is ConnectException -> {
                                logger.debug("Telegram long polling timeout (normal - waiting for updates): ${exception.javaClass.simpleName}")
                            }

                            // Telegram API rate limit (429) - expected, don't notify
                            exception is ClientRequestException &&
                            exception.response.status == HttpStatusCode.TooManyRequests -> {
                                logger.warn("Telegram API rate limit hit: ${exception.message}")
                            }

                            // Genuine error - log and notify admins
                            else -> {
                                logger.error("Bot error: ${exception.message}", exception)
                                errorNotificationService.reportBotError(exception)
                            }
                        }
                    }
                ) {
                    handlers.forEach { handler ->
                        with(handler) { register() }
                    }
                    logger.info("Chatkeep bot started successfully with ${handlers.size} handlers")
                }.join()
            } catch (e: Exception) {
                logger.error("Bot failed to start: ${e.message}", e)
                errorNotificationService.reportBotError(e)
            }
        }
    }

    @PreDestroy
    fun stop() {
        logger.info("Stopping Chatkeep bot...")
        botJob?.cancel()
        scope.cancel()
    }
}
