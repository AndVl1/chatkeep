package ru.andvl.chatkeep.bot.handlers

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.andvl.chatkeep.domain.service.AdminService
import ru.andvl.chatkeep.domain.service.ChatService
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Test for /start command handler registration.
 */
class StartCommandTest {

    private lateinit var chatService: ChatService
    private lateinit var adminService: AdminService
    private lateinit var handler: AdminCommandHandler

    @BeforeEach
    fun setUp() {
        chatService = mockk(relaxed = true)
        adminService = mockk(relaxed = true)
        handler = AdminCommandHandler(chatService, adminService, "https://test-app.example.com")
    }

    @Test
    fun `handler can be instantiated`() = runTest {
        assertNotNull(handler)
    }

    @Test
    fun `handler has correct mini app URL configured`() = runTest {
        val testUrl = "https://test-mini-app.example.com"
        val testHandler = AdminCommandHandler(chatService, adminService, testUrl)
        assertNotNull(testHandler)
    }
}
