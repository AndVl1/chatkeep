package ru.andvl.chatkeep.infrastructure.repository

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.dao.DuplicateKeyException
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import ru.andvl.chatkeep.domain.model.ChatMessage
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Repository tests for MessageRepository.
 * Uses real PostgreSQL via Testcontainers for production parity.
 */
@DataJdbcTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class MessageRepositoryTest {

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("chatkeep_test")
            .withUsername("test")
            .withPassword("test")
    }

    @Autowired
    private lateinit var repository: MessageRepository

    private var messageIdCounter = 1L

    @BeforeEach
    fun cleanup() {
        repository.deleteAll()
        messageIdCounter = 1L
    }

    @Test
    fun `should save and retrieve message`() {
        // Given
        val message = createMessage(
            chatId = -100123L,
            userId = 1001L,
            text = "Hello World"
        )

        // When
        val saved = repository.save(message)

        // Then
        assertNotNull(saved.id, "Should have generated ID")
        assertEquals(-100123L, saved.chatId)
        assertEquals(1001L, saved.userId)
        assertEquals("Hello World", saved.text)
    }

    @Test
    fun `findByChatId should return all messages for chat`() {
        // Given
        val chatId = -100456L
        repository.save(createMessage(chatId = chatId, text = "Message 1"))
        repository.save(createMessage(chatId = chatId, text = "Message 2"))
        repository.save(createMessage(chatId = chatId, text = "Message 3"))
        repository.save(createMessage(chatId = -100999L, text = "Other chat"))

        // When
        val messages = repository.findByChatId(chatId)

        // Then
        assertEquals(3, messages.size)
        assertTrue(messages.all { it.chatId == chatId })
    }

    @Test
    fun `findByChatId should return empty list when no messages`() {
        // When
        val messages = repository.findByChatId(-999999L)

        // Then
        assertTrue(messages.isEmpty())
    }

    @Test
    fun `countByChatId should return correct count`() {
        // Given
        val chatId = -100789L
        repeat(5) {
            repository.save(createMessage(chatId = chatId, text = "Message $it"))
        }
        repository.save(createMessage(chatId = -100888L, text = "Other"))

        // When
        val count = repository.countByChatId(chatId)

        // Then
        assertEquals(5, count)
    }

    @Test
    fun `countByChatId should return zero when no messages`() {
        // When
        val count = repository.countByChatId(-999999L)

        // Then
        assertEquals(0, count)
    }

    @Test
    fun `countUniqueUsersByChatId should return distinct user count`() {
        // Given
        val chatId = -100111L
        // User 1001 sends 3 messages
        repository.save(createMessage(chatId = chatId, userId = 1001L, text = "User1 Msg1"))
        repository.save(createMessage(chatId = chatId, userId = 1001L, text = "User1 Msg2"))
        repository.save(createMessage(chatId = chatId, userId = 1001L, text = "User1 Msg3"))
        // User 1002 sends 2 messages
        repository.save(createMessage(chatId = chatId, userId = 1002L, text = "User2 Msg1"))
        repository.save(createMessage(chatId = chatId, userId = 1002L, text = "User2 Msg2"))
        // User 1003 sends 1 message
        repository.save(createMessage(chatId = chatId, userId = 1003L, text = "User3 Msg1"))

        // When
        val uniqueUsers = repository.countUniqueUsersByChatId(chatId)

        // Then
        assertEquals(3, uniqueUsers)
    }

    @Test
    fun `countUniqueUsersByChatId should return zero when no messages`() {
        // When
        val uniqueUsers = repository.countUniqueUsersByChatId(-999999L)

        // Then
        assertEquals(0, uniqueUsers)
    }

    @Test
    fun `existsByChatIdAndTelegramMessageId should return true when exists`() {
        // Given
        val chatId = -100222L
        val telegramMessageId = 54321L
        repository.save(createMessage(chatId = chatId, telegramMessageId = telegramMessageId))

        // When/Then
        assertTrue(repository.existsByChatIdAndTelegramMessageId(chatId, telegramMessageId))
    }

    @Test
    fun `existsByChatIdAndTelegramMessageId should return false when not exists`() {
        // When/Then
        assertFalse(repository.existsByChatIdAndTelegramMessageId(-100333L, 99999L))
    }

    @Test
    fun `existsByChatIdAndTelegramMessageId should distinguish different chats`() {
        // Given
        val telegramMessageId = 12345L
        repository.save(createMessage(chatId = -100444L, telegramMessageId = telegramMessageId))

        // When/Then
        assertTrue(repository.existsByChatIdAndTelegramMessageId(-100444L, telegramMessageId))
        assertFalse(repository.existsByChatIdAndTelegramMessageId(-100555L, telegramMessageId))
    }

    @Test
    fun `should enforce unique constraint on chat_id and telegram_message_id`() {
        // Given
        val chatId = -100666L
        val telegramMessageId = 77777L
        repository.save(createMessage(chatId = chatId, telegramMessageId = telegramMessageId))

        // When/Then
        assertThrows<DuplicateKeyException> {
            repository.save(createMessage(chatId = chatId, telegramMessageId = telegramMessageId, text = "Duplicate"))
        }
    }

    @Test
    fun `should allow same telegram_message_id in different chats`() {
        // Given
        val telegramMessageId = 88888L

        // When
        val msg1 = repository.save(createMessage(chatId = -100777L, telegramMessageId = telegramMessageId))
        val msg2 = repository.save(createMessage(chatId = -100888L, telegramMessageId = telegramMessageId))

        // Then
        assertNotNull(msg1.id)
        assertNotNull(msg2.id)
        assertTrue(msg1.id != msg2.id)
    }

    @Test
    fun `should handle optional user fields`() {
        // Given
        val message = ChatMessage(
            telegramMessageId = nextTelegramMessageId(),
            chatId = -100999L,
            userId = 1001L,
            username = null,
            firstName = null,
            lastName = null,
            text = "Anonymous message",
            messageDate = Instant.now()
        )

        // When
        val saved = repository.save(message)
        val found = repository.findById(saved.id!!)

        // Then
        assertTrue(found.isPresent)
        val retrieved = found.get()
        assertNotNull(retrieved.id)
        assertEquals("Anonymous message", retrieved.text)
    }

    @Test
    fun `should preserve message text with special characters`() {
        // Given
        val specialText = "Hello! \n\t Emoji: \uD83D\uDE00 Special: <>&\"' Unicode: Привет 你好"
        val message = createMessage(chatId = -100111L, text = specialText)

        // When
        val saved = repository.save(message)
        val found = repository.findById(saved.id!!)

        // Then
        assertTrue(found.isPresent)
        assertEquals(specialText, found.get().text)
    }

    private fun nextTelegramMessageId(): Long = messageIdCounter++

    private fun createMessage(
        chatId: Long,
        userId: Long = 1000L,
        telegramMessageId: Long = nextTelegramMessageId(),
        text: String = "Test message"
    ) = ChatMessage(
        telegramMessageId = telegramMessageId,
        chatId = chatId,
        userId = userId,
        username = "testuser",
        firstName = "Test",
        lastName = "User",
        text = text,
        messageDate = Instant.now()
    )
}
