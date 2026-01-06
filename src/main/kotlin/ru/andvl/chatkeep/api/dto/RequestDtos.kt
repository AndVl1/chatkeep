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
