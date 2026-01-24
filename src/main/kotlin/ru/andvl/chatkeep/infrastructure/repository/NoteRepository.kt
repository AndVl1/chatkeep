package ru.andvl.chatkeep.infrastructure.repository

import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import ru.andvl.chatkeep.domain.model.Note

interface NoteRepository : CrudRepository<Note, Long> {

    @Query("SELECT * FROM notes WHERE chat_id = :chatId ORDER BY created_at DESC")
    fun findAllByChatId(chatId: Long): List<Note>

    @Query("SELECT * FROM notes WHERE chat_id = :chatId AND note_name = :noteName")
    fun findByChatIdAndNoteName(chatId: Long, noteName: String): Note?

    @Modifying
    @Query("DELETE FROM notes WHERE chat_id = :chatId AND id = :noteId")
    fun deleteByChatIdAndId(chatId: Long, noteId: Long): Int

    @Query("SELECT COUNT(*) FROM notes WHERE chat_id = :chatId")
    fun countByChatId(chatId: Long): Int
}
