package ru.andvl.chatkeep.bot.handlers

import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.utils.buildEntities
import org.springframework.stereotype.Component

@Component
class HelpCommandHandler : Handler {

    override suspend fun BehaviourContext.register() {
        onCommand("help") { message ->
            val helpText = buildEntities {
                +"📚 Chatkeep Bot Commands\n\n"
                +"⚖️ Moderation:\n"
                +"/warn @user [reason] - Issue a warning\n"
                +"/unwarn @user - Remove warnings\n"
                +"/mute @user [duration] [reason] - Mute user\n"
                +"/unmute @user - Unmute user\n"
                +"/ban @user [duration] [reason] - Ban user\n"
                +"/unban @user - Unban user\n"
                +"/kick @user [reason] - Kick user\n\n"
                +"📝 Content Management:\n"
                +"/rules - Show chat rules\n"
                +"/setrules [text] - Set chat rules\n"
                +"/note [name] - Retrieve a saved note\n"
                +"/save [name] [content] - Save a note\n"
                +"/notes - List all notes\n"
                +"/delnote [name] - Delete a note\n\n"
                +"🛡 Filters & Protection:\n"
                +"/blocklist - Manage word filters\n"
                +"/locks - Manage content locks\n\n"
                +"👨‍💼 Admin Tools:\n"
                +"/connect - Connect to chat (admin panel)\n"
                +"/disconnect - Disconnect from chat\n"
                +"/exportlogs - Export moderation logs\n"
                +"/stats - Show chat statistics\n\n"
                +"⚙️ Settings:\n"
                +"Use the Mini App (tap menu button) to configure:\n"
                +"• Warning thresholds\n"
                +"• Welcome messages\n"
                +"• Anti-flood protection\n"
                +"• Log channel\n"
                +"• And more...\n\n"
                +"For detailed help, visit: https://chatkeep.app/docs"
            }

            reply(message, helpText)
        }
    }
}
