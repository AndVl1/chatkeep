package ru.andvl.chatkeep.domain.service.moderation

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.andvl.chatkeep.domain.model.moderation.AdminSession
import ru.andvl.chatkeep.infrastructure.repository.moderation.AdminSessionRepository
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Unit tests for AdminSessionService.
 * CRITICAL: Verifies session header formatting with **[Chat Name]** prefix.
 */
class AdminSessionServiceTest {

    private lateinit var repository: AdminSessionRepository
    private lateinit var service: AdminSessionService

    @BeforeEach
    fun setup() {
        repository = mockk()
        service = AdminSessionService(repository)
    }

    // SESSION CREATION TESTS

    @Test
    fun `connect should create new session`() {
        // Given
        val userId = 123L
        val chatId = -456L
        val chatTitle = "Test Group"

        val sessionSlot = slot<AdminSession>()
        every { repository.deleteByUserId(userId) } returns Unit
        every { repository.save(capture(sessionSlot)) } answers { firstArg() }

        // When
        val result = service.connect(userId, chatId, chatTitle)

        // Then
        assertNotNull(result)
        verify { repository.deleteByUserId(userId) }
        verify { repository.save(any()) }

        val savedSession = sessionSlot.captured
        assertEquals(userId, savedSession.userId)
        assertEquals(chatId, savedSession.connectedChatId)
        assertEquals(chatTitle, savedSession.connectedChatTitle)
    }

    @Test
    fun `connect should delete existing session before creating new one`() {
        // Given
        val userId = 123L

        every { repository.deleteByUserId(userId) } returns Unit
        every { repository.save(any()) } answers { firstArg<AdminSession>() }

        // When
        service.connect(userId, -456L, "Chat 1")
        service.connect(userId, -789L, "Chat 2")

        // Then
        verify(exactly = 2) { repository.deleteByUserId(userId) }
    }

    @Test
    fun `connect should handle null chat title`() {
        // Given
        val userId = 123L
        val chatId = -456L

        val sessionSlot = slot<AdminSession>()
        every { repository.deleteByUserId(userId) } returns Unit
        every { repository.save(capture(sessionSlot)) } answers { firstArg() }

        // When
        val result = service.connect(userId, chatId, null)

        // Then
        assertNotNull(result)
        assertNull(sessionSlot.captured.connectedChatTitle)
    }

    @Test
    fun `connect should preserve exact chat title`() {
        // Given
        val userId = 123L
        val chatId = -456L
        val chatTitle = "Special Chars: <>&\"' Русский 中文"

        val sessionSlot = slot<AdminSession>()
        every { repository.deleteByUserId(userId) } returns Unit
        every { repository.save(capture(sessionSlot)) } answers { firstArg() }

        // When
        service.connect(userId, chatId, chatTitle)

        // Then
        assertEquals(chatTitle, sessionSlot.captured.connectedChatTitle)
    }

    // SESSION DISCONNECTION TESTS

    @Test
    fun `disconnect should delete session by userId`() {
        // Given
        val userId = 123L

        every { repository.deleteByUserId(userId) } returns Unit

        // When
        service.disconnect(userId)

        // Then
        verify { repository.deleteByUserId(userId) }
    }

    // SESSION RETRIEVAL TESTS

    @Test
    fun `getSession should return session when exists`() {
        // Given
        val userId = 123L
        val session = createSession(userId, -456L, "Test Chat")

        every { repository.findByUserId(userId) } returns session

        // When
        val result = service.getSession(userId)

        // Then
        assertNotNull(result)
        assertEquals(userId, result.userId)
        assertEquals("Test Chat", result.connectedChatTitle)
    }

    @Test
    fun `getSession should return null when no session exists`() {
        // Given
        val userId = 123L

        every { repository.findByUserId(userId) } returns null

        // When
        val result = service.getSession(userId)

        // Then
        assertNull(result)
    }

    // REPLY PREFIX FORMATTING TESTS (CRITICAL)

    @Test
    fun `formatReplyPrefix should format with chat title in double asterisks and brackets`() {
        // Given
        val session = createSession(123L, -456L, "My Super Group")

        // When
        val prefix = service.formatReplyPrefix(session)

        // Then
        assertEquals("**[My Super Group]**", prefix)
    }

    @Test
    fun `formatReplyPrefix should use fallback format when title is null`() {
        // Given
        val session = createSession(123L, -456L, null)

        // When
        val prefix = service.formatReplyPrefix(session)

        // Then
        assertEquals("**[Chat -456]**", prefix)
    }

    @Test
    fun `formatReplyPrefix should preserve special characters in title`() {
        // Given
        val session = createSession(123L, -456L, "Test & Development")

        // When
        val prefix = service.formatReplyPrefix(session)

        // Then
        assertEquals("**[Test & Development]**", prefix)
    }

    @Test
    fun `formatReplyPrefix should handle Unicode characters`() {
        // Given
        val session = createSession(123L, -456L, "Группа Разработки 开发组")

        // When
        val prefix = service.formatReplyPrefix(session)

        // Then
        assertEquals("**[Группа Разработки 开发组]**", prefix)
    }

    @Test
    fun `formatReplyPrefix should handle emoji in title`() {
        // Given
        val session = createSession(123L, -456L, "\uD83D\uDE80 Dev Team")

        // When
        val prefix = service.formatReplyPrefix(session)

        // Then
        assertEquals("**[\uD83D\uDE80 Dev Team]**", prefix)
    }

    @Test
    fun `formatReplyPrefix should handle empty title`() {
        // Given
        val session = createSession(123L, -456L, "")

        // When
        val prefix = service.formatReplyPrefix(session)

        // Then
        assertEquals("**[]**", prefix)
    }

    @Test
    fun `formatReplyPrefix should handle whitespace-only title`() {
        // Given
        val session = createSession(123L, -456L, "   ")

        // When
        val prefix = service.formatReplyPrefix(session)

        // Then
        assertEquals("**[   ]**", prefix)
    }

    @Test
    fun `formatReplyPrefix should use Chat ID format for null title`() {
        // Given
        val session1 = createSession(123L, -100L, null)
        val session2 = createSession(123L, -999999L, null)

        // When
        val prefix1 = service.formatReplyPrefix(session1)
        val prefix2 = service.formatReplyPrefix(session2)

        // Then
        assertEquals("**[Chat -100]**", prefix1)
        assertEquals("**[Chat -999999]**", prefix2)
    }

    @Test
    fun `formatReplyPrefix should handle very long chat titles`() {
        // Given
        val longTitle = "A".repeat(200)
        val session = createSession(123L, -456L, longTitle)

        // When
        val prefix = service.formatReplyPrefix(session)

        // Then
        assertEquals("**[$longTitle]**", prefix)
    }

    // INTEGRATION SCENARIOS

    @Test
    fun `connect and formatReplyPrefix integration`() {
        // Given
        val userId = 123L
        val chatId = -456L
        val chatTitle = "Support Team"

        every { repository.deleteByUserId(userId) } returns Unit
        every { repository.save(any()) } answers { firstArg<AdminSession>() }

        // When
        val session = service.connect(userId, chatId, chatTitle)
        val prefix = service.formatReplyPrefix(session)

        // Then
        assertEquals("**[Support Team]**", prefix)
    }

    @Test
    fun `connect with null title and formatReplyPrefix integration`() {
        // Given
        val userId = 123L
        val chatId = -789L

        every { repository.deleteByUserId(userId) } returns Unit
        every { repository.save(any()) } answers { firstArg<AdminSession>() }

        // When
        val session = service.connect(userId, chatId, null)
        val prefix = service.formatReplyPrefix(session)

        // Then
        assertEquals("**[Chat -789]**", prefix)
    }

    @Test
    fun `multiple users can have active sessions simultaneously`() {
        // Given
        val user1 = 111L
        val user2 = 222L
        val chat1 = -100L
        val chat2 = -200L

        every { repository.deleteByUserId(any()) } returns Unit
        every { repository.save(any()) } answers { firstArg<AdminSession>() }

        // When
        val session1 = service.connect(user1, chat1, "Chat 1")
        val session2 = service.connect(user2, chat2, "Chat 2")

        // Then
        assertEquals("**[Chat 1]**", service.formatReplyPrefix(session1))
        assertEquals("**[Chat 2]**", service.formatReplyPrefix(session2))
    }

    @Test
    fun `user can switch between chats`() {
        // Given
        val userId = 123L

        every { repository.deleteByUserId(userId) } returns Unit
        every { repository.save(any()) } answers { firstArg<AdminSession>() }

        // When
        val session1 = service.connect(userId, -100L, "Chat 1")
        val prefix1 = service.formatReplyPrefix(session1)

        val session2 = service.connect(userId, -200L, "Chat 2")
        val prefix2 = service.formatReplyPrefix(session2)

        // Then
        assertEquals("**[Chat 1]**", prefix1)
        assertEquals("**[Chat 2]**", prefix2)
        verify(exactly = 2) { repository.deleteByUserId(userId) }
    }

    // Helper function
    private fun createSession(
        userId: Long,
        connectedChatId: Long,
        connectedChatTitle: String?
    ) = AdminSession(
        id = null,
        userId = userId,
        connectedChatId = connectedChatId,
        connectedChatTitle = connectedChatTitle,
        createdAt = Instant.now()
    )
}
