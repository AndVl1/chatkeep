package ru.andvl.chatkeep.domain.model.locks

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("lock_settings")
data class LockSettings(
    @Id
    @Column("chat_id")
    val chatId: Long,

    @Column("locks_json")
    val locksJson: String? = null,

    @Column("lock_warns")
    val lockWarns: Boolean = false,

    @Column("created_at")
    val createdAt: Instant = Instant.now(),

    @Column("updated_at")
    val updatedAt: Instant = Instant.now()
)
