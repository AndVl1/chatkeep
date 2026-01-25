package ru.andvl.chatkeep.infrastructure.repository.gated

import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import ru.andvl.chatkeep.domain.model.gated.ChatGatedFeature

interface ChatGatedFeatureRepository : CrudRepository<ChatGatedFeature, Long> {

    @Query("SELECT * FROM chat_gated_features WHERE chat_id = :chatId")
    fun findByChatId(chatId: Long): List<ChatGatedFeature>

    @Query("SELECT * FROM chat_gated_features WHERE chat_id = :chatId AND feature_key = :featureKey")
    fun findByChatIdAndFeatureKey(chatId: Long, featureKey: String): ChatGatedFeature?
}
