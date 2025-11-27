package travesium.userservice.db.repository

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import travesium.userservice.db.model.Outbox
import java.time.OffsetDateTime

/**
 * @author Maja Razinger
 */
interface OutboxRepository : JpaRepository<Outbox, Long> {

    @Query("SELECT o FROM Outbox o WHERE o.processed = false ORDER BY o.createdAt ASC")
    fun findUnprocessedEvents(pageable: Pageable): List<Outbox>

    @Modifying
    @Transactional
    @Query("UPDATE Outbox o SET o.processed = true, o.processedAt = :processedAt WHERE o.id = :id")
    fun markAsProcessed(@Param("id") id: Long, @Param("processedAt") processedAt: OffsetDateTime)

    @Modifying
    @Transactional
    @Query("UPDATE Outbox o SET o.retryCount = o.retryCount + 1, o.errorMessage = :errorMessage WHERE o.id = :id")
    fun incrementRetryCount(@Param("id") id: Long, @Param("errorMessage") errorMessage: String)

    @Modifying
    @Transactional
    @Query("DELETE FROM Outbox o WHERE o.processed = true AND o.processedAt < :cutoffTime")
    fun deleteProcessedEventsOlderThan(@Param("cutoffTime") cutoffTime: OffsetDateTime): Int
}
