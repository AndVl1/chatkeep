package ru.andvl.chatkeep.domain.model.locks

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.PersistenceCreator
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("lock_settings")
data class LockSettings @PersistenceCreator constructor(
    @Id
    @Column("chat_id")
    val chatId: Long,

    @Column("locks_json")
    val locksJson: String = "{}",

    @Column("lock_warns")
    val lockWarns: Boolean = false,

    @Column("created_at")
    val createdAt: Instant = Instant.now(),

    @Column("updated_at")
    val updatedAt: Instant = Instant.now()
) : Persistable<Long> {

    // Transient field to track if this is a new entity - NOT in constructor
    // Entities loaded from DB via @PersistenceCreator will have this as false (not new)
    // Only entities created via createNew() will be marked as new
    @Transient
    private var _isNew: Boolean = false

    override fun getId(): Long = chatId

    override fun isNew(): Boolean = _isNew

    // Factory method to create new entity (for INSERT)
    companion object {
        fun createNew(
            chatId: Long,
            locksJson: String = "{}",
            lockWarns: Boolean = false
        ): LockSettings {
            return LockSettings(
                chatId = chatId,
                locksJson = locksJson,
                lockWarns = lockWarns,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            ).also { it._isNew = true }
        }
    }
}
