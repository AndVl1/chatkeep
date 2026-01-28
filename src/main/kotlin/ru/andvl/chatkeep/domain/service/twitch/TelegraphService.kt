package ru.andvl.chatkeep.domain.service.twitch

import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import ru.andvl.chatkeep.domain.model.twitch.*
import java.time.Duration

@Service
class TelegraphService(
    @Value("\${telegram.telegraph.token:}") private val telegraphToken: String,
    private val webClient: WebClient
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val json = Json { ignoreUnknownKeys = true }

    // Store page paths for streams: streamerId -> path
    private val pageCache = mutableMapOf<String, String>()

    companion object {
        private const val TELEGRAPH_API_BASE = "https://api.telegra.ph"
    }

    /**
     * Create or update Telegraph page with stream timeline
     * Returns telegra.ph URL or null if failed
     */
    suspend fun createOrUpdateTimelinePage(
        streamerId: String,
        streamerName: String,
        timeline: List<StreamTimelineEvent>
    ): String? {
        if (telegraphToken.isBlank()) {
            logger.warn("Telegraph token not configured, skipping timeline page creation")
            return null
        }

        if (timeline.isEmpty()) {
            logger.debug("Timeline is empty, skipping Telegraph page creation")
            return null
        }

        return try {
            val title = "Таймлайн стрима: $streamerName"
            val content = buildTimelineContent(timeline)

            val existingPath = pageCache[streamerId]

            val url = if (existingPath != null) {
                // Update existing page
                editPage(existingPath, title, content)?.url
            } else {
                // Create new page
                val page = createPage(title, content)
                page?.let {
                    pageCache[streamerId] = it.path
                    it.url
                }
            }

            logger.info("Telegraph page for streamer $streamerId: $url")
            url
        } catch (e: Exception) {
            logger.error("Failed to create/update Telegraph page for streamer $streamerId", e)
            null
        }
    }

    /**
     * Create new Telegraph page
     */
    private suspend fun createPage(title: String, content: List<NodeElement>): TelegraphPage? {
        return try {
            val request = CreatePageRequest(
                access_token = telegraphToken,
                title = title,
                content = content,
                author_name = "Chatkeep Bot"
            )

            val response = webClient.post()
                .uri("$TELEGRAPH_API_BASE/createPage")
                .bodyValue(request)
                .retrieve()
                .awaitBody<TelegraphResponse<TelegraphPage>>()

            if (response.ok && response.result != null) {
                response.result
            } else {
                logger.error("Telegraph API error on createPage: ${response.error}")
                null
            }
        } catch (e: Exception) {
            logger.error("Failed to call Telegraph createPage API", e)
            null
        }
    }

    /**
     * Edit existing Telegraph page
     */
    private suspend fun editPage(
        path: String,
        title: String,
        content: List<NodeElement>
    ): TelegraphPage? {
        return try {
            val request = EditPageRequest(
                access_token = telegraphToken,
                path = path,
                title = title,
                content = content,
                author_name = "Chatkeep Bot"
            )

            val response = webClient.post()
                .uri("$TELEGRAPH_API_BASE/editPage/$path")
                .bodyValue(request)
                .retrieve()
                .awaitBody<TelegraphResponse<TelegraphPage>>()

            if (response.ok && response.result != null) {
                response.result
            } else {
                logger.error("Telegraph API error on editPage: ${response.error}")
                null
            }
        } catch (e: Exception) {
            logger.error("Failed to call Telegraph editPage API", e)
            null
        }
    }

    /**
     * Build Telegraph content from timeline events
     */
    private fun buildTimelineContent(timeline: List<StreamTimelineEvent>): List<NodeElement> {
        val content = mutableListOf<NodeElement>()

        // Add header
        content.add(NodeElement.Tag("h3", children = listOf(
            NodeElement.Text("Таймлайн стрима")
        )))

        // Add each timeline entry
        timeline.forEach { event ->
            val time = formatSeconds(event.streamOffsetSeconds)
            val game = event.gameName ?: "Just Chatting"
            val title = event.streamTitle ?: ""

            // Entry paragraph
            content.add(NodeElement.Tag("p", children = listOf(
                NodeElement.Tag("b", children = listOf(NodeElement.Text(time))),
                NodeElement.Text(" - $game")
            )))

            // Title as separate paragraph (if exists)
            if (title.isNotBlank()) {
                content.add(NodeElement.Tag("p", children = listOf(
                    NodeElement.Text(title)
                )))
            }
        }

        return content
    }

    /**
     * Format seconds as HH:MM or MM:SS
     */
    private fun formatSeconds(seconds: Int): String {
        val duration = Duration.ofSeconds(seconds.toLong())
        val hours = duration.toHours()
        val minutes = (duration.toMinutes() % 60)
        val secs = (duration.seconds % 60)

        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format("%02d:%02d", minutes, secs)
        }
    }

    /**
     * Clear page cache for a streamer (e.g., when stream ends)
     */
    fun clearPageCache(streamerId: String) {
        pageCache.remove(streamerId)
    }
}
