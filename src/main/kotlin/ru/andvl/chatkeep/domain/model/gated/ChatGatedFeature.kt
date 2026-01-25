package ru.andvl.chatkeep.domain.model.gated

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.PersistenceCreator
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("chat_gated_features")
data class ChatGatedFeature @PersistenceCreator constructor(
    @Column("chat_id")
    val chatId: Long,

    @Column("feature_key")
    val featureKey: String,

    @Column("enabled")
    val enabled: Boolean = false,

    @Column("enabled_at")
    val enabledAt: Instant? = null,

    @Column("enabled_by")
    val enabledBy: Long? = null,

    @Id
    @Column("id")
    private val pk: Long? = null
) : Persistable<Long> {

    @Transient
    private var _isNew: Boolean = false

    override fun getId(): Long? = pk

    override fun isNew(): Boolean = _isNew || pk == null

    companion object {
        fun createNew(
            chatId: Long,
            featureKey: String,
            enabled: Boolean = false,
            enabledBy: Long? = null
        ): ChatGatedFeature {
            return ChatGatedFeature(
                chatId = chatId,
                featureKey = featureKey,
                enabled = enabled,
                enabledAt = if (enabled) Instant.now() else null,
                enabledBy = if (enabled) enabledBy else null,
                pk = null
            ).also { it._isNew = true }
        }
    }
}
