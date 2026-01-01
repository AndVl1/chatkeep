package ru.andvl.chatkeep.domain.model.moderation

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("punishments")
data class Punishment(
    @Id
    val id: Long? = null,

    @Column("chat_id")
    val chatId: Long,

    @Column("user_id")
    val userId: Long,

    @Column("issued_by_id")
    val issuedById: Long,

    @Column("punishment_type")
    val punishmentType: String,

    @Column("duration_seconds")
    val durationSeconds: Long?,

    @Column("reason")
    val reason: String?,

    @Column("source")
    val source: String,

    @Column("message_text")
    val messageText: String? = null,

    @Column("created_at")
    val createdAt: Instant = Instant.now()
)
