package ru.andvl.chatkeep.infrastructure.repository.media

import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import ru.andvl.chatkeep.domain.model.media.MediaStorage
import java.time.Instant

@Repository
interface MediaStorageRepository : CrudRepository<MediaStorage, Long> {
    fun findByHash(hash: String): MediaStorage?

    @Modifying
    @Query(
        """
        DELETE FROM media_storage
        WHERE created_at < :cutoff
        AND telegram_file_id IS NULL
        """
    )
    fun deleteByCreatedAtBeforeAndTelegramFileIdIsNull(cutoff: Instant): Int
}
