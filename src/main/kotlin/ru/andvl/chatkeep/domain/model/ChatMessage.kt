package ru.andvl.chatkeep.domain.model

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("messages")
data class ChatMessage(
    @Id
    val id: Long? = null,

    @Column("telegram_message_id")
    val telegramMessageId: Long,

    @Column("chat_id")
    val chatId: Long,

    @Column("user_id")
    val userId: Long,

    val username: String?,

    @Column("first_name")
    val firstName: String?,

    @Column("last_name")
    val lastName: String?,

    val text: String,

    @Column("message_date")
    val messageDate: Instant,

    @Column("created_at")
    val createdAt: Instant = Instant.now()
)
