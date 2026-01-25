package ru.andvl.chatkeep.infrastructure.repository.twitch

import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import ru.andvl.chatkeep.domain.model.twitch.TwitchNotificationSettings

interface TwitchNotificationSettingsRepository : CrudRepository<TwitchNotificationSettings, Long> {

    @Query("SELECT * FROM twitch_notification_settings WHERE chat_id = :chatId")
    fun findByChatId(chatId: Long): TwitchNotificationSettings?
}
