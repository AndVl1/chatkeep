package ru.andvl.chatkeep.api.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import java.time.Instant

@Schema(description = "Chat summary information")
data class ChatSummaryResponse(
    @Schema(description = "Telegram chat ID", example = "-1001234567890")
    val chatId: Long,

    @Schema(description = "Chat title", example = "My Group Chat", nullable = true)
    val chatTitle: String?,

    @Schema(description = "Number of members in the chat", example = "42", nullable = true)
    val memberCount: Int?,

    @Schema(description = "Whether the bot has admin rights", example = "true")
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

@Schema(description = "JWT authentication token response")
data class TokenResponse(
    @Schema(description = "JWT token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    val token: String,

    @Schema(description = "Token expiration time in seconds", example = "86400")
    val expiresIn: Long,

    @Schema(description = "Authenticated user information")
    val user: TelegramUserResponse
)

@Schema(description = "Telegram user information")
data class TelegramUserResponse(
    @Schema(description = "Telegram user ID", example = "123456789")
    val id: Long,

    @Schema(description = "User first name", example = "John")
    val firstName: String,

    @Schema(description = "User last name", example = "Doe", nullable = true)
    val lastName: String? = null,

    @Schema(description = "Telegram username", example = "johndoe", nullable = true)
    val username: String? = null,

    @Schema(description = "Profile photo URL", example = "https://t.me/i/userpic/320/...", nullable = true)
    val photoUrl: String? = null
)

data class UserPreferencesResponse(
    val userId: Long,
    val locale: String
)
