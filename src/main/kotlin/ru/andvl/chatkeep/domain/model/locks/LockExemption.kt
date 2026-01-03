package ru.andvl.chatkeep.domain.model.locks

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("lock_exemptions")
data class LockExemption(
    @Id
    val id: Long? = null,

    @Column("chat_id")
    val chatId: Long,

    @Column("lock_type")
    val lockType: String?,

    @Column("exemption_type")
    val exemptionType: String,

    @Column("exemption_value")
    val exemptionValue: String,

    @Column("created_at")
    val createdAt: Instant = Instant.now()
)

enum class ExemptionType {
    USER,
    BOT,
    CHANNEL,
    STICKER_SET,
    INLINE_BOT
}
