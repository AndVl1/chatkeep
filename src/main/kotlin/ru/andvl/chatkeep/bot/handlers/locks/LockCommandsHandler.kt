package ru.andvl.chatkeep.bot.handlers.locks

import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.types.chat.GroupChat
import dev.inmo.tgbotapi.types.chat.PrivateChat
import dev.inmo.tgbotapi.types.chat.SupergroupChat
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.abstracts.FromUserMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.bot.handlers.Handler
import ru.andvl.chatkeep.bot.util.SessionAuthHelper
import ru.andvl.chatkeep.domain.model.locks.LockCategory
import ru.andvl.chatkeep.domain.model.locks.LockType
import ru.andvl.chatkeep.domain.model.moderation.ActionType
import ru.andvl.chatkeep.domain.model.moderation.PunishmentSource
import ru.andvl.chatkeep.domain.service.ChatService
import ru.andvl.chatkeep.domain.service.locks.LockSettingsService
import ru.andvl.chatkeep.domain.service.logchannel.LogChannelService
import ru.andvl.chatkeep.domain.service.logchannel.dto.ModerationLogEntry
import ru.andvl.chatkeep.domain.service.moderation.AdminCacheService

@Component
class LockCommandsHandler(
    private val lockSettingsService: LockSettingsService,
    private val sessionAuthHelper: SessionAuthHelper,
    private val adminCacheService: AdminCacheService,
    private val logChannelService: LogChannelService,
    private val chatService: ChatService
) : Handler {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun BehaviourContext.register() {
        // Filters for group and private chats
        val groupFilter = { msg: CommonMessage<*> ->
            msg.chat is GroupChat || msg.chat is SupergroupChat
        }
        val privateFilter = { msg: CommonMessage<*> ->
            msg.chat is PrivateChat
        }

        // /lock <type> [reason] - Lock a type
        onCommand("lock", requireOnlyCommandInMessage = false, initialFilter = groupFilter) { message ->
            handleLockInGroup(message)
        }
        onCommand("lock", requireOnlyCommandInMessage = false, initialFilter = privateFilter) { message ->
            handleLockInPrivate(message)
        }

        // /unlock <type> - Unlock a type
        onCommand("unlock", requireOnlyCommandInMessage = false, initialFilter = groupFilter) { message ->
            handleUnlockInGroup(message)
        }
        onCommand("unlock", requireOnlyCommandInMessage = false, initialFilter = privateFilter) { message ->
            handleUnlockInPrivate(message)
        }

        // /locks - Show all locks
        onCommand("locks", initialFilter = groupFilter) { message ->
            handleLocksInGroup(message)
        }
        onCommand("locks", initialFilter = privateFilter) { message ->
            handleLocksInPrivate(message)
        }

        // /locktypes - Show available lock types
        onCommand("locktypes") { message ->
            handleLockTypes(message)
        }

        // /lockwarns on|off - Toggle warnings
        onCommand("lockwarns", requireOnlyCommandInMessage = false, initialFilter = groupFilter) { message ->
            handleLockWarnsInGroup(message)
        }
        onCommand("lockwarns", requireOnlyCommandInMessage = false, initialFilter = privateFilter) { message ->
            handleLockWarnsInPrivate(message)
        }
    }

    // ========== IN-GROUP HANDLERS (Direct admin check) ==========

    private suspend fun BehaviourContext.handleLockInGroup(message: CommonMessage<*>) {
        val userId = (message as? FromUserMessage)?.from?.id?.chatId?.long ?: return
        val chatId = message.chat.id.chatId.long

        if (!withContext(Dispatchers.IO) { adminCacheService.isAdmin(userId, chatId) }) {
            reply(message, "You need to be an admin to use this command.")
            return
        }

        val args = parseArgs(message)
        if (args.isEmpty()) {
            reply(message, "Usage: /lock <type> [reason]\nUse /locktypes to see available types.")
            return
        }

        val lockTypeName = args[0].uppercase()
        val reason = args.drop(1).joinToString(" ").takeIf { it.isNotBlank() }

        val lockType = try {
            LockType.valueOf(lockTypeName)
        } catch (e: Exception) {
            reply(message, "Unknown lock type: $lockTypeName\nUse /locktypes to see available types.")
            return
        }

        withContext(Dispatchers.IO) {
            lockSettingsService.setLock(chatId, lockType, true, reason)
        }

        // Send log notification
        val chatSettings = withContext(Dispatchers.IO) { chatService.getSettings(chatId) }
        val adminUser = (message as? FromUserMessage)?.from
        logChannelService.logModerationAction(
            ModerationLogEntry(
                chatId = chatId,
                chatTitle = chatSettings?.chatTitle,
                adminId = userId,
                adminFirstName = adminUser?.firstName,
                adminLastName = adminUser?.lastName,
                adminUserName = adminUser?.username?.withoutAt,
                actionType = ActionType.LOCK_ENABLED,
                reason = lockType.name,
                source = PunishmentSource.MANUAL
            )
        )

        reply(message, buildString {
            append("Locked: ${lockType.description}")
            reason?.let { append("\nReason: $it") }
        })

        logger.info("Lock ${lockType.name} enabled for chat $chatId by user $userId")
    }

    private suspend fun BehaviourContext.handleUnlockInGroup(message: CommonMessage<*>) {
        val userId = (message as? FromUserMessage)?.from?.id?.chatId?.long ?: return
        val chatId = message.chat.id.chatId.long

        if (!withContext(Dispatchers.IO) { adminCacheService.isAdmin(userId, chatId) }) {
            reply(message, "You need to be an admin to use this command.")
            return
        }

        val args = parseArgs(message)
        if (args.isEmpty()) {
            reply(message, "Usage: /unlock <type>\nUse /locktypes to see available types.")
            return
        }

        val lockTypeName = args[0].uppercase()

        val lockType = try {
            LockType.valueOf(lockTypeName)
        } catch (e: Exception) {
            reply(message, "Unknown lock type: $lockTypeName\nUse /locktypes to see available types.")
            return
        }

        withContext(Dispatchers.IO) {
            lockSettingsService.setLock(chatId, lockType, false)
        }

        // Send log notification
        val chatSettings = withContext(Dispatchers.IO) { chatService.getSettings(chatId) }
        val adminUser = (message as? FromUserMessage)?.from
        logChannelService.logModerationAction(
            ModerationLogEntry(
                chatId = chatId,
                chatTitle = chatSettings?.chatTitle,
                adminId = userId,
                adminFirstName = adminUser?.firstName,
                adminLastName = adminUser?.lastName,
                adminUserName = adminUser?.username?.withoutAt,
                actionType = ActionType.LOCK_DISABLED,
                reason = lockType.name,
                source = PunishmentSource.MANUAL
            )
        )

        reply(message, "Unlocked: ${lockType.description}")

        logger.info("Lock ${lockType.name} disabled for chat $chatId by user $userId")
    }

    private suspend fun BehaviourContext.handleLocksInGroup(message: CommonMessage<*>) {
        val userId = (message as? FromUserMessage)?.from?.id?.chatId?.long ?: return
        val chatId = message.chat.id.chatId.long

        if (!withContext(Dispatchers.IO) { adminCacheService.isAdmin(userId, chatId) }) {
            reply(message, "You need to be an admin to use this command.")
            return
        }

        val locks = withContext(Dispatchers.IO) {
            lockSettingsService.getAllLocks(chatId)
        }

        if (locks.isEmpty()) {
            reply(message, "No locks configured.")
            return
        }

        val locksByCategory = locks.entries
            .groupBy { it.key.category }
            .toSortedMap(compareBy { it.name })

        val text = buildString {
            appendLine("Active locks:")
            appendLine()
            for ((category, categoryLocks) in locksByCategory) {
                appendLine("${category.name}:")
                categoryLocks.forEach { (type, config) ->
                    append("  ${type.name.lowercase()} - ${type.description}")
                    config.reason?.let { append(" (Reason: $it)") }
                    appendLine()
                }
                appendLine()
            }
        }

        reply(message, text)
    }

    private suspend fun BehaviourContext.handleLockWarnsInGroup(message: CommonMessage<*>) {
        val userId = (message as? FromUserMessage)?.from?.id?.chatId?.long ?: return
        val chatId = message.chat.id.chatId.long

        if (!withContext(Dispatchers.IO) { adminCacheService.isAdmin(userId, chatId) }) {
            reply(message, "You need to be an admin to use this command.")
            return
        }

        val args = parseArgs(message)
        if (args.isEmpty()) {
            reply(message, "Usage: /lockwarns <on|off>")
            return
        }

        val enabled = when (args[0].lowercase()) {
            "on", "enable", "yes" -> true
            "off", "disable", "no" -> false
            else -> {
                reply(message, "Usage: /lockwarns <on|off>")
                return
            }
        }

        withContext(Dispatchers.IO) {
            lockSettingsService.setLockWarns(chatId, enabled)
        }

        // Send log notification
        val chatSettings = withContext(Dispatchers.IO) { chatService.getSettings(chatId) }
        val adminUser = (message as? FromUserMessage)?.from
        logChannelService.logModerationAction(
            ModerationLogEntry(
                chatId = chatId,
                chatTitle = chatSettings?.chatTitle,
                adminId = userId,
                adminFirstName = adminUser?.firstName,
                adminLastName = adminUser?.lastName,
                adminUserName = adminUser?.username?.withoutAt,
                actionType = if (enabled) ActionType.LOCK_WARNS_ON else ActionType.LOCK_WARNS_OFF,
                source = PunishmentSource.MANUAL
            )
        )

        reply(message, "Lock warnings ${if (enabled) "enabled" else "disabled"}.")

        logger.info("Lock warns ${if (enabled) "enabled" else "disabled"} for chat $chatId by user $userId")
    }

    // ========== PRIVATE CHAT HANDLERS (Session-based) ==========

    private suspend fun BehaviourContext.handleLockInPrivate(message: CommonMessage<*>) {
        sessionAuthHelper.withSessionAuth(this, message) { ctx ->
            val args = parseArgs(message)
            if (args.isEmpty()) {
                reply(message, buildString {
                    appendLine(sessionAuthHelper.formatReplyPrefix(ctx.session))
                    appendLine()
                    append("Usage: /lock <type> [reason]\nUse /locktypes to see available types.")
                })
                return@withSessionAuth
            }

            val lockTypeName = args[0].uppercase()
            val reason = args.drop(1).joinToString(" ").takeIf { it.isNotBlank() }

            val lockType = try {
                LockType.valueOf(lockTypeName)
            } catch (e: Exception) {
                reply(message, buildString {
                    appendLine(sessionAuthHelper.formatReplyPrefix(ctx.session))
                    appendLine()
                    append("Unknown lock type: $lockTypeName\nUse /locktypes to see available types.")
                })
                return@withSessionAuth
            }

            withContext(Dispatchers.IO) {
                lockSettingsService.setLock(ctx.chatId, lockType, true, reason)
            }

            // Send log notification
            val admin = (message as? FromUserMessage)?.from ?: return@withSessionAuth
            val chatSettings = withContext(Dispatchers.IO) { chatService.getSettings(ctx.chatId) }
            logChannelService.logModerationAction(
                ModerationLogEntry(
                    chatId = ctx.chatId,
                    chatTitle = chatSettings?.chatTitle,
                    adminId = admin.id.chatId.long,
                    adminFirstName = admin.firstName,
                    adminLastName = admin.lastName,
                    adminUserName = admin.username?.withoutAt,
                    actionType = ActionType.LOCK_ENABLED,
                    reason = lockType.name,
                    source = PunishmentSource.MANUAL
                )
            )

            reply(message, buildString {
                appendLine(sessionAuthHelper.formatReplyPrefix(ctx.session))
                appendLine()
                append("Locked: ${lockType.description}")
                reason?.let { append("\nReason: $it") }
            })

            logger.info("Lock ${lockType.name} enabled for chat ${ctx.chatId} by user ${ctx.userId}")
        }
    }

    private suspend fun BehaviourContext.handleUnlockInPrivate(message: CommonMessage<*>) {
        sessionAuthHelper.withSessionAuth(this, message) { ctx ->
            val args = parseArgs(message)
            if (args.isEmpty()) {
                reply(message, buildString {
                    appendLine(sessionAuthHelper.formatReplyPrefix(ctx.session))
                    appendLine()
                    append("Usage: /unlock <type>\nUse /locktypes to see available types.")
                })
                return@withSessionAuth
            }

            val lockTypeName = args[0].uppercase()

            val lockType = try {
                LockType.valueOf(lockTypeName)
            } catch (e: Exception) {
                reply(message, buildString {
                    appendLine(sessionAuthHelper.formatReplyPrefix(ctx.session))
                    appendLine()
                    append("Unknown lock type: $lockTypeName\nUse /locktypes to see available types.")
                })
                return@withSessionAuth
            }

            withContext(Dispatchers.IO) {
                lockSettingsService.setLock(ctx.chatId, lockType, false)
            }

            // Send log notification
            val admin = (message as? FromUserMessage)?.from ?: return@withSessionAuth
            val chatSettings = withContext(Dispatchers.IO) { chatService.getSettings(ctx.chatId) }
            logChannelService.logModerationAction(
                ModerationLogEntry(
                    chatId = ctx.chatId,
                    chatTitle = chatSettings?.chatTitle,
                    adminId = admin.id.chatId.long,
                    adminFirstName = admin.firstName,
                    adminLastName = admin.lastName,
                    adminUserName = admin.username?.withoutAt,
                    actionType = ActionType.LOCK_DISABLED,
                    reason = lockType.name,
                    source = PunishmentSource.MANUAL
                )
            )

            reply(message, buildString {
                appendLine(sessionAuthHelper.formatReplyPrefix(ctx.session))
                appendLine()
                append("Unlocked: ${lockType.description}")
            })

            logger.info("Lock ${lockType.name} disabled for chat ${ctx.chatId} by user ${ctx.userId}")
        }
    }

    private suspend fun BehaviourContext.handleLocksInPrivate(message: CommonMessage<*>) {
        sessionAuthHelper.withSessionAuth(this, message) { ctx ->
            val locks = withContext(Dispatchers.IO) {
                lockSettingsService.getAllLocks(ctx.chatId)
            }

            if (locks.isEmpty()) {
                reply(message, buildString {
                    appendLine(sessionAuthHelper.formatReplyPrefix(ctx.session))
                    appendLine()
                    append("No locks configured.")
                })
                return@withSessionAuth
            }

            val locksByCategory = locks.entries
                .groupBy { it.key.category }
                .toSortedMap(compareBy { it.name })

            val text = buildString {
                appendLine(sessionAuthHelper.formatReplyPrefix(ctx.session))
                appendLine()
                appendLine("Active locks:")
                appendLine()
                for ((category, categoryLocks) in locksByCategory) {
                    appendLine("${category.name}:")
                    categoryLocks.forEach { (type, config) ->
                        append("  ${type.name.lowercase()} - ${type.description}")
                        config.reason?.let { append(" (Reason: $it)") }
                        appendLine()
                    }
                    appendLine()
                }
            }

            reply(message, text)
        }
    }

    private suspend fun BehaviourContext.handleLockWarnsInPrivate(message: CommonMessage<*>) {
        sessionAuthHelper.withSessionAuth(this, message) { ctx ->
            val args = parseArgs(message)
            if (args.isEmpty()) {
                reply(message, buildString {
                    appendLine(sessionAuthHelper.formatReplyPrefix(ctx.session))
                    appendLine()
                    append("Usage: /lockwarns <on|off>")
                })
                return@withSessionAuth
            }

            val enabled = when (args[0].lowercase()) {
                "on", "enable", "yes" -> true
                "off", "disable", "no" -> false
                else -> {
                    reply(message, buildString {
                        appendLine(sessionAuthHelper.formatReplyPrefix(ctx.session))
                        appendLine()
                        append("Usage: /lockwarns <on|off>")
                    })
                    return@withSessionAuth
                }
            }

            withContext(Dispatchers.IO) {
                lockSettingsService.setLockWarns(ctx.chatId, enabled)
            }

            // Send log notification
            val admin = (message as? FromUserMessage)?.from ?: return@withSessionAuth
            val chatSettings = withContext(Dispatchers.IO) { chatService.getSettings(ctx.chatId) }
            logChannelService.logModerationAction(
                ModerationLogEntry(
                    chatId = ctx.chatId,
                    chatTitle = chatSettings?.chatTitle,
                    adminId = admin.id.chatId.long,
                    adminFirstName = admin.firstName,
                    adminLastName = admin.lastName,
                    adminUserName = admin.username?.withoutAt,
                    actionType = if (enabled) ActionType.LOCK_WARNS_ON else ActionType.LOCK_WARNS_OFF,
                    source = PunishmentSource.MANUAL
                )
            )

            reply(message, buildString {
                appendLine(sessionAuthHelper.formatReplyPrefix(ctx.session))
                appendLine()
                append("Lock warnings ${if (enabled) "enabled" else "disabled"}.")
            })

            logger.info("Lock warns ${if (enabled) "enabled" else "disabled"} for chat ${ctx.chatId} by user ${ctx.userId}")
        }
    }

    // ========== COMMON HANDLERS ==========

    private suspend fun BehaviourContext.handleLockTypes(message: CommonMessage<*>) {
        val byCategory = LockType.entries
            .groupBy { it.category }
            .toSortedMap(compareBy { it.name })

        val text = buildString {
            appendLine("Available lock types:")
            appendLine()
            for ((category, types) in byCategory) {
                appendLine("${category.name}:")
                types.forEach { type ->
                    appendLine("  ${type.name.lowercase()} - ${type.description}")
                }
                appendLine()
            }
            appendLine("Use /lock <type> [reason] to enable a lock")
            appendLine("Use /unlock <type> to disable a lock")
            appendLine("Use /locks to view active locks")
        }

        reply(message, text)
    }

    // ========== HELPERS ==========

    private fun parseArgs(message: CommonMessage<*>): List<String> {
        val text = (message.content as? TextContent)?.text ?: return emptyList()
        return text.split(" ").drop(1).filter { it.isNotBlank() }
    }
}
