package ru.andvl.chatkeep.domain.model

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.PersistenceCreator
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("rules")
data class Rules @PersistenceCreator constructor(
    @Id
    @Column("chat_id")
    val chatId: Long,

    @Column("rules_text")
    val rulesText: String,

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
            rulesText: String
        ): Rules {
            return Rules(
                chatId = chatId,
                rulesText = rulesText,
                createdAt = Instant.now(),
                updatedAt = null
            ).also { it._isNew = true }
        }
    }
}
