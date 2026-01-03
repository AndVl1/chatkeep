package ru.andvl.chatkeep.bot.handlers.moderation

import dev.inmo.tgbotapi.extensions.api.files.downloadFile
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onDocument
import dev.inmo.tgbotapi.types.chat.PrivateChat
import dev.inmo.tgbotapi.types.message.content.DocumentContent
import dev.inmo.tgbotapi.types.message.textsources.BotCommandTextSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import ru.andvl.chatkeep.bot.handlers.Handler
import ru.andvl.chatkeep.bot.util.RoseImportParser
import ru.andvl.chatkeep.bot.util.SessionAuthHelper
import ru.andvl.chatkeep.domain.service.moderation.BlocklistService

@Component
class RoseImportHandler(
    private val blocklistService: BlocklistService,
    private val sessionAuthHelper: SessionAuthHelper,
    private val objectMapper: ObjectMapper
) : Handler {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val MAX_FILE_SIZE_BYTES = 1_000_000 // 1MB
    }

    override suspend fun BehaviourContext.register() {
        // Use onDocument to handle file attachments with /import_rose command in caption
        onDocument(
            initialFilter = { message ->
                message.chat is PrivateChat && hasImportRoseCommand(message.content)
            }
        ) { message ->
            handleImportRose(message)
        }
    }

    private fun hasImportRoseCommand(content: DocumentContent): Boolean {
        // Check if caption contains /import_rose command
        return content.textSources
            ?.filterIsInstance<BotCommandTextSource>()
            ?.any { it.command == "import_rose" }
            ?: false
    }

    private suspend fun BehaviourContext.handleImportRose(message: dev.inmo.tgbotapi.types.message.abstracts.CommonMessage<DocumentContent>) {
        sessionAuthHelper.withSessionAuth(this, message) { ctx ->
            try {
                val documentContent = message.content
                val document = documentContent.media

                val fileName = document.fileName?.lowercase() ?: ""

                if (!fileName.endsWith(".txt") && !fileName.endsWith(".json")) {
                    reply(message, "File must be a .txt or .json file")
                    return@withSessionAuth
                }

                // Check file size before downloading
                val fileSize = document.fileSize ?: 0
                if (fileSize > MAX_FILE_SIZE_BYTES) {
                    reply(message, "File too large. Maximum size is 1MB.")
                    logger.warn("Rejected Rose import: file too large (${fileSize} bytes)")
                    return@withSessionAuth
                }

                val fileBytes = downloadFile(document)
                val fileContent = String(fileBytes)

                val parseResult = RoseImportParser.parse(fileContent, objectMapper)

                if (parseResult.items.isEmpty() && parseResult.skippedCount == 0) {
                    val prefix = sessionAuthHelper.formatReplyPrefix(ctx.session)
                    reply(message, "$prefix\n\nNo blocklist patterns found in the file or file format is invalid.")
                    return@withSessionAuth
                }

                val importResult = withContext(Dispatchers.IO) {
                    blocklistService.importPatterns(ctx.chatId, parseResult.items)
                }

                val prefix = sessionAuthHelper.formatReplyPrefix(ctx.session)
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
                logger.info("Rose import completed: chatId=${ctx.chatId}, added=${importResult.added}, updated=${importResult.updated}, skipped=$skippedTotal")
            } catch (e: Exception) {
                logger.error("/import_rose: Failed", e)
                reply(message, "An error occurred while importing. Please check the file format and try again.")
            }
        }
    }
}
