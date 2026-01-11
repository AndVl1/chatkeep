package ru.andvl.chatkeep.config

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(AdminLogsProperties::class, AdminProperties::class, BotProperties::class)
class TelegramBotConfig {
    @Bean
    fun telegramBot(
        @Value("\${telegram.bot.token}")
        token: String
    ): TelegramBot {
        return dev.inmo.tgbotapi.bot.ktor.telegramBot(token)
    }
}
