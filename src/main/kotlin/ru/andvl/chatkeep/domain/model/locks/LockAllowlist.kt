package ru.andvl.chatkeep.domain.model.locks

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("lock_allowlist")
data class LockAllowlist(
    @Id
    val id: Long? = null,

    @Column("chat_id")
    val chatId: Long,

    @Column("allowlist_type")
    val allowlistType: String,

    @Column("pattern")
    val pattern: String,

    @Column("created_at")
    val createdAt: Instant = Instant.now()
)

enum class AllowlistType {
    URL,
    COMMAND,
    DOMAIN
}
