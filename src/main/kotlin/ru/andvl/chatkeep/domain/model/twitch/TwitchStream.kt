package ru.andvl.chatkeep.domain.model.twitch

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.PersistenceCreator
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("twitch_streams")
data class TwitchStream @PersistenceCreator constructor(
    @Id
    @Column("id")
    val id: Long? = null,

    @Column("subscription_id")
    val subscriptionId: Long,

    @Column("twitch_stream_id")
    val twitchStreamId: String? = null,

    @Column("started_at")
    val startedAt: Instant,

    @Column("ended_at")
    val endedAt: Instant? = null,

    @Column("telegram_message_id")
    val telegramMessageId: Long? = null,

    @Column("telegram_chat_id")
    val telegramChatId: Long? = null,

    @Column("status")
    val status: String = "live", // live, ended

    @Column("current_game")
    val currentGame: String? = null,

    @Column("current_title")
    val currentTitle: String? = null,

    @Column("viewer_count")
    val viewerCount: Int = 0
) {

    companion object {
        fun createNew(
            subscriptionId: Long,
            twitchStreamId: String?,
            startedAt: Instant,
            currentGame: String?,
            currentTitle: String?
        ): TwitchStream {
            return TwitchStream(
                id = null,
                subscriptionId = subscriptionId,
                twitchStreamId = twitchStreamId,
                startedAt = startedAt,
                endedAt = null,
                telegramMessageId = null,
                telegramChatId = null,
                status = "live",
                currentGame = currentGame,
                currentTitle = currentTitle,
                viewerCount = 0
            )
        }
    }
}
