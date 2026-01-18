package ru.andvl.chatkeep.domain.model.media

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("media_storage")
data class MediaStorage(
    @Id
    val id: Long? = null,

    @Column("hash")
    val hash: String,

    @Column("content")
    val content: ByteArray,

    @Column("mime_type")
    val mimeType: String,

    @Column("file_size")
    val fileSize: Long,

    @Column("telegram_file_id")
    val telegramFileId: String? = null,

    @Column("created_at")
    val createdAt: Instant = Instant.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MediaStorage

        if (id != other.id) return false
        if (hash != other.hash) return false
        if (!content.contentEquals(other.content)) return false
        if (mimeType != other.mimeType) return false
        if (fileSize != other.fileSize) return false
        if (telegramFileId != other.telegramFileId) return false
        if (createdAt != other.createdAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + hash.hashCode()
        result = 31 * result + content.contentHashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + fileSize.hashCode()
        result = 31 * result + (telegramFileId?.hashCode() ?: 0)
        result = 31 * result + createdAt.hashCode()
        return result
    }
}
