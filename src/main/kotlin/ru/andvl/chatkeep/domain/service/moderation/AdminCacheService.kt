package ru.andvl.chatkeep.domain.service.moderation

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.chat.get.getChatAdministrators
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.RawChatId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import ru.andvl.chatkeep.infrastructure.repository.moderation.AdminCacheRepository
import java.time.Instant
import kotlin.time.Duration.Companion.minutes

@Service
class AdminCacheService(
    private val adminCacheRepository: AdminCacheRepository,
    private val bot: TelegramBot
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val cacheTtl = 5.minutes

    /**
     * Check if user is admin in a chat.
     *
     * @param userId User ID to check
     * @param chatId Chat ID to check in
     * @param forceRefresh If true, bypass cache and fetch fresh from Telegram (SEC-003).
     *                     Use for sensitive operations like /viewlogs.
     * @return true if user is admin
     */
    suspend fun isAdmin(userId: Long, chatId: Long, forceRefresh: Boolean = false): Boolean {
        // Force fresh check for sensitive operations (SEC-003)
        if (forceRefresh) {
            logger.debug("Admin check force refresh: userId=$userId, chatId=$chatId")
            return fetchAdminStatus(userId, chatId)
        }

        val now = Instant.now()

        // Check cache first
        adminCacheRepository.findValidEntry(userId, chatId, now)?.let {
            logger.debug("Admin check cache hit: userId=$userId, chatId=$chatId, isAdmin=${it.isAdmin}")
            return it.isAdmin
        }

        // Cache miss - fetch from Telegram
        logger.debug("Admin check cache miss: userId=$userId, chatId=$chatId")
        val isAdmin = fetchAdminStatus(userId, chatId)

        // Store in cache using upsert to handle expired entries
        val expiresAt = now.plusSeconds(cacheTtl.inWholeSeconds)

        runCatching {
            adminCacheRepository.upsert(
                userId = userId,
                chatId = chatId,
                isAdmin = isAdmin,
                cachedAt = now,
                expiresAt = expiresAt
            )
        }.onFailure {
            logger.warn("Failed to cache admin status: ${it.message}")
        }

        return isAdmin
    }

    private suspend fun fetchAdminStatus(userId: Long, chatId: Long): Boolean {
        return try {
            val admins = bot.getChatAdministrators(ChatId(RawChatId(chatId)))
            admins.any { it.user.id.chatId.long == userId }
        } catch (e: Exception) {
            logger.debug("Failed to get admins for chat $chatId: ${e.message}")
            false
        }
    }

    fun invalidateCache(userId: Long, chatId: Long) {
        adminCacheRepository.deleteByUserIdAndChatId(userId, chatId)
        logger.debug("Invalidated admin cache: userId=$userId, chatId=$chatId")
    }

    fun cleanExpired() {
        val now = Instant.now()
        adminCacheRepository.deleteExpired(now)
        logger.info("Cleaned expired admin cache entries")
    }
}
