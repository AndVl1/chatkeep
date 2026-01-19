package ru.andvl.chatkeep.domain.model.channelreply

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("channel_reply_settings")
data class ChannelReplySettings(
    @Id
    val id: Long? = null,

    @Column("chat_id")
    val chatId: Long,

    @Column("enabled")
    val enabled: Boolean = false,

    @Column("reply_text")
    val replyText: String? = null,

    @Column("media_file_id")
    val mediaFileId: String? = null,

    @Column("media_type")
    val mediaType: String? = null,

    @Column("media_hash")
    val mediaHash: String? = null,

    @Column("buttons_json")
    val buttonsJson: String? = null,

    @Column("created_at")
    val createdAt: Instant = Instant.now(),

    @Column("updated_at")
    val updatedAt: Instant = Instant.now()
)
