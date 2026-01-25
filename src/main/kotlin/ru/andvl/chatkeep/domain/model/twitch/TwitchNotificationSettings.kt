package ru.andvl.chatkeep.domain.model.twitch

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.PersistenceCreator
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("twitch_notification_settings")
data class TwitchNotificationSettings @PersistenceCreator constructor(
    @Id
    @Column("chat_id")
    val chatId: Long,

    @Column("message_template")
    val messageTemplate: String = DEFAULT_TEMPLATE,

    @Column("ended_message_template")
    val endedMessageTemplate: String = DEFAULT_ENDED_TEMPLATE,

    @Column("button_text")
    val buttonText: String = DEFAULT_BUTTON_TEXT,

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
        const val DEFAULT_TEMPLATE = """🔴 {streamer} начал стрим!

{title}

🎮 {game}
👥 {viewers} зрителей
⏱ {duration}"""

        const val DEFAULT_ENDED_TEMPLATE = """⚫️ {streamer} завершил стрим

{title}

🎮 {game}
⏱ Продолжительность: {duration}"""

        const val DEFAULT_BUTTON_TEXT = "📺 Смотреть"

        fun createNew(
            chatId: Long,
            messageTemplate: String = DEFAULT_TEMPLATE,
            endedMessageTemplate: String = DEFAULT_ENDED_TEMPLATE,
            buttonText: String = DEFAULT_BUTTON_TEXT
        ): TwitchNotificationSettings {
            return TwitchNotificationSettings(
                chatId = chatId,
                messageTemplate = messageTemplate,
                endedMessageTemplate = endedMessageTemplate,
                buttonText = buttonText,
                createdAt = Instant.now(),
                updatedAt = null
            ).also { it._isNew = true }
        }
    }
}
