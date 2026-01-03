package ru.andvl.chatkeep.domain.model.moderation

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("blocklist_patterns")
data class BlocklistPattern(
    @Id
    val id: Long? = null,

    @Column("chat_id")
    val chatId: Long?,

    @Column("pattern")
    val pattern: String,

    @Column("match_type")
    val matchType: String,

    @Column("action")
    val action: String,

    @Column("action_duration_minutes")
    val actionDurationMinutes: Int?,

    @Column("severity")
    val severity: Int = 0,

    @Column("created_at")
    val createdAt: Instant = Instant.now()
)
