package ru.andvl.chatkeep.infrastructure.repository.twitch

import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import ru.andvl.chatkeep.domain.model.twitch.StreamTimelineEvent

interface StreamTimelineEventRepository : CrudRepository<StreamTimelineEvent, Long> {

    @Query("SELECT * FROM stream_timeline_events WHERE stream_id = :streamId ORDER BY event_time ASC")
    fun findByStreamId(streamId: Long): List<StreamTimelineEvent>
}
