package ru.andvl.chatkeep.api

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.andvl.chatkeep.api.auth.TelegramAuthService
import ru.andvl.chatkeep.api.support.TestTelegramAuthService

class TestServiceType : MiniAppApiTestBase() {

    @Autowired
    lateinit var telegramAuthService: TelegramAuthService

    @Test
    fun `verify test service is used`() {
        println("Service class: ${telegramAuthService::class.java}")
        println("Is TestTelegramAuthService: ${telegramAuthService is TestTelegramAuthService}")
        assert(telegramAuthService is TestTelegramAuthService) {
            "Expected TestTelegramAuthService but got ${telegramAuthService::class.java}"
        }
    }
}
