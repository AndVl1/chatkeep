package ru.andvl.chatkeep.domain.service.twitch

import com.fasterxml.jackson.annotation.JsonProperty

// Auth
data class TwitchTokenResponse(
    @JsonProperty("access_token")
    val accessToken: String,
    @JsonProperty("expires_in")
    val expiresIn: Int,
    @JsonProperty("token_type")
    val tokenType: String
)

// User/Channel search
data class TwitchUsersResponse(
    val data: List<TwitchUser>
)

data class TwitchUser(
    val id: String,
    val login: String,
    @JsonProperty("display_name")
    val displayName: String,
    @JsonProperty("profile_image_url")
    val profileImageUrl: String?,
    val description: String?
)

data class TwitchSearchChannelsResponse(
    val data: List<TwitchSearchChannel>
)

data class TwitchSearchChannel(
    val id: String,
    val broadcaster_login: String,
    val display_name: String,
    val thumbnail_url: String?,
    val is_live: Boolean
)

// Streams
data class TwitchStreamsResponse(
    val data: List<TwitchStreamData>
)

data class TwitchStreamData(
    val id: String,
    @JsonProperty("user_id")
    val userId: String,
    @JsonProperty("user_login")
    val userLogin: String,
    @JsonProperty("user_name")
    val userName: String,
    @JsonProperty("game_id")
    val gameId: String?,
    @JsonProperty("game_name")
    val gameName: String?,
    val title: String,
    @JsonProperty("viewer_count")
    val viewerCount: Int,
    @JsonProperty("started_at")
    val startedAt: String,
    @JsonProperty("thumbnail_url")
    val thumbnailUrl: String
)

// EventSub
data class TwitchEventSubCreateRequest(
    val type: String,
    val version: String,
    val condition: Map<String, String>,
    val transport: TwitchEventSubTransport
)

data class TwitchEventSubTransport(
    val method: String,
    val callback: String? = null, // Not included in webhook callbacks
    val secret: String? = null // Secret is only returned when creating subscription, not in webhooks
)

data class TwitchEventSubResponse(
    val data: List<TwitchEventSubSubscription>,
    val total: Int? = null,
    val max_total_cost: Int? = null,
    val total_cost: Int? = null
)

data class TwitchEventSubSubscription(
    val id: String,
    val status: String,
    val type: String,
    val version: String,
    val condition: Map<String, Any>,
    val transport: TwitchEventSubTransport,
    val created_at: String,
    val cost: Int? = null
)

// EventSub webhook payload
data class TwitchEventSubWebhook(
    val subscription: TwitchEventSubSubscription,
    val event: Map<String, Any>?,
    val challenge: String? = null
)
