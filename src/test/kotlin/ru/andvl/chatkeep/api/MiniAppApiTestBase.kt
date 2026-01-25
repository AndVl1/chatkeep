package ru.andvl.chatkeep.api

import io.mockk.clearMocks
import io.mockk.coEvery
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.testcontainers.containers.PostgreSQLContainer
import ru.andvl.chatkeep.api.config.ApiTestConfiguration
import ru.andvl.chatkeep.api.support.AuthTestHelper
import ru.andvl.chatkeep.api.support.CapturingLogChannelPort
import ru.andvl.chatkeep.api.support.TestDataFactory
import ru.andvl.chatkeep.config.TestConfiguration
import ru.andvl.chatkeep.domain.service.moderation.AdminCacheService
import ru.andvl.chatkeep.infrastructure.repository.ChatSettingsRepository
import ru.andvl.chatkeep.infrastructure.repository.channelreply.ChannelReplySettingsRepository
import ru.andvl.chatkeep.infrastructure.repository.locks.LockSettingsRepository
import ru.andvl.chatkeep.infrastructure.repository.moderation.BlocklistPatternRepository
import ru.andvl.chatkeep.infrastructure.repository.moderation.ModerationConfigRepository

/**
 * Base test class for Mini App API integration tests.
 * Provides shared infrastructure including:
 * - PostgreSQL testcontainer (singleton - shared across all test classes)
 * - MockMvc setup
 * - Common test data factory
 * - Auth helper
 * - Capturing log channel port
 * - Mock admin cache service
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test", "apitest")
@Import(TestConfiguration::class, ApiTestConfiguration::class)
abstract class MiniAppApiTestBase {

    companion object {
        // Singleton container - started once and shared across all test classes
        @JvmStatic
        private val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("chatkeep_apitest")
            .withUsername("test")
            .withPassword("test")
            .apply { start() }

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.datasource.driver-class-name") { "org.postgresql.Driver" }
        }
    }

    @Autowired
    protected lateinit var mockMvc: MockMvc

    @Autowired
    protected lateinit var authTestHelper: AuthTestHelper

    @Autowired
    protected lateinit var capturingLogChannelPort: CapturingLogChannelPort

    @Autowired
    protected lateinit var adminCacheService: AdminCacheService

    @Autowired
    protected lateinit var testDataFactory: TestDataFactory

    // Repositories for test data setup and cleanup
    @Autowired
    protected lateinit var chatSettingsRepository: ChatSettingsRepository

    @Autowired
    protected lateinit var moderationConfigRepository: ModerationConfigRepository

    @Autowired
    protected lateinit var lockSettingsRepository: LockSettingsRepository

    @Autowired
    protected lateinit var blocklistPatternRepository: BlocklistPatternRepository

    @Autowired
    protected lateinit var channelReplySettingsRepository: ChannelReplySettingsRepository

    @Autowired
    protected lateinit var mediaStorageRepository: ru.andvl.chatkeep.infrastructure.repository.media.MediaStorageRepository

    @Autowired
    protected lateinit var twitchChannelSubscriptionRepository: ru.andvl.chatkeep.infrastructure.repository.twitch.TwitchChannelSubscriptionRepository

    @Autowired
    protected lateinit var twitchStreamRepository: ru.andvl.chatkeep.infrastructure.repository.twitch.TwitchStreamRepository

    @Autowired
    protected lateinit var streamTimelineEventRepository: ru.andvl.chatkeep.infrastructure.repository.twitch.StreamTimelineEventRepository

    @Autowired
    protected lateinit var twitchNotificationSettingsRepository: ru.andvl.chatkeep.infrastructure.repository.twitch.TwitchNotificationSettingsRepository

    @Autowired
    protected lateinit var chatGatedFeatureRepository: ru.andvl.chatkeep.infrastructure.repository.gated.ChatGatedFeatureRepository

    @BeforeEach
    fun baseSetup() {
        // Clear mock recordings (but preserve relaxed behavior)
        clearMocks(adminCacheService, answers = false, recordedCalls = true, childMocks = false)

        // Clear test auth users
        authTestHelper.clearUsers()

        // Clear capturing port
        capturingLogChannelPort.clear()

        // Setup default admin mock (tests can override)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, 123456789L)

        // Clean up test data
        cleanupDatabase()
    }

    /**
     * Mock user as NOT admin in the specified chat.
     */
    protected fun mockUserNotAdmin(chatId: Long, userId: Long) {
        coEvery {
            adminCacheService.isAdmin(userId, chatId, any())
        } returns false
    }

    /**
     * Mock user as admin in the specified chat.
     */
    protected fun mockUserIsAdmin(chatId: Long, userId: Long) {
        coEvery {
            adminCacheService.isAdmin(userId, chatId, any())
        } returns true
    }

    /**
     * Clean up database between tests to avoid pollution.
     */
    private fun cleanupDatabase() {
        // Clean up in reverse order of dependencies
        streamTimelineEventRepository.deleteAll()
        twitchStreamRepository.deleteAll()
        twitchChannelSubscriptionRepository.deleteAll()
        twitchNotificationSettingsRepository.deleteAll()
        chatGatedFeatureRepository.deleteAll()
        blocklistPatternRepository.deleteAll()
        channelReplySettingsRepository.deleteAll()
        mediaStorageRepository.deleteAll()
        lockSettingsRepository.deleteAll()
        moderationConfigRepository.deleteAll()
        chatSettingsRepository.deleteAll()
    }
}
