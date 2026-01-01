package ru.andvl.chatkeep.domain.model.moderation

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("admin_sessions")
data class AdminSession(
    @Id
    val id: Long? = null,

    @Column("user_id")
    val userId: Long,

    @Column("connected_chat_id")
    val connectedChatId: Long,

    @Column("connected_chat_title")
    val connectedChatTitle: String?,

    @Column("created_at")
    val createdAt: Instant = Instant.now()
)
