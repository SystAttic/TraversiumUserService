package travesium.userservice.db.model

import jakarta.persistence.*
import java.time.OffsetDateTime

/**
 * Outbox pattern entity for reliable event publishing
 * @author Maja Razinger
 */
@Entity
@Table(name = Outbox.TABLE_NAME, indexes = [
    Index(name = "idx_outbox_processed", columnList = "processed"),
    Index(name = "idx_outbox_created_at", columnList = "created_at")
])
data class Outbox(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, nullable = false, updatable = false)
    val id: Long? = null,

    @Column(name = "event_type", nullable = false, length = 100)
    val eventType: String? = null,

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    val payload: String? = null,

    @Column(name = "tenant_id", length = 100)
    val tenantId: String? = null,

    @Column(name = "processed", nullable = false)
    val processed: Boolean = false,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "processed_at")
    val processedAt: OffsetDateTime? = null,

    @Column(name = "retry_count", nullable = false)
    val retryCount: Int = 0,

    @Column(name = "error_message", columnDefinition = "TEXT")
    val errorMessage: String? = null
) {
    companion object {
        const val TABLE_NAME = "outbox"
    }
}
