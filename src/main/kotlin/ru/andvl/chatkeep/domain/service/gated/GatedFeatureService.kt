package ru.andvl.chatkeep.domain.service.gated

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.andvl.chatkeep.domain.model.gated.ChatGatedFeature
import ru.andvl.chatkeep.domain.model.gated.GatedFeatureKey
import ru.andvl.chatkeep.infrastructure.repository.gated.ChatGatedFeatureRepository
import java.time.Instant

@Service
class GatedFeatureService(
    private val repository: ChatGatedFeatureRepository
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Get all features for a chat with their statuses
     */
    fun getFeatures(chatId: Long): List<FeatureStatus> {
        val enabledFeatures = repository.findByChatId(chatId)
            .associateBy { it.featureKey }

        return GatedFeatureKey.allFeatures().map { feature ->
            val dbFeature = enabledFeatures[feature.key]
            FeatureStatus(
                key = feature.key,
                enabled = dbFeature?.enabled ?: false,
                name = feature.displayName,
                description = feature.description,
                enabledAt = dbFeature?.enabledAt,
                enabledBy = dbFeature?.enabledBy
            )
        }
    }

    /**
     * Check if a specific feature is enabled for a chat
     */
    fun hasFeature(chatId: Long, featureKey: String): Boolean {
        return repository.findByChatIdAndFeatureKey(chatId, featureKey)?.enabled ?: false
    }

    /**
     * Enable or disable a feature
     * Returns the updated feature status
     */
    @Transactional
    fun setFeature(chatId: Long, featureKey: String, enabled: Boolean, userId: Long): FeatureStatus {
        val feature = GatedFeatureKey.fromKey(featureKey)
            ?: throw IllegalArgumentException("Unknown feature key: $featureKey")

        val existing = repository.findByChatIdAndFeatureKey(chatId, featureKey)
        logger.info("setFeature: chatId=$chatId, key=$featureKey, enabled=$enabled, existingFound=${existing != null}")

        val updated = if (existing != null) {
            existing.copy(
                enabled = enabled,
                enabledAt = if (enabled) Instant.now() else existing.enabledAt,
                enabledBy = if (enabled) userId else existing.enabledBy
            )
        } else {
            ChatGatedFeature.createNew(
                chatId = chatId,
                featureKey = featureKey,
                enabled = enabled,
                enabledBy = if (enabled) userId else null
            )
        }

        val saved = repository.save(updated)
        logger.info("Feature ${if (enabled) "enabled" else "disabled"} successfully: chatId=$chatId, key=$featureKey")

        return FeatureStatus(
            key = saved.featureKey,
            enabled = saved.enabled,
            name = feature.displayName,
            description = feature.description,
            enabledAt = saved.enabledAt,
            enabledBy = saved.enabledBy
        )
    }

    data class FeatureStatus(
        val key: String,
        val enabled: Boolean,
        val name: String,
        val description: String,
        val enabledAt: Instant? = null,
        val enabledBy: Long? = null
    )
}
