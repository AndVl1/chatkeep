package ru.andvl.chatkeep.api.support

import org.springframework.stereotype.Component
import ru.andvl.chatkeep.api.auth.TelegramAuthService
import ru.andvl.chatkeep.api.dto.AddBlocklistPatternRequest
import ru.andvl.chatkeep.api.dto.ButtonDto
import ru.andvl.chatkeep.api.dto.LockDto
import ru.andvl.chatkeep.api.dto.UpdateChannelReplyRequest
import ru.andvl.chatkeep.api.dto.UpdateLocksRequest
import ru.andvl.chatkeep.api.dto.UpdateSettingsRequest
import ru.andvl.chatkeep.domain.model.ChatSettings
import ru.andvl.chatkeep.domain.model.locks.LockConfig
import ru.andvl.chatkeep.domain.model.moderation.BlocklistPattern
import ru.andvl.chatkeep.domain.model.moderation.ModerationConfig
import java.time.Instant

@Component
class TestDataFactory {

    companion object {
        const val DEFAULT_CHAT_ID = -1001234567890L
        const val SECONDARY_CHAT_ID = -1009876543210L
        const val NONEXISTENT_CHAT_ID = -1009999999999L
    }

    // Domain Models

    fun createTelegramUser(
        id: Long = 123456789L,
        firstName: String = "Test",
        lastName: String? = "User",
        username: String? = "testuser",
        photoUrl: String? = null,
        authDate: Long = Instant.now().epochSecond
    ) = TelegramAuthService.TelegramUser(
        id = id,
        firstName = firstName,
        lastName = lastName,
        username = username,
        photoUrl = photoUrl,
        authDate = authDate
    )

    fun createChatSettings(
        chatId: Long = DEFAULT_CHAT_ID,
        chatTitle: String? = "Test Group",
        collectionEnabled: Boolean = true,
        createdAt: Instant = Instant.now(),
        updatedAt: Instant = Instant.now()
    ) = ChatSettings(
        chatId = chatId,
        chatTitle = chatTitle,
        collectionEnabled = collectionEnabled,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    fun createModerationConfig(
        chatId: Long = DEFAULT_CHAT_ID,
        maxWarnings: Int = 3,
        warningTtlHours: Int = 24,
        thresholdAction: String = "MUTE",
        thresholdDurationMinutes: Int? = 1440,
        defaultBlocklistAction: String = "WARN",
        logChannelId: Long? = null,
        cleanServiceEnabled: Boolean = false,
        createdAt: Instant = Instant.now(),
        updatedAt: Instant = Instant.now()
    ) = ModerationConfig(
        chatId = chatId,
        maxWarnings = maxWarnings,
        warningTtlHours = warningTtlHours,
        thresholdAction = thresholdAction,
        thresholdDurationMinutes = thresholdDurationMinutes,
        defaultBlocklistAction = defaultBlocklistAction,
        logChannelId = logChannelId,
        cleanServiceEnabled = cleanServiceEnabled,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    fun createBlocklistPattern(
        chatId: Long = DEFAULT_CHAT_ID,
        pattern: String = "badword",
        matchType: String = "CONTAINS",
        action: String = "WARN",
        actionDurationMinutes: Int? = null,
        severity: Int = 1,
        createdAt: Instant = Instant.now()
    ) = BlocklistPattern(
        chatId = chatId,
        pattern = pattern,
        matchType = matchType,
        action = action,
        actionDurationMinutes = actionDurationMinutes,
        severity = severity,
        createdAt = createdAt
    )

    fun createLockConfig(
        locked: Boolean = true,
        reason: String? = null
    ) = LockConfig(
        locked = locked,
        reason = reason
    )

    // Request DTOs

    fun createUpdateSettingsRequest(
        collectionEnabled: Boolean? = null,
        cleanServiceEnabled: Boolean? = null,
        maxWarnings: Int? = null,
        warningTtlHours: Int? = null,
        thresholdAction: String? = null,
        thresholdDurationMinutes: Int? = null,
        defaultBlocklistAction: String? = null,
        logChannelId: Long? = null
    ) = UpdateSettingsRequest(
        collectionEnabled = collectionEnabled,
        cleanServiceEnabled = cleanServiceEnabled,
        maxWarnings = maxWarnings,
        warningTtlHours = warningTtlHours,
        thresholdAction = thresholdAction,
        thresholdDurationMinutes = thresholdDurationMinutes,
        defaultBlocklistAction = defaultBlocklistAction,
        logChannelId = logChannelId
    )

    fun createAddBlocklistPatternRequest(
        pattern: String = "test pattern",
        matchType: String? = "CONTAINS",
        action: String? = "WARN",
        actionDurationMinutes: Int? = null,
        severity: Int? = 1
    ) = AddBlocklistPatternRequest(
        pattern = pattern,
        matchType = matchType,
        action = action,
        actionDurationMinutes = actionDurationMinutes,
        severity = severity
    )

    fun createUpdateLocksRequest(
        locks: Map<String, LockDto> = emptyMap(),
        lockWarnsEnabled: Boolean? = null
    ) = UpdateLocksRequest(
        locks = locks,
        lockWarnsEnabled = lockWarnsEnabled
    )

    fun createUpdateChannelReplyRequest(
        enabled: Boolean? = null,
        replyText: String? = null,
        buttons: List<ButtonDto>? = null
    ) = UpdateChannelReplyRequest(
        enabled = enabled,
        replyText = replyText,
        buttons = buttons
    )

    fun createButtonDto(
        text: String = "Button",
        url: String = "https://example.com"
    ) = ButtonDto(
        text = text,
        url = url
    )

    fun createLockDto(
        locked: Boolean = true,
        reason: String? = null
    ) = LockDto(
        locked = locked,
        reason = reason
    )
}
