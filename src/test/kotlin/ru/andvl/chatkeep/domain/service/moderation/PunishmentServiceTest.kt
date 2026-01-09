package ru.andvl.chatkeep.domain.service.moderation

import dev.inmo.tgbotapi.bot.TelegramBot
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.andvl.chatkeep.domain.model.moderation.Punishment
import ru.andvl.chatkeep.domain.model.moderation.PunishmentSource
import ru.andvl.chatkeep.domain.model.moderation.PunishmentType
import ru.andvl.chatkeep.domain.service.logchannel.LogChannelService
import ru.andvl.chatkeep.infrastructure.repository.moderation.PunishmentRepository
import java.time.Instant
import kotlin.test.assertTrue

/**
 * Unit tests for PunishmentService.
 * Focus on NOTHING punishment type behavior.
 */
class PunishmentServiceTest {

    private lateinit var repository: PunishmentRepository
    private lateinit var bot: TelegramBot
    private lateinit var logChannelService: LogChannelService
    private lateinit var usernameCacheService: UsernameCacheService
    private lateinit var service: PunishmentService

    @BeforeEach
    fun setup() {
        repository = mockk(relaxed = true)
        bot = mockk(relaxed = true)
        logChannelService = mockk(relaxed = true)
        usernameCacheService = mockk(relaxed = true)
        val metricsService = mockk<ru.andvl.chatkeep.metrics.BotMetricsService>(relaxed = true)
        service = PunishmentService(repository, bot, logChannelService, usernameCacheService, metricsService)
    }

    @Test
    fun `executePunishment with NOTHING should log punishment without Telegram API calls`() = runTest {
        // Given
        val chatId = 123L
        val userId = 456L
        val punishment = Punishment(
            id = 1L,
            chatId = chatId,
            userId = userId,
            issuedById = 0L,
            punishmentType = "NOTHING",
            durationSeconds = null,
            reason = "Blocklist match",
            source = "BLOCKLIST",
            createdAt = Instant.now()
        )
        every { repository.save(any()) } returns punishment

        // When
        val result = service.executePunishment(
            chatId = chatId,
            userId = userId,
            issuedById = 0L,
            type = PunishmentType.NOTHING,
            duration = null,
            reason = "Blocklist match",
            source = PunishmentSource.BLOCKLIST
        )

        // Then
        assertTrue(result, "NOTHING punishment should return success")

        // Verify punishment was logged
        verify { repository.save(match {
            it.punishmentType == "NOTHING" &&
            it.chatId == chatId &&
            it.userId == userId &&
            it.source == "BLOCKLIST"
        }) }

        // Verify NO Telegram API calls were made
        coVerify(exactly = 0) { bot.execute(any()) }
    }

    @Test
    fun `executePunishment with NOTHING should return true`() = runTest {
        // Given
        every { repository.save(any()) } returns mockk()

        // When
        val result = service.executePunishment(
            chatId = 123L,
            userId = 456L,
            issuedById = 789L,
            type = PunishmentType.NOTHING,
            duration = null,
            reason = "Test reason",
            source = PunishmentSource.MANUAL
        )

        // Then
        assertTrue(result)
    }

    @Test
    fun `executePunishment with NOTHING should work with MANUAL source`() = runTest {
        // Given
        val punishment = mockk<Punishment>()
        every { repository.save(any()) } returns punishment

        // When
        val result = service.executePunishment(
            chatId = 123L,
            userId = 456L,
            issuedById = 789L,
            type = PunishmentType.NOTHING,
            duration = null,
            reason = "Admin removed message",
            source = PunishmentSource.MANUAL
        )

        // Then
        assertTrue(result)
        verify { repository.save(match { it.source == "MANUAL" }) }
    }
}
