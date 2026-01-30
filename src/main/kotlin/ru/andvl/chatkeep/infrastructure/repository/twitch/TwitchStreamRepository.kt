package ru.andvl.chatkeep.infrastructure.repository.twitch

import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import ru.andvl.chatkeep.domain.model.twitch.TwitchStream
import java.time.Instant

interface TwitchStreamRepository : CrudRepository<TwitchStream, Long> {

    @Query("SELECT * FROM twitch_streams WHERE subscription_id = :subscriptionId AND status = 'live'")
    fun findActiveBySubscriptionId(subscriptionId: Long): TwitchStream?

    @Query("SELECT * FROM twitch_streams WHERE status = 'live'")
    fun findAllActive(): List<TwitchStream>

    @Query("SELECT * FROM twitch_streams WHERE subscription_id = :subscriptionId ORDER BY started_at DESC LIMIT 1")
    fun findLatestBySubscriptionId(subscriptionId: Long): TwitchStream?

    @Query("""
        SELECT * FROM twitch_streams
        WHERE subscription_id = :subscriptionId
        AND status = 'ended'
        AND ended_at > :since
        ORDER BY ended_at DESC
        LIMIT 1
    """)
    fun findRecentlyEndedBySubscriptionId(subscriptionId: Long, since: Instant): TwitchStream?
}
