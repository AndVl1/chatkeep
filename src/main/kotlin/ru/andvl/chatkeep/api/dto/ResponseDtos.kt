package ru.andvl.chatkeep.api.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import java.time.Instant

data class ChatSummaryResponse(
    val chatId: Long,
    val chatTitle: String?,
    val memberCount: Int?,
    val isBotAdmin: Boolean = false
)

data class SettingsResponse(
    val chatId: Long,
    val chatTitle: String?,
    val collectionEnabled: Boolean,
    val cleanServiceEnabled: Boolean,
    val maxWarnings: Int,
    val warningTtlHours: Int,
    val thresholdAction: String,
    val thresholdDurationMinutes: Int?,
    val defaultBlocklistAction: String,
    val logChannelId: Long?,
    val lockWarnsEnabled: Boolean,
    val locale: String
)

data class LocksResponse(
    val chatId: Long,
    val locks: Map<String, LockDto>,
    val lockWarnsEnabled: Boolean
)

data class LockDto(
    val locked: Boolean,
    val reason: String?
)

data class BlocklistPatternResponse(
    val id: Long,
    val pattern: String,
    val matchType: String,
    val action: String,
    val actionDurationMinutes: Int?,
    val severity: Int,
    val createdAt: Instant?
)

data class ChannelReplyResponse(
    val enabled: Boolean,
    val replyText: String?,
    val mediaFileId: String?,
    val buttons: List<ButtonDto>
)

data class ButtonDto(
    val text: String,

    @field:Pattern(
        regexp = "^(https?://|tg://).*",
        message = "URL must start with https://, http://, or tg://"
    )
    val url: String
)

data class ErrorResponse(
    val code: String,
    val message: String,
    val details: Map<String, Any>? = null
)

data class TelegramLoginRequest(
    @field:NotNull
    @field:Positive
    val id: Long,

    @field:NotBlank
    @JsonProperty("first_name")
    val firstName: String,

    @JsonProperty("last_name")
    val lastName: String? = null,

    val username: String? = null,

    @JsonProperty("photo_url")
    val photoUrl: String? = null,

    @field:NotNull
    @field:Positive
    @JsonProperty("auth_date")
    val authDate: Long,

    @field:NotBlank
    val hash: String
)

data class TokenResponse(
    val token: String,
    val expiresIn: Long,
    val user: TelegramUserResponse
)

data class TelegramUserResponse(
    val id: Long,
    val firstName: String,
    val lastName: String? = null,
    val username: String? = null,
    val photoUrl: String? = null
)

data class UserPreferencesResponse(
    val userId: Long,
    val locale: String
)
