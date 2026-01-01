package ru.andvl.chatkeep.domain.model.moderation

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("warnings")
data class Warning(
    @Id
    val id: Long? = null,

    @Column("chat_id")
    val chatId: Long,

    @Column("user_id")
    val userId: Long,

    @Column("issued_by_id")
    val issuedById: Long,

    @Column("reason")
    val reason: String?,

    @Column("created_at")
    val createdAt: Instant = Instant.now(),

    @Column("expires_at")
    val expiresAt: Instant
)
