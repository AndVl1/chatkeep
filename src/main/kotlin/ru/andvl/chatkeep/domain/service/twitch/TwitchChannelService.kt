package ru.andvl.chatkeep.domain.service.twitch

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.andvl.chatkeep.domain.model.twitch.TwitchChannelSubscription
import ru.andvl.chatkeep.domain.model.twitch.TwitchStream
import ru.andvl.chatkeep.infrastructure.repository.twitch.TwitchChannelSubscriptionRepository
import ru.andvl.chatkeep.infrastructure.repository.twitch.TwitchStreamRepository
import java.time.Instant

@Service
class TwitchChannelService(
    private val repository: TwitchChannelSubscriptionRepository,
    private val streamRepository: TwitchStreamRepository,
    private val twitchApiClient: TwitchApiClient
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        const val MAX_CHANNELS_PER_CHAT = 5
    }

    /**
     * Get all channel subscriptions for a chat
     */
    fun getChannelSubscriptions(chatId: Long): List<TwitchChannelSubscription> {
        return repository.findByChatId(chatId)
    }

    /**
     * Subscribe to a Twitch channel
     * Returns null if limit reached or channel already subscribed
     */
    @Transactional
    fun subscribeToChannel(
        chatId: Long,
        twitchLogin: String,
        createdBy: Long
    ): TwitchChannelSubscription? {
        // Check limit
        val currentCount = repository.countByChatId(chatId)
        if (currentCount >= MAX_CHANNELS_PER_CHAT) {
            logger.warn("Chat $chatId reached max channels limit ($MAX_CHANNELS_PER_CHAT)")
            return null
        }

        // Get channel info from Twitch
        val twitchUser = twitchApiClient.getUserByLogin(twitchLogin)
            ?: throw IllegalArgumentException("Twitch channel not found: $twitchLogin")

        // Check if already subscribed
        val existing = repository.findByChatIdAndTwitchChannelId(chatId, twitchUser.id)
        if (existing != null) {
            logger.info("Chat $chatId already subscribed to ${twitchUser.login}")
            return null
        }

        // Create subscription
        val subscription = TwitchChannelSubscription.createNew(
            chatId = chatId,
            twitchChannelId = twitchUser.id,
            twitchLogin = twitchUser.login,
            displayName = twitchUser.displayName,
            avatarUrl = twitchUser.profileImageUrl,
            createdBy = createdBy
        )

        val saved = repository.save(subscription)
        logger.info("Created subscription: chatId=$chatId, channel=${twitchUser.login}")

        // Check if channel is currently live
        var isCurrentlyLive = false
        try {
            val currentStream = twitchApiClient.getStreams(listOf(twitchUser.id)).firstOrNull()
            if (currentStream != null) {
                logger.info("Channel ${twitchUser.login} is currently live, creating stream record")
                val stream = TwitchStream.createNew(
                    subscriptionId = saved.id!!,
                    twitchStreamId = currentStream.id,
                    startedAt = Instant.parse(currentStream.startedAt),
                    currentGame = currentStream.gameName,
                    currentTitle = currentStream.title
                )
                streamRepository.save(stream)
                isCurrentlyLive = true
            }
        } catch (e: Exception) {
            logger.error("Failed to check initial stream status for ${twitchUser.login}", e)
        }

        // Create EventSub subscriptions
        try {
            val onlineSubscription = twitchApiClient.createEventSubSubscription(
                type = "stream.online",
                version = "1",
                condition = mapOf("broadcaster_user_id" to twitchUser.id)
            )

            val offlineSubscription = twitchApiClient.createEventSubSubscription(
                type = "stream.offline",
                version = "1",
                condition = mapOf("broadcaster_user_id" to twitchUser.id)
            )

            // Log subscription status
            logger.info(
                "Created EventSub subscriptions for ${twitchUser.login}: " +
                    "online=${onlineSubscription.id} (status=${onlineSubscription.status}), " +
                    "offline=${offlineSubscription.id} (status=${offlineSubscription.status})"
            )

            // Warn if subscriptions are not enabled
            if (onlineSubscription.status != "enabled") {
                logger.warn("EventSub online subscription for ${twitchUser.login} is not enabled: status=${onlineSubscription.status}")
            }
            if (offlineSubscription.status != "enabled") {
                logger.warn("EventSub offline subscription for ${twitchUser.login} is not enabled: status=${offlineSubscription.status}")
            }

            // Update with EventSub subscription ID (storing only online, offline will be cleaned up automatically)
            val updated = saved.copy(eventsubSubscriptionId = onlineSubscription.id)
            repository.save(updated)

            return updated
        } catch (e: Exception) {
            logger.error("Failed to create EventSub subscriptions", e)
            // Keep the subscription but without EventSub (will rely on polling)
            return saved
        }
    }

    /**
     * Unsubscribe from a Twitch channel
     */
    @Transactional
    fun unsubscribeFromChannel(subscriptionId: Long) {
        val subscription = repository.findById(subscriptionId).orElse(null)
            ?: throw IllegalArgumentException("Subscription not found: $subscriptionId")

        // Delete EventSub subscription if exists
        subscription.eventsubSubscriptionId?.let { eventsubId ->
            try {
                twitchApiClient.deleteEventSubSubscription(eventsubId)
                logger.info("Deleted EventSub subscription: $eventsubId")
            } catch (e: Exception) {
                logger.error("Failed to delete EventSub subscription: $eventsubId", e)
            }
        }

        repository.deleteById(subscriptionId)
        logger.info("Deleted subscription: id=$subscriptionId, channel=${subscription.twitchLogin}")
    }

    /**
     * Search Twitch channels
     */
    fun searchChannels(query: String): List<TwitchSearchChannel> {
        return twitchApiClient.searchChannels(query)
    }

    /**
     * Resync EventSub subscriptions for channels that don't have eventsub_subscription_id.
     * This is useful after credentials update or when subscriptions failed to create.
     * @return list of pairs (channel_login, success)
     */
    @Transactional
    fun resyncEventSubSubscriptions(): List<Pair<String, Boolean>> {
        val subscriptionsWithoutEventSub = repository.findAll()
            .filter { it.eventsubSubscriptionId == null }

        if (subscriptionsWithoutEventSub.isEmpty()) {
            logger.info("All subscriptions already have EventSub IDs")
            return emptyList()
        }

        logger.info("Found ${subscriptionsWithoutEventSub.size} subscriptions without EventSub ID")

        return subscriptionsWithoutEventSub.map { subscription ->
            try {
                logger.info("Creating EventSub subscriptions for ${subscription.twitchLogin} (id=${subscription.id})")

                val onlineSubscription = twitchApiClient.createEventSubSubscription(
                    type = "stream.online",
                    version = "1",
                    condition = mapOf("broadcaster_user_id" to subscription.twitchChannelId)
                )

                val offlineSubscription = twitchApiClient.createEventSubSubscription(
                    type = "stream.offline",
                    version = "1",
                    condition = mapOf("broadcaster_user_id" to subscription.twitchChannelId)
                )

                logger.info(
                    "Created EventSub for ${subscription.twitchLogin}: " +
                        "online=${onlineSubscription.id} (status=${onlineSubscription.status}), " +
                        "offline=${offlineSubscription.id} (status=${offlineSubscription.status})"
                )

                // Update with EventSub subscription ID
                val updated = subscription.copy(eventsubSubscriptionId = onlineSubscription.id)
                repository.save(updated)

                subscription.twitchLogin to true
            } catch (e: Exception) {
                logger.error("Failed to create EventSub for ${subscription.twitchLogin}", e)
                subscription.twitchLogin to false
            }
        }
    }
}
