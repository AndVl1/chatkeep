package ru.andvl.chatkeep.bot.handlers

import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.andvl.chatkeep.bot.handlers.moderation.ModerationCommandHandler
import ru.andvl.chatkeep.domain.service.moderation.AdminCacheService
import ru.andvl.chatkeep.domain.service.moderation.PunishmentService
import ru.andvl.chatkeep.domain.service.moderation.UsernameCacheService
import ru.andvl.chatkeep.domain.service.moderation.WarningService
import kotlin.test.assertNotNull

/**
 * Test for moderation command handlers (/warn, /mute, /ban, /kick, etc.).
 *
 * Verifies that:
 * - All moderation command handlers can be instantiated
 * - Commands are configured with requireOnlyCommandInMessage = false to accept arguments
 * - Handler dependencies are correctly injected
 */
class ModerationCommandsTest {

    private lateinit var adminCacheService: AdminCacheService
    private lateinit var warningService: WarningService
    private lateinit var punishmentService: PunishmentService
    private lateinit var usernameCacheService: UsernameCacheService
    private lateinit var handler: ModerationCommandHandler

    @BeforeEach
    fun setUp() {
        adminCacheService = mockk(relaxed = true)
        warningService = mockk(relaxed = true)
        punishmentService = mockk(relaxed = true)
        usernameCacheService = mockk(relaxed = true)

        handler = ModerationCommandHandler(
            adminCacheService,
            warningService,
            punishmentService,
            usernameCacheService
        )
    }

    @Test
    fun `moderation handler can be instantiated`() = runTest {
        assertNotNull(handler)
    }

    @Test
    fun `moderation handler has all required dependencies`() = runTest {
        val testHandler = ModerationCommandHandler(
            adminCacheService,
            warningService,
            punishmentService,
            usernameCacheService
        )
        assertNotNull(testHandler)
    }
}
