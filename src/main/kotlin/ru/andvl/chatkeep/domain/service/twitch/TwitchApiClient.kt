package ru.andvl.chatkeep.domain.service.twitch

import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import ru.andvl.chatkeep.config.TwitchProperties
import java.time.Instant
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@Component
class TwitchApiClient(
    private val properties: TwitchProperties
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val lock = ReentrantLock()

    private var cachedToken: String? = null
    private var tokenExpiresAt: Instant? = null

    private val restClient = RestClient.builder()
        .baseUrl("https://api.twitch.tv/helix")
        .build()

    /**
     * Get app access token using Client Credentials flow
     */
    private fun getAppAccessToken(): String {
        lock.withLock {
            // Return cached token if still valid
            val expiresAt = tokenExpiresAt
            if (cachedToken != null && expiresAt != null && Instant.now().isBefore(expiresAt.minusSeconds(60))) {
                return cachedToken!!
            }

            logger.info("Requesting new Twitch app access token")

            try {
                val response = RestClient.create()
                    .post()
                    .uri("https://id.twitch.tv/oauth2/token?client_id=${properties.clientId}&client_secret=${properties.clientSecret}&grant_type=client_credentials")
                    .contentType(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(TwitchTokenResponse::class.java)
                    ?: throw RuntimeException("Failed to get Twitch access token")

                cachedToken = response.accessToken
                tokenExpiresAt = Instant.now().plusSeconds(response.expiresIn.toLong())

                logger.info("Twitch token refreshed successfully, expires in ${response.expiresIn} seconds")
                return response.accessToken
            } catch (e: HttpClientErrorException) {
                logger.error("Twitch auth API error: status=${e.statusCode}, body=${e.responseBodyAsString}")
                throw RuntimeException("Failed to obtain Twitch access token: ${e.statusCode.value()}", e)
            } catch (e: RestClientException) {
                logger.error("Network error during Twitch authentication", e)
                throw RuntimeException("Failed to connect to Twitch authentication API", e)
            }
        }
    }

    /**
     * Search channels by query
     */
    fun searchChannels(query: String): List<TwitchSearchChannel> {
        val token = getAppAccessToken()

        return try {
            restClient.get()
                .uri("/search/channels?query={query}&first=10", query)
                .header("Authorization", "Bearer $token")
                .header("Client-Id", properties.clientId)
                .retrieve()
                .body(TwitchSearchChannelsResponse::class.java)
                ?.data ?: emptyList()
        } catch (e: HttpClientErrorException) {
            logger.error("Twitch API error during search: status=${e.statusCode}, body=${e.responseBodyAsString}")
            throw RuntimeException("Failed to search Twitch channels: ${e.statusCode.value()}", e)
        } catch (e: RestClientException) {
            logger.error("Network error during Twitch search", e)
            throw RuntimeException("Failed to connect to Twitch API", e)
        }
    }

    /**
     * Get user info by login
     */
    fun getUserByLogin(login: String): TwitchUser? {
        val token = getAppAccessToken()

        return try {
            restClient.get()
                .uri("/users?login={login}", login)
                .header("Authorization", "Bearer $token")
                .header("Client-Id", properties.clientId)
                .retrieve()
                .body(TwitchUsersResponse::class.java)
                ?.data?.firstOrNull()
        } catch (e: HttpClientErrorException) {
            logger.error("Twitch API error fetching user '$login': status=${e.statusCode}, body=${e.responseBodyAsString}")
            throw RuntimeException("Failed to fetch Twitch user: ${e.statusCode.value()}", e)
        } catch (e: RestClientException) {
            logger.error("Network error fetching Twitch user '$login'", e)
            throw RuntimeException("Failed to connect to Twitch API", e)
        }
    }

    /**
     * Get stream info by user IDs
     */
    fun getStreams(userIds: List<String>): List<TwitchStreamData> {
        if (userIds.isEmpty()) return emptyList()

        val token = getAppAccessToken()
        val userIdParams = userIds.joinToString("&") { "user_id=$it" }

        return try {
            restClient.get()
                .uri("/streams?$userIdParams")
                .header("Authorization", "Bearer $token")
                .header("Client-Id", properties.clientId)
                .retrieve()
                .body(TwitchStreamsResponse::class.java)
                ?.data ?: emptyList()
        } catch (e: HttpClientErrorException) {
            logger.error("Twitch API error fetching streams: status=${e.statusCode}, body=${e.responseBodyAsString}")
            throw RuntimeException("Failed to fetch Twitch streams: ${e.statusCode.value()}", e)
        } catch (e: RestClientException) {
            logger.error("Network error fetching Twitch streams", e)
            throw RuntimeException("Failed to connect to Twitch API", e)
        }
    }

    /**
     * Create EventSub subscription
     */
    fun createEventSubSubscription(type: String, version: String, condition: Map<String, String>): TwitchEventSubSubscription {
        val token = getAppAccessToken()

        val request = TwitchEventSubCreateRequest(
            type = type,
            version = version,
            condition = condition,
            transport = TwitchEventSubTransport(
                method = "webhook",
                callback = properties.webhookUrl,
                secret = properties.webhookSecret
            )
        )

        logger.info("Creating EventSub subscription: type=$type, condition=$condition")

        val response = restClient.post()
            .uri("/eventsub/subscriptions")
            .header("Authorization", "Bearer $token")
            .header("Client-Id", properties.clientId)
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .body(TwitchEventSubResponse::class.java)
            ?: throw RuntimeException("Failed to create EventSub subscription")

        return response.data.first()
    }

    /**
     * Delete EventSub subscription
     */
    fun deleteEventSubSubscription(subscriptionId: String) {
        val token = getAppAccessToken()

        logger.info("Deleting EventSub subscription: $subscriptionId")

        restClient.delete()
            .uri("/eventsub/subscriptions?id={id}", subscriptionId)
            .header("Authorization", "Bearer $token")
            .header("Client-Id", properties.clientId)
            .retrieve()
            .toBodilessEntity()
    }
}
