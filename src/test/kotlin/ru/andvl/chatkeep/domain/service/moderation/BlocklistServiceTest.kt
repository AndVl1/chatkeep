package ru.andvl.chatkeep.domain.service.moderation

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import ru.andvl.chatkeep.domain.model.moderation.BlocklistPattern
import ru.andvl.chatkeep.domain.model.moderation.MatchType
import ru.andvl.chatkeep.domain.model.moderation.ModerationConfig
import ru.andvl.chatkeep.domain.model.moderation.PunishmentType
import ru.andvl.chatkeep.infrastructure.repository.moderation.BlocklistPatternRepository
import ru.andvl.chatkeep.infrastructure.repository.moderation.ModerationConfigRepository
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for BlocklistService.
 * Critical coverage: Pattern matching (exact + wildcard).
 */
class BlocklistServiceTest {

    private lateinit var repository: BlocklistPatternRepository
    private lateinit var configRepository: ModerationConfigRepository
    private lateinit var service: BlocklistService

    @BeforeEach
    fun setup() {
        repository = mockk()
        configRepository = mockk()
        service = BlocklistService(repository, configRepository)
    }

    // EXACT MATCH TESTS

    @Test
    fun `checkMessage should match exact pattern when text contains it`() {
        // Given
        val chatId = 123L
        val pattern = createPattern("spam", MatchType.EXACT, severity = 10)
        every { repository.findByChatIdOrGlobal(chatId) } returns listOf(pattern)

        // When
        val result = service.checkMessage(chatId, "This is spam message")

        // Then
        assertNotNull(result)
        assertEquals("spam", result.pattern.pattern)
        assertEquals(PunishmentType.BAN, result.action)
    }

    @Test
    fun `checkMessage should match exact pattern case-insensitively`() {
        // Given
        val chatId = 123L
        val pattern = createPattern("SPAM", MatchType.EXACT, severity = 10)
        every { repository.findByChatIdOrGlobal(chatId) } returns listOf(pattern)

        // When
        val result = service.checkMessage(chatId, "this is spam in lowercase")

        // Then
        assertNotNull(result, "Should match regardless of case")
        assertEquals("SPAM", result.pattern.pattern)
    }

    @Test
    fun `checkMessage should not match exact pattern when not present`() {
        // Given
        val chatId = 123L
        val pattern = createPattern("spam", MatchType.EXACT, severity = 10)
        every { repository.findByChatIdOrGlobal(chatId) } returns listOf(pattern)

        // When
        val result = service.checkMessage(chatId, "This is a clean message")

        // Then
        assertNull(result, "Should not match when pattern is not in text")
    }

    @ParameterizedTest
    @CsvSource(
        "viagra,  Buy viagra now!,        true",
        "viagra,  VIAGRA pills cheap,      true",
        "viagra,  This is clean,           false",
        "scam,    This is a scam link,     true",
        "scam,    Legitimate message,      false",
        "http://,  Visit http://evil.com,  true",
        "http://,  Just regular text,      false"
    )
    fun `checkMessage exact matching scenarios`(
        pattern: String,
        message: String,
        shouldMatch: Boolean
    ) {
        // Given
        val chatId = 123L
        val blocklistPattern = createPattern(pattern, MatchType.EXACT, severity = 5)
        every { repository.findByChatIdOrGlobal(chatId) } returns listOf(blocklistPattern)

        // When
        val result = service.checkMessage(chatId, message)

        // Then
        if (shouldMatch) {
            assertNotNull(result, "Pattern '$pattern' should match '$message'")
        } else {
            assertNull(result, "Pattern '$pattern' should NOT match '$message'")
        }
    }

    // WILDCARD MATCH TESTS

    @Test
    fun `checkMessage should match wildcard pattern with asterisk prefix`() {
        // Given
        val chatId = 123L
        val pattern = createPattern("*spam", MatchType.WILDCARD, severity = 8)
        every { repository.findByChatIdOrGlobal(chatId) } returns listOf(pattern)

        // When
        val result1 = service.checkMessage(chatId, "This is spam")
        val result2 = service.checkMessage(chatId, "notspam")
        val result3 = service.checkMessage(chatId, "antispam filter")

        // Then
        assertNotNull(result1, "*spam should match 'is spam'")
        assertNotNull(result2, "*spam should match 'notspam'")
        assertNotNull(result3, "*spam should match 'antispam filter'")
    }

    @Test
    fun `checkMessage should match wildcard pattern with asterisk suffix`() {
        // Given
        val chatId = 123L
        val pattern = createPattern("bad*", MatchType.WILDCARD, severity = 7)
        every { repository.findByChatIdOrGlobal(chatId) } returns listOf(pattern)

        // When
        val result1 = service.checkMessage(chatId, "bad word here")
        val result2 = service.checkMessage(chatId, "badness everywhere")
        val result3 = service.checkMessage(chatId, "this is bad")

        // Then
        assertNotNull(result1, "bad* should match 'bad word'")
        assertNotNull(result2, "bad* should match 'badness'")
        assertNotNull(result3, "bad* should match 'is bad'")
    }

    @Test
    fun `checkMessage should match wildcard pattern with asterisk on both sides`() {
        // Given
        val chatId = 123L
        val pattern = createPattern("*forbidden*", MatchType.WILDCARD, severity = 9)
        every { repository.findByChatIdOrGlobal(chatId) } returns listOf(pattern)

        // When
        val result1 = service.checkMessage(chatId, "forbidden content")
        val result2 = service.checkMessage(chatId, "this is forbidden here")
        val result3 = service.checkMessage(chatId, "prefix forbidden suffix")

        // Then
        assertNotNull(result1)
        assertNotNull(result2)
        assertNotNull(result3)
    }

    @ParameterizedTest
    @CsvSource(
        "*spam,         buy spam now,            true",
        "*spam,         antispam tool,           true",
        "*spam,         clean message,           false",
        "bad*,          bad word,                true",
        "bad*,          badness,                 true",
        "bad*,          not related,             false",
        "*test*,        this is a test message,  true",
        "*test*,        testing things,          true",
        "*test*,       unrelated content,       false",
        "free*money,    free money offer,        true",
        "free*money,    get freemoney now,       true",
        "free*money,    just money,              false"
    )
    fun `checkMessage wildcard matching scenarios`(
        pattern: String,
        message: String,
        shouldMatch: Boolean
    ) {
        // Given
        val chatId = 123L
        val blocklistPattern = createPattern(pattern, MatchType.WILDCARD, severity = 5)
        every { repository.findByChatIdOrGlobal(chatId) } returns listOf(blocklistPattern)

        // When
        val result = service.checkMessage(chatId, message)

        // Then
        if (shouldMatch) {
            assertNotNull(result, "Pattern '$pattern' should match '$message'")
        } else {
            assertNull(result, "Pattern '$pattern' should NOT match '$message'")
        }
    }

    @Test
    fun `checkMessage should match wildcard with question mark`() {
        // Given
        val chatId = 123L
        // ? matches exactly one character
        val pattern = createPattern("b?d", MatchType.WILDCARD, severity = 5)
        every { repository.findByChatIdOrGlobal(chatId) } returns listOf(pattern)

        // When
        val result1 = service.checkMessage(chatId, "bad word")
        val result2 = service.checkMessage(chatId, "bid is here")
        val result3 = service.checkMessage(chatId, "bd is too short")
        val result4 = service.checkMessage(chatId, "baad is too long")

        // Then
        assertNotNull(result1, "b?d should match 'bad'")
        assertNotNull(result2, "b?d should match 'bid'")
        assertNull(result3, "b?d should NOT match 'bd' (missing char)")
        assertNull(result4, "b?d should NOT match 'baad' (too many chars)")
    }

    // SEVERITY/PRIORITY TESTS

    @Test
    fun `checkMessage should return highest severity match when multiple patterns match`() {
        // Given
        val chatId = 123L
        val lowSeverity = createPattern("spam", MatchType.EXACT, PunishmentType.WARN, severity = 3)
        val mediumSeverity = createPattern("*spam*", MatchType.WILDCARD, PunishmentType.MUTE, severity = 5)
        val highSeverity = createPattern("buy", MatchType.EXACT, PunishmentType.BAN, severity = 10)

        // Repository returns patterns ordered by severity DESC
        every { repository.findByChatIdOrGlobal(chatId) } returns
            listOf(highSeverity, mediumSeverity, lowSeverity)

        // When
        val result = service.checkMessage(chatId, "buy spam pills")

        // Then
        assertNotNull(result)
        assertEquals(PunishmentType.BAN, result.action, "Should return highest severity action (BAN)")
        assertEquals("buy", result.pattern.pattern)
    }

    @Test
    fun `checkMessage should use first match when repository returns severity-ordered list`() {
        // Given
        val chatId = 123L
        val pattern1 = createPattern("test1", MatchType.EXACT, PunishmentType.BAN, severity = 10)
        val pattern2 = createPattern("test2", MatchType.EXACT, PunishmentType.MUTE, severity = 5)

        every { repository.findByChatIdOrGlobal(chatId) } returns listOf(pattern1, pattern2)

        // When - message matches both
        val result = service.checkMessage(chatId, "test1 and test2")

        // Then - should return first match (highest severity)
        assertNotNull(result)
        assertEquals("test1", result.pattern.pattern)
        assertEquals(PunishmentType.BAN, result.action)
    }

    // GLOBAL VS CHAT-SPECIFIC PATTERNS

    @Test
    fun `checkMessage should check both global and chat-specific patterns`() {
        // Given
        val chatId = 123L
        val globalPattern = createPattern("global", MatchType.EXACT, chatId = null, severity = 5)
        val chatPattern = createPattern("chat", MatchType.EXACT, chatId = 123L, severity = 5)

        every { repository.findByChatIdOrGlobal(chatId) } returns listOf(globalPattern, chatPattern)

        // When
        val result1 = service.checkMessage(chatId, "this is global pattern")
        val result2 = service.checkMessage(chatId, "this is chat pattern")

        // Then
        assertNotNull(result1, "Should match global pattern")
        assertEquals("global", result1.pattern.pattern)

        assertNotNull(result2, "Should match chat-specific pattern")
        assertEquals("chat", result2.pattern.pattern)
    }

    // DURATION TESTS

    @Test
    fun `checkMessage should return action duration from pattern`() {
        // Given
        val chatId = 123L
        val pattern = createPattern(
            "spam",
            MatchType.EXACT,
            PunishmentType.MUTE,
            durationHours = 24,
            severity = 5
        )
        every { repository.findByChatIdOrGlobal(chatId) } returns listOf(pattern)

        // When
        val result = service.checkMessage(chatId, "spam message")

        // Then
        assertNotNull(result)
        assertEquals(24, result.durationHours)
        assertEquals(PunishmentType.MUTE, result.action)
    }

    @Test
    fun `checkMessage should handle null duration for permanent punishments`() {
        // Given
        val chatId = 123L
        val pattern = createPattern(
            "severe",
            MatchType.EXACT,
            PunishmentType.BAN,
            durationHours = null,
            severity = 10
        )
        every { repository.findByChatIdOrGlobal(chatId) } returns listOf(pattern)

        // When
        val result = service.checkMessage(chatId, "severe violation")

        // Then
        assertNotNull(result)
        assertNull(result.durationHours, "Duration should be null for permanent ban")
        assertEquals(PunishmentType.BAN, result.action)
    }

    // PATTERN MANAGEMENT TESTS

    @Test
    fun `addPattern should create and save pattern`() {
        // Given
        val chatId = 123L
        val pattern = "spam"
        val savedPattern = createPattern(pattern, MatchType.EXACT, chatId = chatId, severity = 5)

        every { repository.findByChatIdAndPattern(chatId, pattern) } returns null
        every { repository.save(any()) } returns savedPattern

        // When
        val result = service.addPattern(
            chatId = chatId,
            pattern = pattern,
            matchType = MatchType.EXACT,
            action = PunishmentType.BAN,
            durationHours = 24,
            severity = 5
        )

        // Then
        verify { repository.save(match {
            it.chatId == chatId &&
            it.pattern == pattern &&
            it.matchType == MatchType.EXACT.name
        }) }
        assertNotNull(result)
        assertFalse(result.isUpdate)
    }

    @Test
    fun `addPattern should support global patterns with null chatId`() {
        // Given
        val pattern = "global-spam"
        val savedPattern = createPattern(pattern, MatchType.EXACT, chatId = null, severity = 10)

        // null chatId skips findByChatIdAndPattern
        every { repository.save(any()) } returns savedPattern

        // When
        val result = service.addPattern(
            chatId = null,
            pattern = pattern,
            matchType = MatchType.EXACT,
            action = PunishmentType.BAN,
            durationHours = null,
            severity = 10
        )

        // Then
        verify { repository.save(match { it.chatId == null && it.pattern == pattern }) }
        assertNotNull(result)
        assertFalse(result.isUpdate)
    }

    @Test
    fun `addPattern should update existing pattern instead of creating duplicate`() {
        // Given
        val chatId = 123L
        val pattern = "spam"
        val existingPattern = createPattern(
            pattern,
            MatchType.EXACT,
            chatId = chatId,
            severity = 0,
            action = PunishmentType.WARN
        )
        val updatedPattern = existingPattern.copy(
            action = PunishmentType.BAN.name,
            severity = 5
        )

        every { repository.findByChatIdAndPattern(chatId, pattern) } returns existingPattern
        every { repository.save(any()) } returns updatedPattern

        // When
        val result = service.addPattern(
            chatId = chatId,
            pattern = pattern,
            matchType = MatchType.EXACT,
            action = PunishmentType.BAN,
            durationHours = 24,
            severity = 5
        )

        // Then
        verify { repository.save(match {
            it.id == existingPattern.id &&
            it.action == PunishmentType.BAN.name &&
            it.severity == 5
        }) }
        assertNotNull(result)
        assertTrue(result.isUpdate)
    }

    @Test
    fun `removePattern should delete pattern by chatId and pattern text`() {
        // Given
        val chatId = 123L
        val pattern = "spam"

        every { repository.deleteByChatIdAndPattern(chatId, pattern) } returns Unit

        // When
        service.removePattern(chatId, pattern)

        // Then
        verify { repository.deleteByChatIdAndPattern(chatId, pattern) }
    }

    @Test
    fun `listPatterns should return patterns for specific chat`() {
        // Given
        val chatId = 123L
        val patterns = listOf(
            createPattern("spam", MatchType.EXACT, chatId = chatId, severity = 5),
            createPattern("scam", MatchType.EXACT, chatId = chatId, severity = 8)
        )

        every { repository.findByChatId(chatId) } returns patterns

        // When
        val result = service.listPatterns(chatId)

        // Then
        assertEquals(2, result.size)
        verify { repository.findByChatId(chatId) }
    }

    @Test
    fun `listGlobalPatterns should return only global patterns`() {
        // Given
        val globalPatterns = listOf(
            createPattern("global1", MatchType.EXACT, chatId = null, severity = 10),
            createPattern("global2", MatchType.WILDCARD, chatId = null, severity = 9)
        )

        every { repository.findGlobalPatterns() } returns globalPatterns

        // When
        val result = service.listGlobalPatterns()

        // Then
        assertEquals(2, result.size)
        verify { repository.findGlobalPatterns() }
    }

    // EDGE CASES

    @Test
    fun `checkMessage should return null when no patterns exist`() {
        // Given
        val chatId = 123L
        every { repository.findByChatIdOrGlobal(chatId) } returns emptyList()

        // When
        val result = service.checkMessage(chatId, "any message")

        // Then
        assertNull(result)
    }

    @Test
    fun `checkMessage should handle empty message text`() {
        // Given
        val chatId = 123L
        val pattern = createPattern("spam", MatchType.EXACT, severity = 5)
        every { repository.findByChatIdOrGlobal(chatId) } returns listOf(pattern)

        // When
        val result = service.checkMessage(chatId, "")

        // Then
        assertNull(result, "Empty message should not match any pattern")
    }

    @Test
    fun `checkMessage should handle whitespace-only message`() {
        // Given
        val chatId = 123L
        val pattern = createPattern("spam", MatchType.EXACT, severity = 5)
        every { repository.findByChatIdOrGlobal(chatId) } returns listOf(pattern)

        // When
        val result = service.checkMessage(chatId, "   \n\t  ")

        // Then
        assertNull(result, "Whitespace-only message should not match")
    }

    // DEFAULT ACTION TESTS

    @Test
    fun `getDefaultAction should return config value when exists`() {
        // Given
        val chatId = 123L
        val config = ModerationConfig(
            chatId = chatId,
            defaultBlocklistAction = "MUTE"
        )
        every { configRepository.findByChatId(chatId) } returns config

        // When
        val result = service.getDefaultAction(chatId)

        // Then
        assertEquals(PunishmentType.MUTE, result)
    }

    @Test
    fun `getDefaultAction should return WARN when no config exists`() {
        // Given
        val chatId = 123L
        every { configRepository.findByChatId(chatId) } returns null

        // When
        val result = service.getDefaultAction(chatId)

        // Then
        assertEquals(PunishmentType.WARN, result)
    }

    @Test
    fun `getDefaultAction should return NOTHING when configured`() {
        // Given
        val chatId = 123L
        val config = ModerationConfig(
            chatId = chatId,
            defaultBlocklistAction = "NOTHING"
        )
        every { configRepository.findByChatId(chatId) } returns config

        // When
        val result = service.getDefaultAction(chatId)

        // Then
        assertEquals(PunishmentType.NOTHING, result)
    }

    @Test
    fun `getDefaultAction should fallback to WARN on invalid config value`() {
        // Given
        val chatId = 123L
        val config = ModerationConfig(
            chatId = chatId,
            defaultBlocklistAction = "INVALID_ACTION"
        )
        every { configRepository.findByChatId(chatId) } returns config

        // When
        val result = service.getDefaultAction(chatId)

        // Then
        assertEquals(PunishmentType.WARN, result)
    }

    // Helper function to create test patterns
    private fun createPattern(
        pattern: String,
        matchType: MatchType,
        action: PunishmentType = PunishmentType.BAN,
        chatId: Long? = 123L,
        durationHours: Int? = 24,
        severity: Int = 5
    ) = BlocklistPattern(
        id = null,
        chatId = chatId,
        pattern = pattern,
        matchType = matchType.name,
        action = action.name,
        actionDurationHours = durationHours,
        severity = severity,
        createdAt = Instant.now()
    )
}
