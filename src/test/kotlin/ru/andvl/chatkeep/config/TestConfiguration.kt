package ru.andvl.chatkeep.config

import dev.inmo.tgbotapi.bot.TelegramBot
import io.mockk.mockk
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import ru.andvl.chatkeep.api.auth.TelegramAuthService
import ru.andvl.chatkeep.api.support.TestTelegramAuthService
import ru.andvl.chatkeep.api.config.MediaUploadConfig
import ru.andvl.chatkeep.domain.service.moderation.AdminCacheService

/**
 * Base test configuration for all integration tests.
 * Provides mock beans for bot and API services.
 */
@TestConfiguration
class TestConfiguration {

    @Bean
    @Primary
    fun telegramBot(): TelegramBot = mockk(relaxed = true)

    @Bean
    @Primary
    fun mediaUploadConfig(): MediaUploadConfig = MediaUploadConfig()

    @Bean
    @Primary
    fun mockAdminCacheService(): AdminCacheService = mockk(relaxed = true)

    // Register as concrete type so ApiTestConfiguration can inject TestTelegramAuthService
    @Bean
    @Primary
    fun testTelegramAuthService(): TestTelegramAuthService = TestTelegramAuthService()
}
