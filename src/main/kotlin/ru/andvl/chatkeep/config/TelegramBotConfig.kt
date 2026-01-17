package ru.andvl.chatkeep.config

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.requests.abstracts.Request
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(AdminLogsProperties::class, AdminProperties::class, BotProperties::class, AdminBotProperties::class)
class TelegramBotConfig {
    @Bean
    @ConditionalOnProperty(name = ["telegram.bot.enabled"], havingValue = "true", matchIfMissing = true)
    fun telegramBot(
        @Value("\${telegram.bot.token}")
        token: String
    ): TelegramBot {
        return dev.inmo.tgbotapi.bot.ktor.telegramBot(token)
    }

    /**
     * No-op TelegramBot for OpenAPI schema generation when bot is disabled.
     * This bot throws UnsupportedOperationException on any API call.
     */
    @Bean
    @ConditionalOnProperty(name = ["telegram.bot.enabled"], havingValue = "false")
    fun noOpTelegramBot(): TelegramBot {
        return NoOpTelegramBot()
    }
}

/**
 * A no-op implementation of TelegramBot that throws on execute.
 * Used only for OpenAPI schema generation in CI.
 */
private class NoOpTelegramBot : TelegramBot {
    override suspend fun <T : Any> execute(request: Request<T>): T {
        throw UnsupportedOperationException("Bot is disabled in openapi profile. This should not be called during schema generation.")
    }

    override fun close() {
        // No-op
    }
}
