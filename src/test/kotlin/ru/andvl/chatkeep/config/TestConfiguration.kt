package ru.andvl.chatkeep.config

import dev.inmo.tgbotapi.bot.TelegramBot
import io.mockk.mockk
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class TestConfiguration {

    @Bean
    @Primary
    fun telegramBot(): TelegramBot = mockk(relaxed = true)
}
