package ru.andvl.chatkeep.domain.model

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("chat_settings")
data class ChatSettings(
    @Id
    val id: Long? = null,

    @Column("chat_id")
    val chatId: Long,

    @Column("chat_title")
    val chatTitle: String?,

    @Column("collection_enabled")
    val collectionEnabled: Boolean = true,

    @Column("created_at")
    val createdAt: Instant = Instant.now(),

    @Column("updated_at")
    val updatedAt: Instant = Instant.now()
)
