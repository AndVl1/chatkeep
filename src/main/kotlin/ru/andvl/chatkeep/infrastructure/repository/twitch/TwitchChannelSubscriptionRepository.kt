package ru.andvl.chatkeep.infrastructure.repository.twitch

import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import ru.andvl.chatkeep.domain.model.twitch.TwitchChannelSubscription

interface TwitchChannelSubscriptionRepository : CrudRepository<TwitchChannelSubscription, Long> {

    @Query("SELECT * FROM twitch_channel_subscriptions WHERE chat_id = :chatId")
    fun findByChatId(chatId: Long): List<TwitchChannelSubscription>

    @Query("SELECT * FROM twitch_channel_subscriptions WHERE chat_id = :chatId AND twitch_channel_id = :twitchChannelId")
    fun findByChatIdAndTwitchChannelId(chatId: Long, twitchChannelId: String): TwitchChannelSubscription?

    @Query("SELECT * FROM twitch_channel_subscriptions WHERE twitch_channel_id = :twitchChannelId")
    fun findByTwitchChannelId(twitchChannelId: String): List<TwitchChannelSubscription>

    @Query("SELECT COUNT(*) FROM twitch_channel_subscriptions WHERE chat_id = :chatId")
    fun countByChatId(chatId: Long): Int

    @Modifying
    @Query("UPDATE twitch_channel_subscriptions SET is_pinned = false WHERE chat_id = :chatId")
    fun unpinAllForChat(chatId: Long): Int
}
