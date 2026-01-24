package ru.andvl.chatkeep.domain.model

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("notes")
data class Note(
    @Id
    val id: Long = 0,

    @Column("chat_id")
    val chatId: Long,

    @Column("note_name")
    val noteName: String,

    @Column("content")
    val content: String,

    @Column("created_by")
    val createdBy: Long,

    @Column("created_at")
    val createdAt: Instant = Instant.now(),

    @Column("updated_at")
    val updatedAt: Instant? = null
)
