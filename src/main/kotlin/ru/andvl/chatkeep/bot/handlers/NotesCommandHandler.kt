package ru.andvl.chatkeep.bot.handlers

import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.types.chat.GroupChat
import dev.inmo.tgbotapi.types.chat.SupergroupChat
import dev.inmo.tgbotapi.types.message.content.TextContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.domain.service.NotesService
import ru.andvl.chatkeep.domain.service.moderation.AdminCacheService

@Component
class NotesCommandHandler(
    private val notesService: NotesService,
    private val adminCacheService: AdminCacheService
) : Handler {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun BehaviourContext.register() {
        // /note [name] - Get a note
        onCommand("note", requireOnlyCommandInMessage = false) { message ->
            if (message.chat !is GroupChat && message.chat !is SupergroupChat) {
                reply(message, "This command can only be used in group chats")
                return@onCommand
            }

            val chatId = message.chat.id.chatId.long
            val messageText = (message.content as? TextContent)?.text ?: return@onCommand
            val noteName = messageText.substringAfter("/note").trim()

            if (noteName.isEmpty()) {
                reply(message, "Usage: /note [name]")
                return@onCommand
            }

            withContext(Dispatchers.IO) {
                try {
                    val note = notesService.getNote(chatId, noteName)
                    if (note == null) {
                        reply(message, "📝 Note '$noteName' not found")
                    } else {
                        reply(message, note.content)
                    }
                } catch (e: Exception) {
                    logger.error("Failed to get note: ${e.message}", e)
                    reply(message, "❌ Failed to retrieve note")
                }
            }
        }

        // /save [name] [content] - Save a note (admin only)
        onCommand("save", requireOnlyCommandInMessage = false) { message ->
            if (message.chat !is GroupChat && message.chat !is SupergroupChat) {
                reply(message, "This command can only be used in group chats")
                return@onCommand
            }

            val userId = (message as? dev.inmo.tgbotapi.types.message.abstracts.FromUserMessage)?.from?.id?.chatId?.long ?: return@onCommand
            val chatId = message.chat.id.chatId.long

            // Check if user is admin
            val isAdmin = withContext(Dispatchers.IO) {
                adminCacheService.isAdmin(userId, chatId, forceRefresh = false)
            }

            if (!isAdmin) {
                reply(message, "❌ Only admins can save notes")
                return@onCommand
            }

            val messageText = (message.content as? TextContent)?.text ?: return@onCommand
            val args = messageText.substringAfter("/save").trim()
            val parts = args.split(" ", limit = 2)

            if (parts.size < 2) {
                reply(message, "Usage: /save [name] [content]")
                return@onCommand
            }

            val noteName = parts[0]
            val content = parts[1]

            withContext(Dispatchers.IO) {
                try {
                    notesService.createNote(chatId, noteName, content, userId)
                    reply(message, "✅ Note '$noteName' saved")
                    logger.info("Note saved: chatId=$chatId, name=$noteName")
                } catch (e: IllegalStateException) {
                    reply(message, "❌ Note '$noteName' already exists")
                } catch (e: Exception) {
                    logger.error("Failed to save note: ${e.message}", e)
                    reply(message, "❌ Failed to save note")
                }
            }
        }

        // /notes - List all notes
        onCommand("notes") { message ->
            if (message.chat !is GroupChat && message.chat !is SupergroupChat) {
                reply(message, "This command can only be used in group chats")
                return@onCommand
            }

            val chatId = message.chat.id.chatId.long

            withContext(Dispatchers.IO) {
                try {
                    val notes = notesService.getAllNotes(chatId)
                    if (notes.isEmpty()) {
                        reply(message, "📝 No notes saved yet")
                    } else {
                        val notesList = notes.joinToString("\n") { "• ${it.noteName}" }
                        reply(message, "📝 Saved Notes:\n\n$notesList\n\nUse /note [name] to view a note")
                    }
                } catch (e: Exception) {
                    logger.error("Failed to list notes: ${e.message}", e)
                    reply(message, "❌ Failed to list notes")
                }
            }
        }

        // /delnote [name] - Delete a note (admin only)
        onCommand("delnote", requireOnlyCommandInMessage = false) { message ->
            if (message.chat !is GroupChat && message.chat !is SupergroupChat) {
                reply(message, "This command can only be used in group chats")
                return@onCommand
            }

            val userId = (message as? dev.inmo.tgbotapi.types.message.abstracts.FromUserMessage)?.from?.id?.chatId?.long ?: return@onCommand
            val chatId = message.chat.id.chatId.long

            // Check if user is admin
            val isAdmin = withContext(Dispatchers.IO) {
                adminCacheService.isAdmin(userId, chatId, forceRefresh = false)
            }

            if (!isAdmin) {
                reply(message, "❌ Only admins can delete notes")
                return@onCommand
            }

            val messageText = (message.content as? TextContent)?.text ?: return@onCommand
            val noteName = messageText.substringAfter("/delnote").trim()

            if (noteName.isEmpty()) {
                reply(message, "Usage: /delnote [name]")
                return@onCommand
            }

            withContext(Dispatchers.IO) {
                try {
                    val note = notesService.getNote(chatId, noteName)
                    if (note == null) {
                        reply(message, "📝 Note '$noteName' not found")
                    } else {
                        notesService.deleteNote(chatId, note.id)
                        reply(message, "✅ Note '$noteName' deleted")
                        logger.info("Note deleted: chatId=$chatId, name=$noteName")
                    }
                } catch (e: Exception) {
                    logger.error("Failed to delete note: ${e.message}", e)
                    reply(message, "❌ Failed to delete note")
                }
            }
        }
    }
}
