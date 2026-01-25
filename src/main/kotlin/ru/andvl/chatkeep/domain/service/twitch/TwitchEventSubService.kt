package ru.andvl.chatkeep.domain.service.twitch

import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.andvl.chatkeep.domain.model.twitch.StreamTimelineEvent
import ru.andvl.chatkeep.domain.model.twitch.TwitchStream
import ru.andvl.chatkeep.infrastructure.repository.twitch.StreamTimelineEventRepository
import ru.andvl.chatkeep.infrastructure.repository.twitch.TwitchChannelSubscriptionRepository
import ru.andvl.chatkeep.infrastructure.repository.twitch.TwitchStreamRepository
import java.time.Instant
import java.time.format.DateTimeFormatter

@Service
class TwitchEventSubService(
    private val channelRepo: TwitchChannelSubscriptionRepository,
    private val streamRepo: TwitchStreamRepository,
    private val timelineRepo: StreamTimelineEventRepository,
    private val notificationService: TwitchNotificationService,
    private val twitchApiClient: TwitchApiClient
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @PreDestroy
    fun cleanup() {
        scope.cancel()
    }

    /**
     * Handle stream.online event
     */
    @Transactional
    fun handleStreamOnline(broadcasterId: String, streamId: String, startedAt: String) {
        logger.info("EventSub: Stream online event received - broadcasterId=$broadcasterId, streamId=$streamId, startedAt=$startedAt")

        val subscriptions = channelRepo.findByTwitchChannelId(broadcasterId)
        if (subscriptions.isEmpty()) {
            logger.warn("EventSub: No subscriptions found for broadcaster: $broadcasterId")
            return
        }

        logger.info("EventSub: Found ${subscriptions.size} subscription(s) for broadcaster $broadcasterId")

        // Get stream details
        val streamData = twitchApiClient.getStreams(listOf(broadcasterId)).firstOrNull()
        logger.info("EventSub: Stream data fetched for $broadcasterId: ${if (streamData != null) "found" else "not found"}")

        subscriptions.forEach { subscription ->
            try {
                logger.info("EventSub: Processing subscription ${subscription.id} for chat ${subscription.chatId}")

                // Check if stream already exists
                val existing = streamRepo.findActiveBySubscriptionId(subscription.id!!)
                if (existing != null) {
                    logger.info("EventSub: Stream already active for subscription ${subscription.id}, skipping")
                    return@forEach
                }

                // Create stream record
                val stream = TwitchStream.createNew(
                    subscriptionId = subscription.id,
                    twitchStreamId = streamId,
                    startedAt = Instant.parse(startedAt),
                    currentGame = streamData?.gameName,
                    currentTitle = streamData?.title
                )

                val saved = streamRepo.save(stream)
                logger.info("EventSub: Created stream record with id=${saved.id} for subscription ${subscription.id}")

                // Create initial timeline event
                timelineRepo.save(
                    StreamTimelineEvent.createNew(
                        streamId = saved.id!!,
                        streamOffsetSeconds = 0,
                        gameName = streamData?.gameName,
                        streamTitle = streamData?.title
                    )
                )
                logger.info("EventSub: Created timeline event for stream ${saved.id}")

                // Send notification asynchronously
                logger.info("EventSub: Sending stream start notification for subscription ${subscription.id} to chat ${subscription.chatId}")
                scope.launch {
                    try {
                        val messageId = notificationService.sendStreamStartNotification(
                            chatId = subscription.chatId,
                            stream = saved,
                            streamerName = subscription.displayName ?: subscription.twitchLogin,
                            streamerLogin = subscription.twitchLogin,
                            thumbnailUrl = streamData?.thumbnailUrl
                        )

                        if (messageId != null) {
                            logger.info("EventSub: Notification sent successfully, messageId=$messageId")
                            // Update stream with message info
                            streamRepo.save(
                                saved.copy(
                                    telegramMessageId = messageId,
                                    telegramChatId = subscription.chatId,
                                    viewerCount = streamData?.viewerCount ?: 0
                                )
                            )
                        } else {
                            logger.warn("EventSub: Notification service returned null messageId for subscription ${subscription.id}")
                        }
                    } catch (e: Exception) {
                        logger.error("EventSub: Failed to send stream start notification for subscription ${subscription.id}", e)
                    }
                }
            } catch (e: Exception) {
                logger.error("EventSub: Failed to handle stream online for subscription ${subscription.id}", e)
            }
        }
    }

    /**
     * Handle stream.offline event
     */
    @Transactional
    fun handleStreamOffline(broadcasterId: String) {
        logger.info("EventSub: Stream offline event received - broadcasterId=$broadcasterId")

        val subscriptions = channelRepo.findByTwitchChannelId(broadcasterId)
        if (subscriptions.isEmpty()) {
            logger.warn("EventSub: No subscriptions found for broadcaster: $broadcasterId")
            return
        }

        logger.info("EventSub: Found ${subscriptions.size} subscription(s) for broadcaster $broadcasterId")

        subscriptions.forEach { subscription ->
            try {
                logger.info("EventSub: Processing offline event for subscription ${subscription.id}")

                val stream = streamRepo.findActiveBySubscriptionId(subscription.id!!)
                if (stream == null) {
                    logger.info("EventSub: No active stream found for subscription ${subscription.id}, skipping")
                    return@forEach
                }

                // Update stream status
                val updated = stream.copy(
                    status = "ended",
                    endedAt = Instant.now()
                )
                streamRepo.save(updated)
                logger.info("EventSub: Marked stream ${updated.id} as ended")

                // Update Telegram message asynchronously
                if (updated.telegramMessageId != null && updated.telegramChatId != null) {
                    logger.info("EventSub: Updating Telegram notification for stream ${updated.id} in chat ${updated.telegramChatId}")
                    scope.launch {
                        try {
                            val timeline = timelineRepo.findByStreamId(updated.id!!)
                            notificationService.updateStreamNotification(
                                chatId = updated.telegramChatId,
                                messageId = updated.telegramMessageId,
                                stream = updated,
                                streamerName = subscription.displayName ?: subscription.twitchLogin,
                                streamerLogin = subscription.twitchLogin,
                                timeline = timeline
                            )
                            logger.info("EventSub: Successfully updated offline notification for stream ${updated.id}")
                        } catch (e: Exception) {
                            logger.error("EventSub: Failed to update stream offline notification for subscription ${subscription.id}", e)
                        }
                    }
                } else {
                    logger.warn("EventSub: Stream ${updated.id} has no Telegram message info (messageId=${updated.telegramMessageId}, chatId=${updated.telegramChatId})")
                }
            } catch (e: Exception) {
                logger.error("EventSub: Failed to handle stream offline for subscription ${subscription.id}", e)
            }
        }
    }
}
