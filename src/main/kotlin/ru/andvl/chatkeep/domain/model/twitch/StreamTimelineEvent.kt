package ru.andvl.chatkeep.domain.model.twitch

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.PersistenceCreator
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("stream_timeline_events")
data class StreamTimelineEvent @PersistenceCreator constructor(
    @Id
    @Column("id")
    val id: Long? = null,

    @Column("stream_id")
    val streamId: Long,

    @Column("event_time")
    val eventTime: Instant = Instant.now(),

    @Column("stream_offset_seconds")
    val streamOffsetSeconds: Int,

    @Column("game_name")
    val gameName: String? = null,

    @Column("stream_title")
    val streamTitle: String? = null
) {

    companion object {
        fun createNew(
            streamId: Long,
            streamOffsetSeconds: Int,
            gameName: String?,
            streamTitle: String?
        ): StreamTimelineEvent {
            return StreamTimelineEvent(
                id = null,
                streamId = streamId,
                eventTime = Instant.now(),
                streamOffsetSeconds = streamOffsetSeconds,
                gameName = gameName,
                streamTitle = streamTitle
            )
        }
    }
}
