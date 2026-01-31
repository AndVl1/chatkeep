package ru.andvl.chatkeep.infrastructure

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import ru.andvl.chatkeep.config.TestConfiguration
import ru.andvl.chatkeep.domain.model.media.MediaStorage
import ru.andvl.chatkeep.infrastructure.repository.media.MediaStorageRepository
import kotlin.test.*

/**
 * Tests for BLOB (BYTEA) serialization/deserialization in media_storage table.
 * Ensures binary data is stored and retrieved correctly without corruption.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Import(TestConfiguration::class)
class MediaStorageBlobTest {

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
    private lateinit var repository: MediaStorageRepository

    @Test
    fun `should serialize and deserialize small binary data`() {
        // Arrange: Small PNG header
        val pngHeader = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
        )
        val media = MediaStorage(
            hash = "test_small_blob",
            content = pngHeader,
            mimeType = "image/png",
            fileSize = pngHeader.size.toLong()
        )

        // Act: Save and retrieve
        val saved = repository.save(media)
        val retrieved = repository.findByHash("test_small_blob")

        // Assert: Binary data should be identical
        assertNotNull(retrieved)
        assertContentEquals(pngHeader, retrieved.content, "Binary data should match exactly")
        assertEquals("image/png", retrieved.mimeType)
    }

    @Test
    fun `should serialize and deserialize large binary data`() {
        // Arrange: 1MB of random binary data
        val largeData = ByteArray(1024 * 1024) { (it % 256).toByte() }
        val media = MediaStorage(
            hash = "test_large_blob",
            content = largeData,
            mimeType = "application/octet-stream",
            fileSize = largeData.size.toLong()
        )

        // Act
        val saved = repository.save(media)
        val retrieved = repository.findByHash("test_large_blob")

        // Assert
        assertNotNull(retrieved)
        assertEquals(largeData.size, retrieved.content.size, "Size should match")
        assertContentEquals(largeData, retrieved.content, "Large binary data should match")
    }

    @Test
    fun `should handle binary data with all byte values`() {
        // Arrange: All possible byte values (0x00 - 0xFF)
        val allBytes = ByteArray(256) { it.toByte() }
        val media = MediaStorage(
            hash = "test_all_bytes",
            content = allBytes,
            mimeType = "application/octet-stream",
            fileSize = allBytes.size.toLong()
        )

        // Act
        val saved = repository.save(media)
        val retrieved = repository.findByHash("test_all_bytes")

        // Assert
        assertNotNull(retrieved)
        assertContentEquals(allBytes, retrieved.content, "Should preserve all byte values including 0x00")
    }

    @Test
    fun `should preserve binary data across multiple updates`() {
        // Arrange: Initial data
        val initialData = byteArrayOf(0x01, 0x02, 0x03)
        val media = MediaStorage(
            hash = "test_updates",
            content = initialData,
            mimeType = "application/octet-stream",
            fileSize = initialData.size.toLong()
        )

        // Act: Save, update telegram_file_id, retrieve
        val saved = repository.save(media)
        val updated = saved.copy(telegramFileId = "file_123")
        repository.save(updated)
        val retrieved = repository.findByHash("test_updates")

        // Assert: Binary data should remain unchanged
        assertNotNull(retrieved)
        assertContentEquals(initialData, retrieved.content, "Binary data should not be corrupted by updates")
        assertEquals("file_123", retrieved.telegramFileId)
    }

    @Test
    fun `should handle empty binary data`() {
        // Arrange: Empty array
        val emptyData = ByteArray(0)
        val media = MediaStorage(
            hash = "test_empty_blob",
            content = emptyData,
            mimeType = "application/octet-stream",
            fileSize = 0
        )

        // Act
        val saved = repository.save(media)
        val retrieved = repository.findByHash("test_empty_blob")

        // Assert
        assertNotNull(retrieved)
        assertEquals(0, retrieved.content.size, "Empty blob should have size 0")
        assertContentEquals(emptyData, retrieved.content)
    }

    @Test
    fun `should verify MD5 hash consistency after retrieval`() {
        // Arrange: Test data with known MD5
        val testData = "Hello, World!".toByteArray(Charsets.UTF_8)
        val expectedMd5 = calculateMd5(testData)

        val media = MediaStorage(
            hash = expectedMd5,
            content = testData,
            mimeType = "text/plain",
            fileSize = testData.size.toLong()
        )

        // Act
        repository.save(media)
        val retrieved = repository.findByHash(expectedMd5)

        // Assert: Recalculated hash should match
        assertNotNull(retrieved)
        val actualMd5 = calculateMd5(retrieved.content)
        assertEquals(expectedMd5, actualMd5, "MD5 hash should match after deserialization")
    }

    @Test
    fun `should maintain referential integrity when deleting media`() {
        // Arrange
        val media = MediaStorage(
            hash = "test_delete",
            content = byteArrayOf(0x01, 0x02),
            mimeType = "application/octet-stream",
            fileSize = 2
        )

        // Act
        val saved = repository.save(media)
        repository.delete(saved)
        val retrieved = repository.findByHash("test_delete")

        // Assert
        assertNull(retrieved, "Deleted media should not be retrievable")
    }

    private fun calculateMd5(data: ByteArray): String {
        val md = java.security.MessageDigest.getInstance("MD5")
        val digest = md.digest(data)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
