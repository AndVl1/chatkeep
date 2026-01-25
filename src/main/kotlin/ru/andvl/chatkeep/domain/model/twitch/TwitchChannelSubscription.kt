package ru.andvl.chatkeep.domain.model.twitch

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.PersistenceCreator
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("twitch_channel_subscriptions")
data class TwitchChannelSubscription @PersistenceCreator constructor(
    @Id
    @Column("id")
    val id: Long? = null,

    @Column("chat_id")
    val chatId: Long,

    @Column("twitch_channel_id")
    val twitchChannelId: String,

    @Column("twitch_login")
    val twitchLogin: String,

    @Column("display_name")
    val displayName: String? = null,

    @Column("avatar_url")
    val avatarUrl: String? = null,

    @Column("eventsub_subscription_id")
    val eventsubSubscriptionId: String? = null,

    @Column("created_at")
    val createdAt: Instant = Instant.now(),

    @Column("created_by")
    val createdBy: Long? = null
) {

    companion object {
        fun createNew(
            chatId: Long,
            twitchChannelId: String,
            twitchLogin: String,
            displayName: String?,
            avatarUrl: String?,
            createdBy: Long?
        ): TwitchChannelSubscription {
            return TwitchChannelSubscription(
                id = null,
                chatId = chatId,
                twitchChannelId = twitchChannelId,
                twitchLogin = twitchLogin,
                displayName = displayName,
                avatarUrl = avatarUrl,
                eventsubSubscriptionId = null,
                createdAt = Instant.now(),
                createdBy = createdBy
            )
        }
    }
}
