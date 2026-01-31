package ru.andvl.chatkeep.api

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.springframework.http.MediaType
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.put
import ru.andvl.chatkeep.api.support.TestDataFactory
import ru.andvl.chatkeep.domain.model.locks.LockSettings
import ru.andvl.chatkeep.domain.model.locks.LockType
import ru.andvl.chatkeep.infrastructure.repository.locks.findByChatId
import ru.andvl.chatkeep.domain.model.moderation.ActionType

/**
 * Integration tests for MiniAppLocksController.
 * Tests lock settings retrieval and updates.
 */
class MiniAppLocksControllerTest : MiniAppApiTestBase() {

    @Test
    fun `GET locks - returns 401 when not authenticated`() {
        mockMvc.get("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/locks")
            .andExpect {
                status { isUnauthorized() }
            }
    }

    @Test
    fun `GET locks - returns 403 when user is not admin`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)

        chatSettingsRepository.save(testDataFactory.createChatSettings())
        mockUserNotAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        mockMvc.get("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/locks") {
            header("Authorization", authHeader)
        }.asyncDispatchIfNeeded().andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `GET locks - returns default lock settings when none exist`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        mockMvc.get("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/locks") {
            header("Authorization", authHeader)
        }.asyncDispatchIfNeeded().andExpect {
            status { isOk() }
            jsonPath("$.chatId") { value(TestDataFactory.DEFAULT_CHAT_ID) }
            jsonPath("$.locks") { isMap() }
            jsonPath("$.lockWarnsEnabled") { value(false) } // Default is false when no settings exist
        }
    }

    @Test
    fun `GET locks - returns existing lock configurations`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        // Create lock settings with JSON encoding (use factory method)
        val locksJson = """{"TEXT":{"locked":true,"reason":"No text messages allowed"},"STICKER":{"locked":true,"reason":null}}"""

        val settings = LockSettings.createNew(
            chatId = TestDataFactory.DEFAULT_CHAT_ID,
            locksJson = locksJson,
            lockWarns = true
        )
        lockSettingsRepository.save(settings)

        mockMvc.get("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/locks") {
            header("Authorization", authHeader)
        }.asyncDispatchIfNeeded().andExpect {
            status { isOk() }
            jsonPath("$.locks.TEXT.locked") { value(true) }
            jsonPath("$.locks.TEXT.reason") { value("No text messages allowed") }
            jsonPath("$.locks.STICKER.locked") { value(true) }
            jsonPath("$.locks.STICKER.reason") { doesNotExist() }
        }
    }

    @Test
    fun `GET locks - includes lock warns setting`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        // Create lock_warns config set to false
        lockSettingsRepository.save(
            LockSettings.createNew(
                chatId = TestDataFactory.DEFAULT_CHAT_ID,
                locksJson = "{}",
                lockWarns = false
            )
        )

        mockMvc.get("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/locks") {
            header("Authorization", authHeader)
        }.asyncDispatchIfNeeded().andExpect {
            status { isOk() }
            jsonPath("$.lockWarnsEnabled") { value(false) }
        }
    }

    @Test
    fun `PUT locks - returns 403 when user is not admin`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)

        chatSettingsRepository.save(testDataFactory.createChatSettings())
        mockUserNotAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        mockMvc.put("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/locks") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """{"locks": {}}"""
        }.asyncDispatchIfNeeded().andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `PUT locks - validates invalid lock type`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        mockMvc.put("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/locks") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                    "locks": {
                        "INVALID_TYPE": {
                            "locked": true
                        }
                    }
                }
            """.trimIndent()
        }.asyncDispatchIfNeeded().andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `PUT locks - updates single lock`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        mockMvc.put("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/locks") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                    "locks": {
                        "TEXT": {
                            "locked": true,
                            "reason": "No spam"
                        }
                    }
                }
            """.trimIndent()
        }.asyncDispatchIfNeeded().andExpect {
            status { isOk() }
            jsonPath("$.locks.TEXT.locked") { value(true) }
            jsonPath("$.locks.TEXT.reason") { value("No spam") }
        }

        // Verify lock saved (locks are stored in JSON format)
        val lockSettings = lockSettingsRepository.findByChatId(TestDataFactory.DEFAULT_CHAT_ID)
        assertThat(lockSettings).isNotNull
    }

    @Test
    fun `PUT locks - updates multiple locks`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        mockMvc.put("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/locks") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                    "locks": {
                        "TEXT": {
                            "locked": true,
                            "reason": "No text"
                        },
                        "STICKER": {
                            "locked": true,
                            "reason": null
                        },
                        "PHOTO": {
                            "locked": true,
                            "reason": null
                        }
                    }
                }
            """.trimIndent()
        }.asyncDispatchIfNeeded().andExpect {
            status { isOk() }
            jsonPath("$.locks.TEXT.locked") { value(true) }
            jsonPath("$.locks.STICKER.locked") { value(true) }
            jsonPath("$.locks.PHOTO.locked") { value(true) }
        }
    }

    @Test
    fun `PUT locks - enables lock warns and logs change`(): Unit = runBlocking {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings(chatTitle = "Test Chat"))

        // Setup config with log channel
        moderationConfigRepository.save(
            ru.andvl.chatkeep.domain.model.moderation.ModerationConfig(
                chatId = TestDataFactory.DEFAULT_CHAT_ID,
                logChannelId = -1001234567890L
            )
        )

        // Initially disabled
        lockSettingsRepository.save(
            LockSettings.createNew(
                chatId = TestDataFactory.DEFAULT_CHAT_ID,
                locksJson = "{}",
                lockWarns = false
            )
        )

        mockMvc.put("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/locks") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                    "locks": {},
                    "lockWarnsEnabled": true
                }
            """.trimIndent()
        }.asyncDispatchIfNeeded().andExpect {
            status { isOk() }
            jsonPath("$.lockWarnsEnabled") { value(true) }
        }

        // Verify log entry sent
        val logEntry = capturingLogChannelPort.waitForEntry(
            channelId = -1001234567890L,
            actionType = ActionType.LOCK_WARNS_ON,
            timeoutMs = 2000
        )

        assertThat(logEntry).isNotNull
        assertThat(logEntry?.chatId).isEqualTo(TestDataFactory.DEFAULT_CHAT_ID)
        assertThat(logEntry?.chatTitle).isEqualTo("Test Chat")
        assertThat(logEntry?.adminId).isEqualTo(user.id)
        assertThat(logEntry?.actionType).isEqualTo(ActionType.LOCK_WARNS_ON)
    }

    @Test
    fun `PUT locks - disables lock warns and logs change`(): Unit = runBlocking {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings(chatTitle = "Test Chat"))

        // Setup config with log channel
        moderationConfigRepository.save(
            ru.andvl.chatkeep.domain.model.moderation.ModerationConfig(
                chatId = TestDataFactory.DEFAULT_CHAT_ID,
                logChannelId = -1001234567890L
            )
        )

        // First enable lock warns (default is false)
        lockSettingsRepository.save(
            LockSettings.createNew(
                chatId = TestDataFactory.DEFAULT_CHAT_ID,
                lockWarns = true
            )
        )

        // Now disable lock warns - this should trigger a log
        mockMvc.put("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/locks") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                    "locks": {},
                    "lockWarnsEnabled": false
                }
            """.trimIndent()
        }.asyncDispatchIfNeeded().andExpect {
            status { isOk() }
            jsonPath("$.lockWarnsEnabled") { value(false) }
        }

        // Verify log entry sent
        val logEntry = capturingLogChannelPort.waitForEntry(
            channelId = -1001234567890L,
            actionType = ActionType.LOCK_WARNS_OFF,
            timeoutMs = 2000
        )

        assertThat(logEntry).isNotNull
        assertThat(logEntry?.actionType).isEqualTo(ActionType.LOCK_WARNS_OFF)
    }

    @Test
    fun `PUT locks - does not log when lock warns unchanged`(): Unit = runBlocking {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        // Setup config with log channel
        moderationConfigRepository.save(
            ru.andvl.chatkeep.domain.model.moderation.ModerationConfig(
                chatId = TestDataFactory.DEFAULT_CHAT_ID,
                logChannelId = -1001234567890L
            )
        )

        // Set to false explicitly, then set again to false (no change)
        lockSettingsRepository.save(
            LockSettings.createNew(
                chatId = TestDataFactory.DEFAULT_CHAT_ID,
                locksJson = "{}",
                lockWarns = false
            )
        )

        mockMvc.put("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/locks") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                    "locks": {},
                    "lockWarnsEnabled": false
                }
            """.trimIndent()
        }.asyncDispatchIfNeeded().andExpect {
            status { isOk() }
        }

        // Verify no log entry sent
        assertThat(capturingLogChannelPort.getEntriesForChannel(-1001234567890L)).isEmpty()
    }

    @Test
    fun `PUT locks - updates locks without changing lock warns`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        mockMvc.put("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/locks") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                    "locks": {
                        "TEXT": {
                            "locked": true,
                            "reason": "Test"
                        }
                    }
                }
            """.trimIndent()
        }.asyncDispatchIfNeeded().andExpect {
            status { isOk() }
            jsonPath("$.locks.TEXT.locked") { value(true) }
            jsonPath("$.lockWarnsEnabled") { value(false) } // Default is false when no settings exist
        }
    }

    @Test
    fun `PUT locks - unlocks previously locked content`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        // First lock it
        lockSettingsRepository.save(
            LockSettings.createNew(
                chatId = TestDataFactory.DEFAULT_CHAT_ID,
                locksJson = """{"TEXT": {"locked": true, "reason": "Locked"}}""",
                lockWarns = true
            )
        )

        // Then unlock it
        mockMvc.put("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/locks") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                    "locks": {
                        "TEXT": {
                            "locked": false,
                            "reason": null
                        }
                    }
                }
            """.trimIndent()
        }.asyncDispatchIfNeeded().andExpect {
            status { isOk() }
            jsonPath("$.locks.TEXT") { doesNotExist() } // Unlocked locks are removed from response
        }

        // Verify lock updated (locks stored in JSON)
        val lockSettings = lockSettingsRepository.findByChatId(TestDataFactory.DEFAULT_CHAT_ID)
        assertThat(lockSettings).isNotNull
    }

    @Test
    fun `PUT locks - handles all lock types`() {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        mockMvc.put("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/locks") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                    "locks": {
                        "TEXT": {"locked": true, "reason": null},
                        "STICKER": {"locked": true, "reason": null},
                        "GIF": {"locked": true, "reason": null},
                        "PHOTO": {"locked": true, "reason": null},
                        "VIDEO": {"locked": true, "reason": null},
                        "VOICE": {"locked": true, "reason": null},
                        "AUDIO": {"locked": true, "reason": null},
                        "DOCUMENT": {"locked": true, "reason": null},
                        "FORWARD": {"locked": true, "reason": null},
                        "LINK": {"locked": true, "reason": null},
                        "RTLCHAR": {"locked": true, "reason": null}
                    }
                }
            """.trimIndent()
        }.asyncDispatchIfNeeded().andExpect {
            status { isOk() }
            jsonPath("$.locks.TEXT.locked") { value(true) }
            jsonPath("$.locks.STICKER.locked") { value(true) }
            jsonPath("$.locks.GIF.locked") { value(true) }
            jsonPath("$.locks.PHOTO.locked") { value(true) }
            jsonPath("$.locks.VIDEO.locked") { value(true) }
        }
    }

    @Test
    fun `PUT locks - logs LOCK_ENABLED when lock is enabled`(): Unit = runBlocking {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings(chatTitle = "Test Chat"))

        // Setup config with log channel
        moderationConfigRepository.save(
            ru.andvl.chatkeep.domain.model.moderation.ModerationConfig(
                chatId = TestDataFactory.DEFAULT_CHAT_ID,
                logChannelId = -1001234567890L
            )
        )

        // Initially no locks
        lockSettingsRepository.save(
            LockSettings.createNew(
                chatId = TestDataFactory.DEFAULT_CHAT_ID,
                locksJson = "{}",
                lockWarns = false
            )
        )

        mockMvc.put("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/locks") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                    "locks": {
                        "TEXT": {
                            "locked": true,
                            "reason": "No spam"
                        }
                    }
                }
            """.trimIndent()
        }.asyncDispatchIfNeeded().andExpect {
            status { isOk() }
        }

        // Verify log entry sent
        val logEntry = capturingLogChannelPort.waitForEntry(
            channelId = -1001234567890L,
            actionType = ActionType.LOCK_ENABLED,
            timeoutMs = 2000
        )

        assertThat(logEntry).isNotNull
        assertThat(logEntry?.chatId).isEqualTo(TestDataFactory.DEFAULT_CHAT_ID)
        assertThat(logEntry?.chatTitle).isEqualTo("Test Chat")
        assertThat(logEntry?.adminId).isEqualTo(user.id)
        assertThat(logEntry?.actionType).isEqualTo(ActionType.LOCK_ENABLED)
        assertThat(logEntry?.reason).isEqualTo("TEXT")
    }

    @Test
    fun `PUT locks - logs LOCK_DISABLED when lock is disabled`(): Unit = runBlocking {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings(chatTitle = "Test Chat"))

        // Setup config with log channel
        moderationConfigRepository.save(
            ru.andvl.chatkeep.domain.model.moderation.ModerationConfig(
                chatId = TestDataFactory.DEFAULT_CHAT_ID,
                logChannelId = -1001234567890L
            )
        )

        // Initially locked
        lockSettingsRepository.save(
            LockSettings.createNew(
                chatId = TestDataFactory.DEFAULT_CHAT_ID,
                locksJson = """{"TEXT": {"locked": true, "reason": "No spam"}}""",
                lockWarns = false
            )
        )

        mockMvc.put("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/locks") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                    "locks": {
                        "TEXT": {
                            "locked": false,
                            "reason": null
                        }
                    }
                }
            """.trimIndent()
        }.asyncDispatchIfNeeded().andExpect {
            status { isOk() }
        }

        // Verify log entry sent
        val logEntry = capturingLogChannelPort.waitForEntry(
            channelId = -1001234567890L,
            actionType = ActionType.LOCK_DISABLED,
            timeoutMs = 2000
        )

        assertThat(logEntry).isNotNull
        assertThat(logEntry?.actionType).isEqualTo(ActionType.LOCK_DISABLED)
        assertThat(logEntry?.reason).isEqualTo("TEXT")
    }

    @Test
    fun `PUT locks - does not log when lock state unchanged`(): Unit = runBlocking {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings())

        // Setup config with log channel
        moderationConfigRepository.save(
            ru.andvl.chatkeep.domain.model.moderation.ModerationConfig(
                chatId = TestDataFactory.DEFAULT_CHAT_ID,
                logChannelId = -1001234567890L
            )
        )

        // Already locked
        lockSettingsRepository.save(
            LockSettings.createNew(
                chatId = TestDataFactory.DEFAULT_CHAT_ID,
                locksJson = """{"TEXT": {"locked": true, "reason": "Old reason"}}""",
                lockWarns = false
            )
        )

        mockMvc.put("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/locks") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                    "locks": {
                        "TEXT": {
                            "locked": true,
                            "reason": "New reason"
                        }
                    }
                }
            """.trimIndent()
        }.asyncDispatchIfNeeded().andExpect {
            status { isOk() }
        }

        // Verify no log entry sent (state unchanged from true -> true)
        assertThat(capturingLogChannelPort.getEntriesForChannel(-1001234567890L)).isEmpty()
    }

    @Test
    fun `PUT locks - logs multiple lock changes`(): Unit = runBlocking {
        val user = testDataFactory.createTelegramUser()
        val authHeader = authTestHelper.createValidAuthHeader(user)
        mockUserIsAdmin(TestDataFactory.DEFAULT_CHAT_ID, user.id)

        chatSettingsRepository.save(testDataFactory.createChatSettings(chatTitle = "Test Chat"))

        // Setup config with log channel
        moderationConfigRepository.save(
            ru.andvl.chatkeep.domain.model.moderation.ModerationConfig(
                chatId = TestDataFactory.DEFAULT_CHAT_ID,
                logChannelId = -1001234567890L
            )
        )

        // Setup mixed initial state
        lockSettingsRepository.save(
            LockSettings.createNew(
                chatId = TestDataFactory.DEFAULT_CHAT_ID,
                locksJson = """{"TEXT": {"locked": true, "reason": null}}""",
                lockWarns = false
            )
        )

        mockMvc.put("/api/v1/miniapp/chats/${TestDataFactory.DEFAULT_CHAT_ID}/locks") {
            header("Authorization", authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                    "locks": {
                        "TEXT": {"locked": false, "reason": null},
                        "STICKER": {"locked": true, "reason": null},
                        "PHOTO": {"locked": true, "reason": null}
                    }
                }
            """.trimIndent()
        }.asyncDispatchIfNeeded().andExpect {
            status { isOk() }
        }

        // Verify multiple log entries
        val entries = capturingLogChannelPort.getEntriesForChannel(-1001234567890L)
        assertThat(entries).hasSize(3)

        // Check we have entries for all three types
        val actionsByType = entries.associate { it.reason!! to it.actionType }
        assertThat(actionsByType["TEXT"]).isEqualTo(ActionType.LOCK_DISABLED)
        assertThat(actionsByType["STICKER"]).isEqualTo(ActionType.LOCK_ENABLED)
        assertThat(actionsByType["PHOTO"]).isEqualTo(ActionType.LOCK_ENABLED)
    }
}
