package ru.andvl.chatkeep.domain.model

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.PersistenceCreator
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("welcome_settings")
data class WelcomeSettings @PersistenceCreator constructor(
    @Id
    @Column("chat_id")
    val chatId: Long,

    @Column("enabled")
    val enabled: Boolean = true,

    @Column("message_text")
    val messageText: String? = null,

    @Column("send_to_chat")
    val sendToChat: Boolean = true,

    @Column("delete_after_seconds")
    val deleteAfterSeconds: Int? = null,

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
            enabled: Boolean = true,
            messageText: String? = null,
            sendToChat: Boolean = true,
            deleteAfterSeconds: Int? = null
        ): WelcomeSettings {
            return WelcomeSettings(
                chatId = chatId,
                enabled = enabled,
                messageText = messageText,
                sendToChat = sendToChat,
                deleteAfterSeconds = deleteAfterSeconds,
                createdAt = Instant.now(),
                updatedAt = null
            ).also { it._isNew = true }
        }
    }
}
