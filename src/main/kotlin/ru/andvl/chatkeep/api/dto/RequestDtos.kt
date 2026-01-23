package ru.andvl.chatkeep.api.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class UpdateSettingsRequest(
    val collectionEnabled: Boolean? = null,
    val cleanServiceEnabled: Boolean? = null,
    val lockWarnsEnabled: Boolean? = null,

    @field:Min(1)
    @field:Max(20)
    val maxWarnings: Int? = null,

    @field:Min(1)
    @field:Max(168)
    val warningTtlHours: Int? = null,

    val thresholdAction: String? = null,
    val thresholdDurationMinutes: Int? = null,
    val defaultBlocklistAction: String? = null,
    val logChannelId: Long? = null
)

data class UpdateLocksRequest(
    val locks: Map<String, LockDto>,
    val lockWarnsEnabled: Boolean? = null
)

data class AddBlocklistPatternRequest(
    @field:NotBlank
    @field:Size(max = 500)
    val pattern: String,

    val matchType: String? = null,
    val action: String? = null,
    val actionDurationMinutes: Int? = null,

    @field:Min(1)
    @field:Max(10)
    val severity: Int? = null
)

data class UpdateChannelReplyRequest(
    val enabled: Boolean? = null,

    @field:Size(max = 4096)
    val replyText: String? = null,

    @field:Valid
    val buttons: List<ButtonDto>? = null
)

data class UpdatePreferencesRequest(
    @field:NotBlank
    @field:Pattern(regexp = "^(en|ru)$", message = "Locale must be 'en' or 'ru'")
    val locale: String
)

// Moderation actions
data class WarnRequest(
    val userId: Long,
    val reason: String? = null
)

data class MuteRequest(
    val userId: Long,
    val durationMinutes: Int? = null,
    val reason: String? = null
)

data class BanRequest(
    val userId: Long,
    val durationMinutes: Int? = null,
    val reason: String? = null
)

data class KickRequest(
    val userId: Long,
    val reason: String? = null
)

// Welcome/Goodbye
data class UpdateWelcomeRequest(
    val enabled: Boolean? = null,

    @field:Size(max = 4096)
    val messageText: String? = null,

    val sendToChat: Boolean? = null,
    val deleteAfterSeconds: Int? = null
)

// Rules
data class UpdateRulesRequest(
    @field:NotBlank
    @field:Size(max = 10000)
    val rulesText: String
)

// Notes
data class CreateNoteRequest(
    @field:NotBlank
    @field:Size(max = 255)
    val noteName: String,

    @field:NotBlank
    @field:Size(max = 4096)
    val content: String
)

data class UpdateNoteRequest(
    @field:NotBlank
    @field:Size(max = 4096)
    val content: String
)

// Anti-flood
data class UpdateAntifloodRequest(
    val enabled: Boolean? = null,

    @field:Min(1)
    @field:Max(100)
    val maxMessages: Int? = null,

    @field:Min(1)
    @field:Max(60)
    val timeWindowSeconds: Int? = null,

    val action: String? = null,
    val actionDurationMinutes: Int? = null
)
