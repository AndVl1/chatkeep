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
    private val lockSettingsService: ru.andvl.chatkeep.domain.service.locks.LockSettingsService,
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

    private fun importLocks(chatId: Long, roseLocks: RoseImportParser.RoseLocks): RoseImportParser.LocksImportResult {
        var lockedCount = 0

        // Import lock settings
        for ((roseName, config) in roseLocks.locks) {
            if (!config.locked) continue

            val lockType = mapRoseLockType(roseName) ?: continue

            lockSettingsService.setLock(
                chatId = chatId,
                lockType = lockType,
                locked = true,
                reason = config.reason.takeIf { it.isNotBlank() }
            )
            lockedCount++
        }

        // Import allowlisted URLs
        var allowlistedUrlCount = 0
        for (urlEntry in roseLocks.allowlistedUrl) {
            val url = urlEntry.url.trim()
            if (url.isNotEmpty()) {
                lockSettingsService.addToAllowlist(
                    chatId = chatId,
                    type = ru.andvl.chatkeep.domain.model.locks.AllowlistType.URL,
                    pattern = url
                )
                allowlistedUrlCount++
            }
        }

        // Set lock_warns
        lockSettingsService.setLockWarns(chatId, roseLocks.lockWarns)

        logger.info("Imported locks for chat $chatId: $lockedCount locks enabled, $allowlistedUrlCount URLs allowlisted, lockWarns=${roseLocks.lockWarns}")

        return RoseImportParser.LocksImportResult(
            lockedCount = lockedCount,
            allowlistedUrlCount = allowlistedUrlCount,
            lockWarns = roseLocks.lockWarns
        )
    }

    private fun mapRoseLockType(roseName: String): ru.andvl.chatkeep.domain.model.locks.LockType? {
        return when (roseName.lowercase()) {
            "photo" -> ru.andvl.chatkeep.domain.model.locks.LockType.PHOTO
            "video" -> ru.andvl.chatkeep.domain.model.locks.LockType.VIDEO
            "audio" -> ru.andvl.chatkeep.domain.model.locks.LockType.AUDIO
            "voice" -> ru.andvl.chatkeep.domain.model.locks.LockType.VOICE
            "document" -> ru.andvl.chatkeep.domain.model.locks.LockType.DOCUMENT
            "sticker" -> ru.andvl.chatkeep.domain.model.locks.LockType.STICKER
            "gif" -> ru.andvl.chatkeep.domain.model.locks.LockType.GIF
            "videonote" -> ru.andvl.chatkeep.domain.model.locks.LockType.VIDEONOTE
            "contact" -> ru.andvl.chatkeep.domain.model.locks.LockType.CONTACT
            "location" -> ru.andvl.chatkeep.domain.model.locks.LockType.LOCATION
            "venue" -> ru.andvl.chatkeep.domain.model.locks.LockType.VENUE
            "dice", "emojigame" -> ru.andvl.chatkeep.domain.model.locks.LockType.DICE
            "poll" -> ru.andvl.chatkeep.domain.model.locks.LockType.POLL
            "game" -> ru.andvl.chatkeep.domain.model.locks.LockType.GAME
            "forward" -> ru.andvl.chatkeep.domain.model.locks.LockType.FORWARD
            "forwarduser" -> ru.andvl.chatkeep.domain.model.locks.LockType.FORWARDUSER
            "forwardchannel" -> ru.andvl.chatkeep.domain.model.locks.LockType.FORWARDCHANNEL
            "forwardbot" -> ru.andvl.chatkeep.domain.model.locks.LockType.FORWARDBOT
            "url" -> ru.andvl.chatkeep.domain.model.locks.LockType.URL
            "button" -> ru.andvl.chatkeep.domain.model.locks.LockType.BUTTON
            "invitelink", "invite" -> ru.andvl.chatkeep.domain.model.locks.LockType.INVITE
            "text" -> ru.andvl.chatkeep.domain.model.locks.LockType.TEXT
            "command" -> ru.andvl.chatkeep.domain.model.locks.LockType.COMMANDS
            "email" -> ru.andvl.chatkeep.domain.model.locks.LockType.EMAIL
            "phone" -> ru.andvl.chatkeep.domain.model.locks.LockType.PHONE
            "spoiler" -> ru.andvl.chatkeep.domain.model.locks.LockType.SPOILER
            "mention" -> ru.andvl.chatkeep.domain.model.locks.LockType.MENTION
            "hashtag" -> ru.andvl.chatkeep.domain.model.locks.LockType.HASHTAG
            "cashtag" -> ru.andvl.chatkeep.domain.model.locks.LockType.CASHTAG
            "emoji", "emojicustom" -> ru.andvl.chatkeep.domain.model.locks.LockType.EMOJI
            "inline" -> ru.andvl.chatkeep.domain.model.locks.LockType.INLINE
            "rtl" -> ru.andvl.chatkeep.domain.model.locks.LockType.RTLCHAR
            "anonchannel" -> ru.andvl.chatkeep.domain.model.locks.LockType.ANONCHANNEL
            "comment" -> ru.andvl.chatkeep.domain.model.locks.LockType.COMMENT
            "album" -> ru.andvl.chatkeep.domain.model.locks.LockType.ALBUM
            "topic" -> ru.andvl.chatkeep.domain.model.locks.LockType.TOPIC
            "premium", "stickerpremium" -> ru.andvl.chatkeep.domain.model.locks.LockType.PREMIUM
            else -> {
                logger.debug("Unknown Rose lock type: $roseName")
                null
            }
        }
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

                // Parse blocklists
                val parseResult = RoseImportParser.parse(fileContent, objectMapper)

                // Parse locks
                val roseLocks = RoseImportParser.parseLocks(fileContent, objectMapper)

                if (parseResult.items.isEmpty() && parseResult.skippedCount == 0 && roseLocks == null) {
                    val prefix = sessionAuthHelper.formatReplyPrefix(ctx.session)
                    reply(message, "$prefix\n\nNo blocklist patterns or locks found in the file or file format is invalid.")
                    return@withSessionAuth
                }

                // Import blocklists
                val importResult = withContext(Dispatchers.IO) {
                    blocklistService.importPatterns(ctx.chatId, parseResult.items)
                }

                // Import locks
                val locksResult = roseLocks?.let { locks ->
                    withContext(Dispatchers.IO) {
                        importLocks(ctx.chatId, locks)
                    }
                }

                val prefix = sessionAuthHelper.formatReplyPrefix(ctx.session)
                val total = importResult.added + importResult.updated
                val skippedTotal = parseResult.skippedCount

                val resultMessage = buildString {
                    append("$prefix\n\n")
                    append("Imported $total blocklist patterns")
                    if (importResult.updated > 0) {
                        append(" (${importResult.updated} updated, ${importResult.added} new)")
                    }
                    if (skippedTotal > 0) {
                        append(", $skippedTotal skipped")
                    }
                    append(".")

                    locksResult?.let { result ->
                        append("\n\nLocks: ${result.lockedCount} enabled")
                        if (result.allowlistedUrlCount > 0) {
                            append(", ${result.allowlistedUrlCount} allowlisted URLs")
                        }
                        if (result.lockWarns) {
                            append(" (lock warns enabled)")
                        }
                    }
                }

                reply(message, resultMessage)
                logger.info("Rose import completed: chatId=${ctx.chatId}, patterns_added=${importResult.added}, patterns_updated=${importResult.updated}, patterns_skipped=$skippedTotal, locks=${locksResult?.lockedCount ?: 0}")
            } catch (e: Exception) {
                logger.error("/import_rose: Failed", e)
                reply(message, "An error occurred while importing. Please check the file format and try again.")
            }
        }
    }
}
