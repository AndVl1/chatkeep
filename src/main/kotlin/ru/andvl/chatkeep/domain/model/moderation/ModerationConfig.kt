package ru.andvl.chatkeep.domain.model.moderation

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("moderation_config")
data class ModerationConfig(
    @Id
    val id: Long? = null,

    @Column("chat_id")
    val chatId: Long,

    @Column("max_warnings")
    val maxWarnings: Int = 3,

    @Column("warning_ttl_hours")
    val warningTtlHours: Int = 24,

    @Column("threshold_action")
    val thresholdAction: String = "MUTE",

    @Column("threshold_duration_minutes")
    val thresholdDurationMinutes: Int? = 1440,

    @Column("default_blocklist_action")
    val defaultBlocklistAction: String = "WARN",

    @Column("log_channel_id")
    val logChannelId: Long? = null,

    @Column("clean_service_enabled")
    val cleanServiceEnabled: Boolean = false,

    @Column("created_at")
    val createdAt: Instant = Instant.now(),

    @Column("updated_at")
    val updatedAt: Instant = Instant.now()
)
