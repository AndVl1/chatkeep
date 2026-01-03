package ru.andvl.chatkeep.infrastructure.repository.channelreply

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import ru.andvl.chatkeep.domain.model.channelreply.ChannelReplySettings

@Repository
interface ChannelReplySettingsRepository : CrudRepository<ChannelReplySettings, Long> {
    fun findByChatId(chatId: Long): ChannelReplySettings?
    fun deleteByChatId(chatId: Long)
}
