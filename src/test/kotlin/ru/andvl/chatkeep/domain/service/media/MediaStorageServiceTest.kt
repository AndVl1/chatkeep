package ru.andvl.chatkeep.domain.service.media

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import ru.andvl.chatkeep.domain.model.media.MediaStorage
import ru.andvl.chatkeep.infrastructure.repository.media.MediaStorageRepository
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MediaStorageServiceTest {

    private val mockRepository = mockk<MediaStorageRepository>()
    private val service = MediaStorageService(mockRepository)

    @Test
    fun `calculateMd5 should generate correct hash`() {
        val bytes = "test content".toByteArray()
        val hash = service.calculateMd5(bytes)
        assertEquals("9473fdd0d880a43c21b7778d34872157", hash)
    }

    @Test
    fun `calculateMd5 should generate same hash for same content`() {
        val bytes = "test content".toByteArray()
        val hash1 = service.calculateMd5(bytes)
        val hash2 = service.calculateMd5(bytes)
        assertEquals(hash1, hash2)
    }

    @Test
    fun `calculateMd5 should generate different hash for different content`() {
        val hash1 = service.calculateMd5("content1".toByteArray())
        val hash2 = service.calculateMd5("content2".toByteArray())
        assert(hash1 != hash2)
    }

    @Test
    fun `getFileId should return cached file_id if exists`() {
        val hash = "abc123"
        val fileId = "file_id_from_telegram"
        val media = MediaStorage(
            hash = hash,
            content = ByteArray(10),
            mimeType = "image/png",
            fileSize = 10,
            telegramFileId = fileId
        )

        every { mockRepository.findByHash(hash) } returns media

        val result = service.getFileId(hash)

        assertEquals(fileId, result)
        verify { mockRepository.findByHash(hash) }
    }

    @Test
    fun `getFileId should return null if not cached`() {
        val hash = "abc123"
        val media = MediaStorage(
            hash = hash,
            content = ByteArray(10),
            mimeType = "image/png",
            fileSize = 10
        )

        every { mockRepository.findByHash(hash) } returns media

        val result = service.getFileId(hash)

        assertNull(result)
    }

    @Test
    fun `getFileId should return null if media not found`() {
        every { mockRepository.findByHash(any()) } returns null

        val result = service.getFileId("nonexistent")

        assertNull(result)
    }

    @Test
    fun `saveFileId should save file_id when not cached`() {
        val hash = "abc123"
        val fileId = "new_file_id"
        val media = MediaStorage(
            hash = hash,
            content = ByteArray(10),
            mimeType = "image/png",
            fileSize = 10
        )
        val updatedMedia = media.copy(telegramFileId = fileId)

        every { mockRepository.findByHash(hash) } returns media
        every { mockRepository.save(updatedMedia) } returns updatedMedia

        service.saveFileId(hash, fileId)

        verify { mockRepository.findByHash(hash) }
        verify { mockRepository.save(updatedMedia) }
    }

    @Test
    fun `saveFileId should skip if file_id already cached`() {
        val hash = "abc123"
        val existingFileId = "existing_file_id"
        val media = MediaStorage(
            hash = hash,
            content = ByteArray(10),
            mimeType = "image/png",
            fileSize = 10,
            telegramFileId = existingFileId
        )

        every { mockRepository.findByHash(hash) } returns media

        service.saveFileId(hash, "new_file_id")

        verify { mockRepository.findByHash(hash) }
        verify(exactly = 0) { mockRepository.save(any()) }
    }

    @Test
    fun `saveFileId should throw if media not found`() {
        every { mockRepository.findByHash(any()) } returns null

        assertThrows<IllegalArgumentException> {
            service.saveFileId("nonexistent", "file_id")
        }
    }

    @Test
    fun `getMedia should return media by hash`() {
        val hash = "abc123"
        val media = MediaStorage(
            hash = hash,
            content = byteArrayOf(1, 2, 3),
            mimeType = "image/png",
            fileSize = 3
        )

        every { mockRepository.findByHash(hash) } returns media

        val result = service.getMedia(hash)

        assertNotNull(result)
        assertEquals(hash, result.hash)
        verify { mockRepository.findByHash(hash) }
    }

    @Test
    fun `getMedia should return null if not found`() {
        every { mockRepository.findByHash(any()) } returns null

        val result = service.getMedia("nonexistent")

        assertNull(result)
    }

    @Test
    fun `deleteOrphanMedia should delete media older than specified days without file_id`() {
        val cutoffInstant = Instant.now()
        every { mockRepository.deleteByCreatedAtBeforeAndTelegramFileIdIsNull(any()) } returns 5

        val result = service.deleteOrphanMedia(30)

        assertEquals(5, result)
        verify { mockRepository.deleteByCreatedAtBeforeAndTelegramFileIdIsNull(any()) }
    }

    @Test
    fun `deleteOrphanMedia should return 0 if no orphans found`() {
        every { mockRepository.deleteByCreatedAtBeforeAndTelegramFileIdIsNull(any()) } returns 0

        val result = service.deleteOrphanMedia(30)

        assertEquals(0, result)
    }
}
