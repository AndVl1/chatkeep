package ru.andvl.chatkeep.infrastructure.repository

import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import ru.andvl.chatkeep.domain.model.ChatMessage

@Repository
interface MessageRepository : CrudRepository<ChatMessage, Long> {

    fun findByChatId(chatId: Long): List<ChatMessage>

    @Query("SELECT COUNT(*) FROM messages WHERE chat_id = :chatId")
    fun countByChatId(chatId: Long): Long

    @Query("SELECT COUNT(DISTINCT user_id) FROM messages WHERE chat_id = :chatId")
    fun countUniqueUsersByChatId(chatId: Long): Long

    fun existsByChatIdAndTelegramMessageId(chatId: Long, telegramMessageId: Long): Boolean

    @Query("""
        SELECT COUNT(*) FROM messages
        WHERE DATE(message_date AT TIME ZONE 'UTC') = CURRENT_DATE
    """)
    fun countMessagesToday(): Long

    @Query("""
        SELECT COUNT(*) FROM messages
        WHERE DATE(message_date AT TIME ZONE 'UTC') = CURRENT_DATE - INTERVAL '1 day'
    """)
    fun countMessagesYesterday(): Long

    @Query("""
        SELECT COUNT(*) FROM messages
        WHERE chat_id = :chatId
        AND DATE(message_date AT TIME ZONE 'UTC') = CURRENT_DATE
    """)
    fun countMessagesTodayByChatId(chatId: Long): Long

    @Query("""
        SELECT COUNT(*) FROM messages
        WHERE chat_id = :chatId
        AND DATE(message_date AT TIME ZONE 'UTC') = CURRENT_DATE - INTERVAL '1 day'
    """)
    fun countMessagesYesterdayByChatId(chatId: Long): Long
}
