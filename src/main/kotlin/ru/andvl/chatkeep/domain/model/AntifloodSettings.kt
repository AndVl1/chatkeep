package ru.andvl.chatkeep.domain.model

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.PersistenceCreator
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("antiflood_settings")
data class AntifloodSettings @PersistenceCreator constructor(
    @Id
    @Column("chat_id")
    val chatId: Long,

    @Column("enabled")
    val enabled: Boolean = false,

    @Column("max_messages")
    val maxMessages: Int = 5,

    @Column("time_window_seconds")
    val timeWindowSeconds: Int = 5,

    @Column("action")
    val action: String = "MUTE",

    @Column("action_duration_minutes")
    val actionDurationMinutes: Int? = null,

    @Column("created_at")
    val createdAt: Instant = Instant.now(),

    @Column("updated_at")
    val updatedAt: Instant? = null
) : Persistable<Long> {

    @Transient
    private var _isNew: Boolean = false

    override fun getId(): Long = chatId

    override fun isNew(): Boolean = _isNew

    companion object {
        fun createNew(
            chatId: Long,
            enabled: Boolean = false,
            maxMessages: Int = 5,
            timeWindowSeconds: Int = 5,
            action: String = "MUTE",
            actionDurationMinutes: Int? = null
        ): AntifloodSettings {
            return AntifloodSettings(
                chatId = chatId,
                enabled = enabled,
                maxMessages = maxMessages,
                timeWindowSeconds = timeWindowSeconds,
                action = action,
                actionDurationMinutes = actionDurationMinutes,
                createdAt = Instant.now(),
                updatedAt = null
            ).also { it._isNew = true }
        }
    }
}
