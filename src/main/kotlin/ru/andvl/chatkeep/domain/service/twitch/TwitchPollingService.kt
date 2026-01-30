package ru.andvl.chatkeep.domain.service.twitch

import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import ru.andvl.chatkeep.domain.model.twitch.StreamTimelineEvent
import ru.andvl.chatkeep.domain.model.twitch.TwitchStream
import ru.andvl.chatkeep.infrastructure.repository.twitch.StreamTimelineEventRepository
import ru.andvl.chatkeep.infrastructure.repository.twitch.TwitchChannelSubscriptionRepository
import ru.andvl.chatkeep.infrastructure.repository.twitch.TwitchStreamRepository
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class TwitchPollingService(
    private val streamRepo: TwitchStreamRepository,
    private val channelRepo: TwitchChannelSubscriptionRepository,
    private val timelineRepo: StreamTimelineEventRepository,
    private val notificationService: TwitchNotificationService,
    private val twitchApiClient: TwitchApiClient,
    private val transactionTemplate: TransactionTemplate
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @PreDestroy
    fun cleanup() {
        scope.cancel()
    }

    /**
     * Check subscribed channels for new live streams every 2 minutes.
     * This is a fallback in case EventSub doesn't fire (e.g., stream already live when subscribed).
     */
    @Scheduled(fixedDelay = 120_000, initialDelay = 30_000) // 2 minutes
    fun checkForNewStreams() {
        // Read subscriptions in transaction
        val subscriptions = transactionTemplate.execute {
            channelRepo.findAll().toList()
        } ?: emptyList()

        if (subscriptions.isEmpty()) {
            return
        }

        logger.info("Checking ${subscriptions.size} subscriptions for new streams")

        // Get all subscribed channel IDs
        val channelIds = subscriptions.map { it.twitchChannelId }

        try {
            // Fetch all live streams for subscribed channels in one API call (no transaction needed)
            val liveStreams = twitchApiClient.getStreams(channelIds)

            liveStreams.forEach { streamData ->
                // Find subscription for this channel
                val subscription = subscriptions.find { it.twitchChannelId == streamData.userId }
                    ?: return@forEach

                val subscriptionId = subscription.id ?: return@forEach

                // Check if we already have an active stream record (in transaction)
                val existingStream = transactionTemplate.execute {
                    streamRepo.findActiveBySubscriptionId(subscriptionId)
                }

                if (existingStream != null) {
                    // Stream already tracked, skip
                    return@forEach
                }

                // Check for recently ended stream (within last 5 minutes) - stream recovery
                val recentlyEnded = transactionTemplate.execute {
                    streamRepo.findRecentlyEndedBySubscriptionId(
                        subscriptionId,
                        Instant.now().minus(5, ChronoUnit.MINUTES)
                    )
                }

                // New live stream detected! Create record or recover and send notification
                logger.info("New live stream detected for ${subscription.twitchLogin} (subscription ${subscription.id}, recovered=${recentlyEnded != null})")

                // Create/recover stream and timeline event in transaction
                val saved = transactionTemplate.execute {
                    val savedStream = if (recentlyEnded != null) {
                        // Reuse existing stream record
                        logger.info("Stream recovered for subscription $subscriptionId, reusing stream ${recentlyEnded.id}")
                        streamRepo.save(
                            recentlyEnded.copy(
                                status = "live",
                                twitchStreamId = streamData.id,
                                endedAt = null,
                                currentGame = streamData.gameName,
                                currentTitle = streamData.title
                            )
                        )
                    } else {
                        // Create new stream
                        val stream = TwitchStream.createNew(
                            subscriptionId = subscriptionId,
                            twitchStreamId = streamData.id,
                            startedAt = Instant.parse(streamData.startedAt),
                            currentGame = streamData.gameName,
                            currentTitle = streamData.title,
                            hasPhoto = streamData.thumbnailUrl != null
                        )
                        streamRepo.save(stream)
                    }

                    // Create initial timeline event
                    timelineRepo.save(
                        StreamTimelineEvent.createNew(
                            streamId = savedStream.id!!,
                            streamOffsetSeconds = 0,
                            gameName = streamData.gameName,
                            streamTitle = streamData.title
                        )
                    )

                    savedStream
                }!!

                // Send notification asynchronously (only if not recovered)
                if (recentlyEnded == null) {
                    scope.launch {
                        try {
                            val result = notificationService.sendStreamStartNotification(
                                chatId = subscription.chatId,
                                stream = saved,
                                streamerName = subscription.displayName ?: subscription.twitchLogin,
                                streamerLogin = subscription.twitchLogin,
                                thumbnailUrl = streamData.thumbnailUrl,
                                isPinned = subscription.isPinned,
                                pinSilently = subscription.pinSilently
                            )

                            if (result != null) {
                                val (messageId, hasPhoto) = result
                                // Update stream with message ID in separate transaction
                                transactionTemplate.execute {
                                    streamRepo.save(
                                        saved.copy(
                                            telegramMessageId = messageId,
                                            telegramChatId = subscription.chatId,
                                            viewerCount = streamData.viewerCount,
                                            hasPhoto = hasPhoto
                                        )
                                    )
                                }
                                logger.info("Sent notification for new stream ${saved.id}, messageId=$messageId, hasPhoto=$hasPhoto")
                            }
                        } catch (e: Exception) {
                            logger.error("Failed to send notification for new stream ${saved.id}", e)
                        }
                    }
                } else {
                    logger.info("Stream recovered, skipping notification send (reusing existing message)")
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to check for new streams", e)
        }
    }

    /**
     * Poll active streams every 3 minutes to check for updates.
     * Optimized to make ONE Twitch API call for all unique channels.
     */
    @Scheduled(fixedDelay = 180_000, initialDelay = 60_000) // 3 minutes
    fun pollActiveStreams() {
        // Read active streams in transaction
        val activeStreams = transactionTemplate.execute {
            streamRepo.findAllActive()
        } ?: emptyList()

        if (activeStreams.isEmpty()) {
            return
        }

        logger.info("Polling ${activeStreams.size} active streams")

        // Load all subscriptions for active streams in transaction
        val subscriptionIds = activeStreams.map { it.subscriptionId }.distinct()
        val subscriptions = transactionTemplate.execute {
            subscriptionIds.mapNotNull { channelRepo.findById(it).orElse(null) }
        } ?: emptyList()

        val subscriptionMap = subscriptions.associateBy { it.id }

        // Get unique channel IDs and make ONE API call
        val uniqueChannelIds = subscriptions.map { it.twitchChannelId }.distinct()

        if (uniqueChannelIds.isEmpty()) {
            return
        }

        logger.info("Fetching data for ${uniqueChannelIds.size} unique channels (from ${activeStreams.size} streams)")

        try {
            // ONE API call for all channels (no transaction needed)
            val liveStreamsData = twitchApiClient.getStreams(uniqueChannelIds)
            val streamDataByChannelId = liveStreamsData.associateBy { it.userId }

            // Process each active stream
            activeStreams.forEach { stream ->
                try {
                    val subscription = subscriptionMap[stream.subscriptionId] ?: return@forEach
                    val streamData = streamDataByChannelId[subscription.twitchChannelId]
                    val streamerName = subscription.displayName ?: subscription.twitchLogin
                    val streamerLogin = subscription.twitchLogin

                    if (streamData == null) {
                        // Stream ended but EventSub didn't fire
                        handleStreamEnded(stream, streamerName, streamerLogin)
                    } else {
                        // Check for game/title changes
                        val gameChanged = stream.currentGame != streamData.gameName
                        val titleChanged = stream.currentTitle != streamData.title

                        if (gameChanged || titleChanged) {
                            // Game/title changed - add timeline event and update message
                            handleStreamUpdate(stream, streamData, streamerName, streamerLogin, streamData.thumbnailUrl)
                        } else {
                            // No game/title change - still update viewer count and duration
                            updateViewerCountAndDuration(stream, streamData, streamerName, streamerLogin, streamData.thumbnailUrl)
                        }
                    }
                } catch (e: Exception) {
                    logger.error("Failed to process stream ${stream.id}", e)
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to fetch stream data from Twitch API", e)
        }
    }

    /**
     * Update viewer count and duration without adding timeline event
     */
    private fun updateViewerCountAndDuration(stream: ru.andvl.chatkeep.domain.model.twitch.TwitchStream, streamData: TwitchStreamData, streamerName: String, streamerLogin: String, thumbnailUrl: String?) {
        // Update stream with new viewer count in transaction
        val updated = transactionTemplate.execute {
            val updatedStream = stream.copy(viewerCount = streamData.viewerCount)
            streamRepo.save(updatedStream)
        }!!

        // Update Telegram message asynchronously (viewer count and duration will be refreshed)
        if (stream.telegramMessageId != null && stream.telegramChatId != null) {
            scope.launch {
                try {
                    val timeline = transactionTemplate.execute {
                        timelineRepo.findByStreamId(stream.id!!)
                    } ?: emptyList()

                    val telegraphUrl = notificationService.updateStreamNotification(
                        chatId = stream.telegramChatId,
                        messageId = stream.telegramMessageId,
                        stream = updated,
                        streamerName = streamerName,
                        streamerLogin = streamerLogin,
                        timeline = timeline,
                        thumbnailUrl = thumbnailUrl
                    )

                    // Store Telegraph URL if created
                    if (telegraphUrl != null && stream.telegraphUrl != telegraphUrl) {
                        transactionTemplate.execute {
                            streamRepo.save(updated.copy(telegraphUrl = telegraphUrl))
                        }
                    }

                    logger.debug("Updated viewer count/duration for stream ${stream.id}: viewers=${streamData.viewerCount}")
                } catch (e: Exception) {
                    logger.error("Failed to update stream notification for stream ${stream.id}", e)
                }
            }
        }
    }

    private fun handleStreamUpdate(stream: ru.andvl.chatkeep.domain.model.twitch.TwitchStream, streamData: TwitchStreamData, streamerName: String, streamerLogin: String, thumbnailUrl: String?) {
        // Update stream and add timeline event in transaction
        val updated = transactionTemplate.execute {
            val updatedStream = stream.copy(
                currentGame = streamData.gameName,
                currentTitle = streamData.title,
                viewerCount = streamData.viewerCount
            )
            streamRepo.save(updatedStream)

            // Add timeline event
            val offsetSeconds = Duration.between(stream.startedAt, Instant.now()).seconds.toInt()
            timelineRepo.save(
                StreamTimelineEvent.createNew(
                    streamId = stream.id!!,
                    streamOffsetSeconds = offsetSeconds,
                    gameName = streamData.gameName,
                    streamTitle = streamData.title
                )
            )

            updatedStream
        }!!

        // Update Telegram message asynchronously
        if (stream.telegramMessageId != null && stream.telegramChatId != null) {
            scope.launch {
                try {
                    val timeline = transactionTemplate.execute {
                        timelineRepo.findByStreamId(stream.id!!)
                    } ?: emptyList()

                    val telegraphUrl = notificationService.updateStreamNotification(
                        chatId = stream.telegramChatId,
                        messageId = stream.telegramMessageId,
                        stream = updated,
                        streamerName = streamerName,
                        streamerLogin = streamerLogin,
                        timeline = timeline,
                        thumbnailUrl = thumbnailUrl
                    )

                    // Store Telegraph URL if created
                    if (telegraphUrl != null && stream.telegraphUrl != telegraphUrl) {
                        transactionTemplate.execute {
                            streamRepo.save(updated.copy(telegraphUrl = telegraphUrl))
                        }
                    }
                } catch (e: Exception) {
                    logger.error("Failed to update stream notification for stream ${stream.id}", e)
                }
            }
        }

        logger.info("Updated stream ${stream.id}: game=${streamData.gameName}, title=${streamData.title}")
    }

    private fun handleStreamEnded(stream: ru.andvl.chatkeep.domain.model.twitch.TwitchStream, streamerName: String, streamerLogin: String) {
        // Update stream status in transaction
        val updated = transactionTemplate.execute {
            val updatedStream = stream.copy(
                status = "ended",
                endedAt = Instant.now()
            )
            streamRepo.save(updatedStream)
        }!!

        // Update Telegram message asynchronously with Telegraph button if URL was stored
        if (stream.telegramMessageId != null && stream.telegramChatId != null) {
            scope.launch {
                try {
                    val timeline = transactionTemplate.execute {
                        timelineRepo.findByStreamId(stream.id!!)
                    } ?: emptyList()

                    val telegraphUrl = notificationService.updateStreamNotification(
                        chatId = stream.telegramChatId,
                        messageId = stream.telegramMessageId,
                        stream = updated,
                        streamerName = streamerName,
                        streamerLogin = streamerLogin,
                        timeline = timeline
                    )

                    // Store Telegraph URL if created (for ended stream)
                    if (telegraphUrl != null && stream.telegraphUrl != telegraphUrl) {
                        transactionTemplate.execute {
                            streamRepo.save(updated.copy(telegraphUrl = telegraphUrl))
                        }
                    }
                } catch (e: Exception) {
                    logger.error("Failed to update stream ended notification for stream ${stream.id}", e)
                }
            }
        }

        logger.info("Stream ended (detected by polling): ${stream.id}")
    }
}
