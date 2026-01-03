package ru.andvl.chatkeep.domain.service.channelreply

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import ru.andvl.chatkeep.domain.model.channelreply.ChannelReplySettings
import ru.andvl.chatkeep.domain.model.channelreply.MediaType
import ru.andvl.chatkeep.domain.model.channelreply.ReplyButton
import ru.andvl.chatkeep.infrastructure.repository.channelreply.ChannelReplySettingsRepository
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for ChannelReplyService.
 *
 * Tests JSON serialization/deserialization of buttons and
 * all CRUD operations for channel reply settings.
 */
class ChannelReplyServiceTest {

    private lateinit var repository: ChannelReplySettingsRepository
    private lateinit var objectMapper: ObjectMapper
    private lateinit var service: ChannelReplyService

    @BeforeEach
    fun setup() {
        repository = mockk()
        objectMapper = ObjectMapper()
        service = ChannelReplyService(repository, objectMapper)
    }

    @Nested
    inner class ParseButtonsTests {

        @Test
        fun `parseButtons with valid JSON returns list`() {
            // Given
            val json = """[{"text":"Button 1","url":"https://example.com"},{"text":"Button 2","url":"https://test.com"}]"""

            // When
            val result = service.parseButtons(json)

            // Then
            assertEquals(2, result.size)
            assertEquals("Button 1", result[0].text)
            assertEquals("https://example.com", result[0].url)
            assertEquals("Button 2", result[1].text)
            assertEquals("https://test.com", result[1].url)
        }

        @Test
        fun `parseButtons with empty JSON array returns empty list`() {
            // Given
            val json = "[]"

            // When
            val result = service.parseButtons(json)

            // Then
            assertTrue(result.isEmpty())
        }

        @Test
        fun `parseButtons with null returns empty list`() {
            // Given
            val json: String? = null

            // When
            val result = service.parseButtons(json)

            // Then
            assertTrue(result.isEmpty())
        }

        @Test
        fun `parseButtons with blank string returns empty list`() {
            // Given
            val json = "   "

            // When
            val result = service.parseButtons(json)

            // Then
            assertTrue(result.isEmpty())
        }

        @Test
        fun `parseButtons with empty string returns empty list`() {
            // Given
            val json = ""

            // When
            val result = service.parseButtons(json)

            // Then
            assertTrue(result.isEmpty())
        }

        @Test
        fun `parseButtons with invalid JSON returns empty list`() {
            // Given
            val json = "{invalid json"

            // When
            val result = service.parseButtons(json)

            // Then
            assertTrue(result.isEmpty())
        }

        @Test
        fun `parseButtons with malformed JSON returns empty list`() {
            // Given
            val json = """[{"text":"Button"}]""" // Missing url field

            // When
            val result = service.parseButtons(json)

            // Then - Jackson will fail to deserialize, should return empty list
            assertTrue(result.isEmpty())
        }

        @Test
        fun `parseButtons with single button returns list with one item`() {
            // Given
            val json = """[{"text":"Single","url":"https://single.com"}]"""

            // When
            val result = service.parseButtons(json)

            // Then
            assertEquals(1, result.size)
            assertEquals("Single", result[0].text)
            assertEquals("https://single.com", result[0].url)
        }

        @Test
        fun `parseButtons with unicode characters works correctly`() {
            // Given
            val json = """[{"text":"Кнопка","url":"https://example.com"}]"""

            // When
            val result = service.parseButtons(json)

            // Then
            assertEquals(1, result.size)
            assertEquals("Кнопка", result[0].text)
        }

        @Test
        fun `parseButtons with emoji in text works correctly`() {
            // Given
            val json = """[{"text":"Click 🔥","url":"https://example.com"}]"""

            // When
            val result = service.parseButtons(json)

            // Then
            assertEquals(1, result.size)
            assertEquals("Click 🔥", result[0].text)
        }
    }

    @Nested
    inner class SerializeButtonsTests {

        @Test
        fun `setButtons with empty list stores null`() {
            // Given
            val chatId = 123L
            val buttons = emptyList<ReplyButton>()

            every { repository.findByChatId(chatId) } returns null

            val settingsSlot = slot<ChannelReplySettings>()
            every { repository.save(capture(settingsSlot)) } answers { firstArg() }

            // When
            service.setButtons(chatId, buttons)

            // Then
            verify { repository.save(any()) }
            val saved = settingsSlot.captured
            // Empty list should be serialized to "[]"
            assertEquals("[]", saved.buttonsJson)
        }

        @Test
        fun `setButtons with valid list stores JSON`() {
            // Given
            val chatId = 123L
            val buttons = listOf(
                ReplyButton("Button 1", "https://example.com"),
                ReplyButton("Button 2", "https://test.com")
            )

            every { repository.findByChatId(chatId) } returns null

            val settingsSlot = slot<ChannelReplySettings>()
            every { repository.save(capture(settingsSlot)) } answers { firstArg() }

            // When
            service.setButtons(chatId, buttons)

            // Then
            verify { repository.save(any()) }
            val saved = settingsSlot.captured
            assertNotNull(saved.buttonsJson)

            // Verify JSON can be parsed back
            val parsed = service.parseButtons(saved.buttonsJson)
            assertEquals(2, parsed.size)
            assertEquals("Button 1", parsed[0].text)
            assertEquals("https://example.com", parsed[0].url)
        }

        @Test
        fun `clearButtons sets buttonsJson to null`() {
            // Given
            val chatId = 123L
            val existing = ChannelReplySettings(
                id = 1,
                chatId = chatId,
                buttonsJson = """[{"text":"Old","url":"https://old.com"}]"""
            )

            every { repository.findByChatId(chatId) } returns existing

            val settingsSlot = slot<ChannelReplySettings>()
            every { repository.save(capture(settingsSlot)) } answers { firstArg() }

            // When
            service.clearButtons(chatId)

            // Then
            verify { repository.save(any()) }
            val saved = settingsSlot.captured
            assertNull(saved.buttonsJson)
        }

        @Test
        fun `clearButtons with non-existent chat does nothing`() {
            // Given
            val chatId = 999L

            every { repository.findByChatId(chatId) } returns null

            // When
            service.clearButtons(chatId)

            // Then
            verify(exactly = 0) { repository.save(any()) }
        }
    }

    @Nested
    inner class GetSettingsTests {

        @Test
        fun `getSettings with existing chat returns settings`() {
            // Given
            val chatId = 123L
            val settings = ChannelReplySettings(
                id = 1,
                chatId = chatId,
                replyText = "Test reply",
                enabled = true
            )

            every { repository.findByChatId(chatId) } returns settings

            // When
            val result = service.getSettings(chatId)

            // Then
            assertNotNull(result)
            assertEquals(chatId, result.chatId)
            assertEquals("Test reply", result.replyText)
            assertTrue(result.enabled)
        }

        @Test
        fun `getSettings with non-existent chat returns null`() {
            // Given
            val chatId = 999L

            every { repository.findByChatId(chatId) } returns null

            // When
            val result = service.getSettings(chatId)

            // Then
            assertNull(result)
        }
    }

    @Nested
    inner class SetTextTests {

        @Test
        fun `setText creates new settings if not exists`() {
            // Given
            val chatId = 123L
            val text = "Welcome message"

            every { repository.findByChatId(chatId) } returns null

            val settingsSlot = slot<ChannelReplySettings>()
            every { repository.save(capture(settingsSlot)) } answers { firstArg() }

            // When
            service.setText(chatId, text)

            // Then
            verify { repository.save(any()) }
            val saved = settingsSlot.captured
            assertEquals(chatId, saved.chatId)
            assertEquals(text, saved.replyText)
            assertNotNull(saved.updatedAt)
        }

        @Test
        fun `setText updates existing settings`() {
            // Given
            val chatId = 123L
            val newText = "Updated message"
            val existing = ChannelReplySettings(
                id = 1,
                chatId = chatId,
                replyText = "Old message",
                createdAt = Instant.now().minusSeconds(3600)
            )

            every { repository.findByChatId(chatId) } returns existing

            val settingsSlot = slot<ChannelReplySettings>()
            every { repository.save(capture(settingsSlot)) } answers { firstArg() }

            // When
            val before = Instant.now()
            service.setText(chatId, newText)
            val after = Instant.now()

            // Then
            verify { repository.save(any()) }
            val saved = settingsSlot.captured
            assertEquals(chatId, saved.chatId)
            assertEquals(newText, saved.replyText)

            // Verify updatedAt was updated
            assertTrue(saved.updatedAt.isAfter(before.minusSeconds(1)))
            assertTrue(saved.updatedAt.isBefore(after.plusSeconds(1)))
        }

        @Test
        fun `setText with empty string is allowed`() {
            // Given
            val chatId = 123L
            val text = ""

            every { repository.findByChatId(chatId) } returns null

            val settingsSlot = slot<ChannelReplySettings>()
            every { repository.save(capture(settingsSlot)) } answers { firstArg() }

            // When
            service.setText(chatId, text)

            // Then
            verify { repository.save(any()) }
            val saved = settingsSlot.captured
            assertEquals("", saved.replyText)
        }

        @Test
        fun `setText with unicode characters works`() {
            // Given
            val chatId = 123L
            val text = "Привет! 🔥"

            every { repository.findByChatId(chatId) } returns null

            val settingsSlot = slot<ChannelReplySettings>()
            every { repository.save(capture(settingsSlot)) } answers { firstArg() }

            // When
            service.setText(chatId, text)

            // Then
            verify { repository.save(any()) }
            val saved = settingsSlot.captured
            assertEquals(text, saved.replyText)
        }
    }

    @Nested
    inner class SetEnabledTests {

        @Test
        fun `setEnabled creates new settings if not exists`() {
            // Given
            val chatId = 123L
            val enabled = true

            every { repository.findByChatId(chatId) } returns null

            val settingsSlot = slot<ChannelReplySettings>()
            every { repository.save(capture(settingsSlot)) } answers { firstArg() }

            // When
            service.setEnabled(chatId, enabled)

            // Then
            verify { repository.save(any()) }
            val saved = settingsSlot.captured
            assertEquals(chatId, saved.chatId)
            assertTrue(saved.enabled)
        }

        @Test
        fun `setEnabled updates existing settings to true`() {
            // Given
            val chatId = 123L
            val existing = ChannelReplySettings(
                id = 1,
                chatId = chatId,
                enabled = false
            )

            every { repository.findByChatId(chatId) } returns existing

            val settingsSlot = slot<ChannelReplySettings>()
            every { repository.save(capture(settingsSlot)) } answers { firstArg() }

            // When
            service.setEnabled(chatId, true)

            // Then
            verify { repository.save(any()) }
            val saved = settingsSlot.captured
            assertTrue(saved.enabled)
        }

        @Test
        fun `setEnabled updates existing settings to false`() {
            // Given
            val chatId = 123L
            val existing = ChannelReplySettings(
                id = 1,
                chatId = chatId,
                enabled = true
            )

            every { repository.findByChatId(chatId) } returns existing

            val settingsSlot = slot<ChannelReplySettings>()
            every { repository.save(capture(settingsSlot)) } answers { firstArg() }

            // When
            service.setEnabled(chatId, false)

            // Then
            verify { repository.save(any()) }
            val saved = settingsSlot.captured
            assertNotNull(saved)
            assertEquals(false, saved.enabled)
        }
    }

    @Nested
    inner class SetMediaTests {

        @Test
        fun `setMedia creates new settings with photo`() {
            // Given
            val chatId = 123L
            val fileId = "photo_file_123"
            val type = MediaType.PHOTO

            every { repository.findByChatId(chatId) } returns null

            val settingsSlot = slot<ChannelReplySettings>()
            every { repository.save(capture(settingsSlot)) } answers { firstArg() }

            // When
            service.setMedia(chatId, fileId, type)

            // Then
            verify { repository.save(any()) }
            val saved = settingsSlot.captured
            assertEquals(fileId, saved.mediaFileId)
            assertEquals("PHOTO", saved.mediaType)
        }

        @Test
        fun `setMedia supports all MediaType values`() {
            // Given
            val chatId = 123L

            every { repository.findByChatId(chatId) } returns null
            every { repository.save(any()) } answers { firstArg() }

            // When/Then - test all media types
            MediaType.values().forEach { type ->
                val settingsSlot = slot<ChannelReplySettings>()
                every { repository.save(capture(settingsSlot)) } answers { firstArg() }

                service.setMedia(chatId, "file_$type", type)

                val saved = settingsSlot.captured
                assertEquals(type.name, saved.mediaType)
            }
        }

        @Test
        fun `clearMedia removes media fields`() {
            // Given
            val chatId = 123L
            val existing = ChannelReplySettings(
                id = 1,
                chatId = chatId,
                mediaFileId = "old_file_123",
                mediaType = "PHOTO"
            )

            every { repository.findByChatId(chatId) } returns existing

            val settingsSlot = slot<ChannelReplySettings>()
            every { repository.save(capture(settingsSlot)) } answers { firstArg() }

            // When
            service.clearMedia(chatId)

            // Then
            verify { repository.save(any()) }
            val saved = settingsSlot.captured
            assertNull(saved.mediaFileId)
            assertNull(saved.mediaType)
        }

        @Test
        fun `clearMedia with non-existent chat does nothing`() {
            // Given
            val chatId = 999L

            every { repository.findByChatId(chatId) } returns null

            // When
            service.clearMedia(chatId)

            // Then
            verify(exactly = 0) { repository.save(any()) }
        }
    }

    @Nested
    inner class DeleteSettingsTests {

        @Test
        fun `deleteSettings calls repository delete`() {
            // Given
            val chatId = 123L

            justRun { repository.deleteByChatId(chatId) }

            // When
            service.deleteSettings(chatId)

            // Then
            verify { repository.deleteByChatId(chatId) }
        }
    }

    @Nested
    inner class EdgeCaseTests {

        @Test
        fun `setText preserves other fields`() {
            // Given
            val chatId = 123L
            val existing = ChannelReplySettings(
                id = 1,
                chatId = chatId,
                replyText = "Old",
                enabled = true,
                mediaFileId = "file_123",
                mediaType = "PHOTO",
                buttonsJson = """[{"text":"Button","url":"https://test.com"}]"""
            )

            every { repository.findByChatId(chatId) } returns existing

            val settingsSlot = slot<ChannelReplySettings>()
            every { repository.save(capture(settingsSlot)) } answers { firstArg() }

            // When
            service.setText(chatId, "New text")

            // Then
            val saved = settingsSlot.captured
            assertEquals("New text", saved.replyText)
            // Other fields should be preserved
            assertTrue(saved.enabled)
            assertEquals("file_123", saved.mediaFileId)
            assertEquals("PHOTO", saved.mediaType)
            assertEquals(existing.buttonsJson, saved.buttonsJson)
        }

        @Test
        fun `setMedia preserves other fields`() {
            // Given
            val chatId = 123L
            val existing = ChannelReplySettings(
                id = 1,
                chatId = chatId,
                replyText = "Text",
                enabled = false,
                buttonsJson = """[{"text":"Button","url":"https://test.com"}]"""
            )

            every { repository.findByChatId(chatId) } returns existing

            val settingsSlot = slot<ChannelReplySettings>()
            every { repository.save(capture(settingsSlot)) } answers { firstArg() }

            // When
            service.setMedia(chatId, "new_file", MediaType.VIDEO)

            // Then
            val saved = settingsSlot.captured
            assertEquals("new_file", saved.mediaFileId)
            assertEquals("VIDEO", saved.mediaType)
            // Other fields should be preserved
            assertEquals("Text", saved.replyText)
            assertEquals(false, saved.enabled)
            assertEquals(existing.buttonsJson, saved.buttonsJson)
        }

        @Test
        fun `setButtons preserves other fields`() {
            // Given
            val chatId = 123L
            val existing = ChannelReplySettings(
                id = 1,
                chatId = chatId,
                replyText = "Text",
                enabled = true,
                mediaFileId = "file_123",
                mediaType = "PHOTO"
            )

            every { repository.findByChatId(chatId) } returns existing

            val settingsSlot = slot<ChannelReplySettings>()
            every { repository.save(capture(settingsSlot)) } answers { firstArg() }

            val buttons = listOf(ReplyButton("New", "https://new.com"))

            // When
            service.setButtons(chatId, buttons)

            // Then
            val saved = settingsSlot.captured
            assertNotNull(saved.buttonsJson)
            // Other fields should be preserved
            assertEquals("Text", saved.replyText)
            assertTrue(saved.enabled)
            assertEquals("file_123", saved.mediaFileId)
            assertEquals("PHOTO", saved.mediaType)
        }
    }
}
