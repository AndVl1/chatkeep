package ru.andvl.chatkeep.domain.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.andvl.chatkeep.domain.model.Note
import ru.andvl.chatkeep.infrastructure.repository.NoteRepository
import java.time.Instant

@Service
class NotesService(
    private val repository: NoteRepository
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun getAllNotes(chatId: Long): List<Note> {
        return repository.findAllByChatId(chatId)
    }

    fun getNote(chatId: Long, noteName: String): Note? {
        return repository.findByChatIdAndNoteName(chatId, noteName)
    }

    fun getNoteById(chatId: Long, noteId: Long): Note? {
        return repository.findById(noteId).orElse(null)?.let {
            if (it.chatId == chatId) it else null
        }
    }

    @Transactional
    fun createNote(chatId: Long, noteName: String, content: String, createdBy: Long): Note {
        // Check if note already exists
        val existing = repository.findByChatIdAndNoteName(chatId, noteName)
        if (existing != null) {
            throw IllegalStateException("Note with name '$noteName' already exists")
        }

        val note = Note(
            chatId = chatId,
            noteName = noteName,
            content = content,
            createdBy = createdBy
        )

        val saved = repository.save(note)
        logger.info("Created note: chatId=$chatId, name=$noteName")
        return saved
    }

    @Transactional
    fun updateNote(chatId: Long, noteId: Long, content: String): Note {
        val existing = repository.findById(noteId).orElse(null)
            ?: throw IllegalArgumentException("Note not found: $noteId")

        if (existing.chatId != chatId) {
            throw IllegalArgumentException("Note does not belong to chat $chatId")
        }

        val updated = existing.copy(
            content = content,
            updatedAt = Instant.now()
        )

        val saved = repository.save(updated)
        logger.info("Updated note: chatId=$chatId, noteId=$noteId")
        return saved
    }

    @Transactional
    fun deleteNote(chatId: Long, noteId: Long): Boolean {
        val deleted = repository.deleteByChatIdAndId(chatId, noteId)
        if (deleted > 0) {
            logger.info("Deleted note: chatId=$chatId, noteId=$noteId")
        }
        return deleted > 0
    }

    fun countNotes(chatId: Long): Int {
        return repository.countByChatId(chatId)
    }
}
