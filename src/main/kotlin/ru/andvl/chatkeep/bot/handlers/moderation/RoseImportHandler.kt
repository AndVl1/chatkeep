package ru.andvl.chatkeep.bot.handlers.moderation

import dev.inmo.tgbotapi.extensions.api.files.downloadFile
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.requests.abstracts.asMultipartFile
import dev.inmo.tgbotapi.types.chat.PrivateChat
import dev.inmo.tgbotapi.types.message.abstracts.FromUserMessage
import dev.inmo.tgbotapi.types.message.content.DocumentContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import ru.andvl.chatkeep.bot.handlers.Handler
import ru.andvl.chatkeep.bot.util.RoseImportParser
import ru.andvl.chatkeep.domain.service.moderation.AdminCacheService
import ru.andvl.chatkeep.domain.service.moderation.AdminSessionService
import ru.andvl.chatkeep.domain.service.moderation.BlocklistService

@Component
class RoseImportHandler(
    private val blocklistService: BlocklistService,
    private val adminSessionService: AdminSessionService,
    private val adminCacheService: AdminCacheService,
    private val objectMapper: ObjectMapper
) : Handler {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val MAX_FILE_SIZE_BYTES = 1_000_000 // 1MB
    }

    override suspend fun BehaviourContext.register() {
        val privateFilter = { msg: dev.inmo.tgbotapi.types.message.abstracts.CommonMessage<*> ->
            msg.chat is PrivateChat
        }

        onCommand("import_rose", initialFilter = privateFilter) { message ->
            handleImportRose(message)
        }
    }

    private suspend fun BehaviourContext.handleImportRose(message: dev.inmo.tgbotapi.types.message.abstracts.CommonMessage<*>) {
        try {
            val userId = (message as? FromUserMessage)?.from?.id?.chatId?.long ?: return

            val session = withContext(Dispatchers.IO) {
                adminSessionService.getSession(userId)
            } ?: run {
                reply(message, "You must be connected to a chat first. Use /connect <chat_id>")
                return
            }

            val chatId = session.connectedChatId

            val isAdmin = withContext(Dispatchers.IO) {
                adminCacheService.isAdmin(userId, chatId)
            }

            if (!isAdmin) {
                reply(message, "You are not an admin in the connected chat.")
                return
            }

            val documentContent = message.content as? DocumentContent
            if (documentContent == null) {
                reply(message, "Please attach a Rose Bot export file (.txt or .json) with the /import_rose command")
                return
            }

            val document = documentContent.media
            val fileName = document.fileName?.lowercase() ?: ""

            if (!fileName.endsWith(".txt") && !fileName.endsWith(".json")) {
                reply(message, "File must be a .txt or .json file")
                return
            }

            // Check file size before downloading
            val fileSize = document.fileSize ?: 0
            if (fileSize > MAX_FILE_SIZE_BYTES) {
                reply(message, "File too large. Maximum size is 1MB.")
                logger.warn("Rejected Rose import: file too large (${fileSize} bytes)")
                return
            }

            val fileBytes = downloadFile(document)
            val fileContent = String(fileBytes)

            val parseResult = RoseImportParser.parse(fileContent, objectMapper)

            if (parseResult.items.isEmpty() && parseResult.skippedCount == 0) {
                val prefix = adminSessionService.formatReplyPrefix(session)
                reply(message, "$prefix\n\nNo blocklist patterns found in the file or file format is invalid.")
                return
            }

            val importResult = withContext(Dispatchers.IO) {
                blocklistService.importPatterns(chatId, parseResult.items)
            }

            val prefix = adminSessionService.formatReplyPrefix(session)
            val total = importResult.added + importResult.updated
            val skippedTotal = parseResult.skippedCount

            val resultMessage = buildString {
                append("$prefix\n\n")
                append("Imported $total patterns")
                if (importResult.updated > 0) {
                    append(" (${importResult.updated} updated, ${importResult.added} new)")
                }
                if (skippedTotal > 0) {
                    append(", $skippedTotal skipped")
                }
                append(".")
            }

            reply(message, resultMessage)
            logger.info("Rose import completed: chatId=$chatId, added=${importResult.added}, updated=${importResult.updated}, skipped=$skippedTotal")
        } catch (e: Exception) {
            logger.error("/import_rose: Failed", e)
            reply(message, "An error occurred while importing. Please check the file format and try again.")
        }
    }
}
