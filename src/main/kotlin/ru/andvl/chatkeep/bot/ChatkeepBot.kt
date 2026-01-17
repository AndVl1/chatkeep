package ru.andvl.chatkeep.bot

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.bot.handlers.Handler
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel

@Component
@ConditionalOnProperty(name = ["telegram.bot.enabled"], havingValue = "true", matchIfMissing = true)
class ChatkeepBot(
    private val bot: TelegramBot,
    private val handlers: List<Handler>
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var botJob: Job? = null

    @PostConstruct
    fun start() {
        logger.info("Starting Chatkeep bot...")

        botJob = scope.launch {
            try {
                bot.buildBehaviourWithLongPolling {
                    handlers.forEach { handler ->
                        with(handler) { register() }
                    }
                    logger.info("Chatkeep bot started successfully with ${handlers.size} handlers")
                }.join()
            } catch (e: Exception) {
                logger.error("Bot failed to start: ${e.message}", e)
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
