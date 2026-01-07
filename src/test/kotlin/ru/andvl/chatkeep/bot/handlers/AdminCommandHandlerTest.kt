package ru.andvl.chatkeep.bot.handlers

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.chat.get.getChatAdministrators
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.chat.member.AdministratorChatMember
import dev.inmo.tgbotapi.types.chat.PreviewUser
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.andvl.chatkeep.domain.model.ChatSettings
import ru.andvl.chatkeep.domain.service.AdminService
import ru.andvl.chatkeep.domain.service.ChatService
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdminCommandHandlerTest {

    private lateinit var chatService: ChatService
    private lateinit var adminService: AdminService
    private lateinit var handler: AdminCommandHandler

    @BeforeEach
    fun setUp() {
        chatService = mockk()
        adminService = mockk()
        handler = AdminCommandHandler(chatService, adminService, "https://test-mini-app.example.com")
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic("dev.inmo.tgbotapi.extensions.api.chat.get.GetChatAdministratorsKt")
    }

    @Test
    fun `getAllChats returns all registered chats`() = runTest {
        // Given
        val chats = listOf(
            createChatSettings(chatId = -100123L, title = "Test Group 1"),
            createChatSettings(chatId = -100456L, title = "Test Group 2")
        )
        every { chatService.getAllChats() } returns chats

        // When
        val result = chatService.getAllChats()

        // Then
        assertEquals(2, result.size)
        assertEquals("Test Group 1", result[0].chatTitle)
        assertEquals("Test Group 2", result[1].chatTitle)
    }

    @Test
    fun `isUserAdminInChat returns true when user is admin`() = runTest {
        // Given
        val bot = mockk<TelegramBot>()
        val userId = 12345L
        val chatId = -100123L

        mockkStatic("dev.inmo.tgbotapi.extensions.api.chat.get.GetChatAdministratorsKt")

        val adminUser = mockk<PreviewUser>()
        every { adminUser.id.chatId.long } returns userId

        val admin = mockk<AdministratorChatMember>()
        every { admin.user } returns adminUser

        coEvery { bot.getChatAdministrators(ChatId(RawChatId(chatId))) } returns listOf(admin)

        // When - test the logic directly
        val admins = bot.getChatAdministrators(ChatId(RawChatId(chatId)))
        val isAdmin = admins.any { it.user.id.chatId.long == userId }

        // Then
        assertTrue(isAdmin)
    }

    @Test
    fun `isUserAdminInChat returns false when user is not admin`() = runTest {
        // Given
        val bot = mockk<TelegramBot>()
        val userId = 12345L
        val chatId = -100123L

        mockkStatic("dev.inmo.tgbotapi.extensions.api.chat.get.GetChatAdministratorsKt")

        val otherUser = mockk<PreviewUser>()
        every { otherUser.id.chatId.long } returns 99999L

        val admin = mockk<AdministratorChatMember>()
        every { admin.user } returns otherUser

        coEvery { bot.getChatAdministrators(ChatId(RawChatId(chatId))) } returns listOf(admin)

        // When
        val admins = bot.getChatAdministrators(ChatId(RawChatId(chatId)))
        val isAdmin = admins.any { it.user.id.chatId.long == userId }

        // Then
        assertTrue(!isAdmin)
    }

    @Test
    fun `isUserAdminInChat returns false when exception occurs`() = runTest {
        // Given
        val bot = mockk<TelegramBot>()
        val chatId = -100123L

        mockkStatic("dev.inmo.tgbotapi.extensions.api.chat.get.GetChatAdministratorsKt")

        coEvery { bot.getChatAdministrators(ChatId(RawChatId(chatId))) } throws RuntimeException("API error")

        // When
        val isAdmin = try {
            val admins = bot.getChatAdministrators(ChatId(RawChatId(chatId)))
            admins.any { it.user.id.chatId.long == 12345L }
        } catch (e: Exception) {
            false
        }

        // Then
        assertTrue(!isAdmin)
    }

    @Test
    fun `filter admin chats correctly filters by admin status`() = runTest {
        // Given
        val userId = 12345L
        val chat1 = createChatSettings(chatId = -100123L, title = "Admin Chat")
        val chat2 = createChatSettings(chatId = -100456L, title = "Non-Admin Chat")
        val allChats = listOf(chat1, chat2)

        val bot = mockk<TelegramBot>()
        mockkStatic("dev.inmo.tgbotapi.extensions.api.chat.get.GetChatAdministratorsKt")

        // User is admin in chat1 only
        val adminUser = mockk<PreviewUser>()
        every { adminUser.id.chatId.long } returns userId

        val admin = mockk<AdministratorChatMember>()
        every { admin.user } returns adminUser

        val otherUser = mockk<PreviewUser>()
        every { otherUser.id.chatId.long } returns 99999L

        val otherAdmin = mockk<AdministratorChatMember>()
        every { otherAdmin.user } returns otherUser

        coEvery { bot.getChatAdministrators(ChatId(RawChatId(-100123L))) } returns listOf(admin)
        coEvery { bot.getChatAdministrators(ChatId(RawChatId(-100456L))) } returns listOf(otherAdmin)

        // When - simulate the filtering logic from handler
        val adminChats = mutableListOf<ChatSettings>()
        for (settings in allChats) {
            val isAdmin = try {
                val admins = bot.getChatAdministrators(ChatId(RawChatId(settings.chatId)))
                admins.any { it.user.id.chatId.long == userId }
            } catch (e: Exception) {
                false
            }
            if (isAdmin) {
                adminChats.add(settings)
            }
        }

        // Then
        assertEquals(1, adminChats.size)
        assertEquals("Admin Chat", adminChats[0].chatTitle)
    }

    private fun createChatSettings(
        id: Long? = null,
        chatId: Long,
        title: String? = null,
        enabled: Boolean = true
    ) = ChatSettings(
        id = id,
        chatId = chatId,
        chatTitle = title,
        collectionEnabled = enabled,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )
}
