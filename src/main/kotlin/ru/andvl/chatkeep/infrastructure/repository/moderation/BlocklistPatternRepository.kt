package ru.andvl.chatkeep.infrastructure.repository.moderation

import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import ru.andvl.chatkeep.domain.model.moderation.BlocklistPattern

interface BlocklistPatternRepository : CrudRepository<BlocklistPattern, Long> {

    @Query("""
        SELECT * FROM blocklist_patterns
        WHERE chat_id = :chatId OR chat_id IS NULL
        ORDER BY severity DESC, created_at ASC
    """)
    fun findByChatIdOrGlobal(chatId: Long): List<BlocklistPattern>

    @Query("SELECT * FROM blocklist_patterns WHERE chat_id = :chatId")
    fun findByChatId(chatId: Long): List<BlocklistPattern>

    @Query("SELECT * FROM blocklist_patterns WHERE chat_id IS NULL")
    fun findGlobalPatterns(): List<BlocklistPattern>

    @Query("""
        DELETE FROM blocklist_patterns
        WHERE chat_id = :chatId
        AND pattern = :pattern
    """)
    fun deleteByChatIdAndPattern(chatId: Long, pattern: String)
}
