package ru.andvl.chatkeep.api.config

import io.mockk.mockk
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import ru.andvl.chatkeep.api.auth.TelegramAuthService
import ru.andvl.chatkeep.api.service.MediaUploadService
import ru.andvl.chatkeep.api.support.AuthTestHelper
import ru.andvl.chatkeep.api.support.CapturingLogChannelPort
import ru.andvl.chatkeep.api.support.TestDataFactory
import ru.andvl.chatkeep.api.support.TestTelegramAuthService
import ru.andvl.chatkeep.domain.service.media.MediaStorageService

/**
 * API-specific test configuration.
 * Extends base TestConfiguration with API test helpers.
 */
@TestConfiguration
class ApiTestConfiguration {

    @Bean("logChannelPort")
    @Primary
    fun capturingLogChannelPort(): CapturingLogChannelPort = CapturingLogChannelPort()

    @Bean
    fun authTestHelper(telegramAuthService: TestTelegramAuthService): AuthTestHelper =
        AuthTestHelper(telegramAuthService)

    @Bean
    fun testDataFactory(): TestDataFactory = TestDataFactory()

    @Bean
    @Primary
    fun mediaUploadService(): MediaUploadService = mockk(relaxed = true)

    @Bean
    @Primary
    fun mediaStorageService(): MediaStorageService = mockk(relaxed = true)
}
