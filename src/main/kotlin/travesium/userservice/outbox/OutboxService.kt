package travesium.userservice.outbox

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import org.apache.logging.log4j.kotlin.logger
import org.springframework.data.domain.PageRequest
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import traversium.commonmultitenancy.TenantContext
import traversium.notification.kafka.NotificationStreamData
import travesium.userservice.db.repository.OutboxRepository
import travesium.userservice.kafka.KafkaProperties
import java.time.OffsetDateTime

/**
 * @author Maja Razinger
 */
@Service
class OutboxService(
    private val outboxRepository: OutboxRepository,
    private val kafkaTemplate: KafkaTemplate<String, Any>,
    private val kafkaProperties: KafkaProperties,
    private val objectMapper: ObjectMapper
) {

    companion object {
        private const val BATCH_SIZE = 100
        private const val MAX_RETRY_COUNT = 3
    }

    @Transactional
    fun processEventsForCurrentTenant() {
        try {
            val unprocessedEvents = outboxRepository.findUnprocessedEvents(PageRequest.of(0, BATCH_SIZE))

            if (unprocessedEvents.isEmpty()) {
                return
            }

            val tenantId = TenantContext.getTenant()
            logger.info { "Processing ${unprocessedEvents.size} outbox events for tenant $tenantId" }

            unprocessedEvents.forEach { event ->
                try {
                    when (event.eventType) {
                        "NOTIFICATION" -> {
                            if (!kafkaProperties.notificationTopic.isNullOrBlank()) {
                                publishNotificationEvent(event.payload!!, event.tenantId)
                                outboxRepository.markAsProcessed(event.id!!, OffsetDateTime.now())
                                logger.debug { "Successfully processed notification event ${event.id}" }
                            } else {
                                logger.debug { "Notification topic not configured, skipping event ${event.id}" }
                                // Event stays unprocessed
                            }
                        }
                        else -> {
                            logger.warn { "Unknown event type: ${event.eventType}, marking as processed" }
                            outboxRepository.markAsProcessed(event.id!!, OffsetDateTime.now())
                        }
                    }

                } catch (e: Exception) {
                    logger.error(e) { "Error processing outbox event ${event.id}: ${e.message}" }

                    if (event.retryCount < MAX_RETRY_COUNT) {
                        outboxRepository.incrementRetryCount(event.id!!, e.message ?: "Unknown error")
                        logger.info { "Incremented retry count for event ${event.id} (${event.retryCount + 1}/$MAX_RETRY_COUNT)" }
                    } else {
                        logger.error { "Event ${event.id} exceeded max retry count, marking as processed to prevent blocking" }
                        outboxRepository.markAsProcessed(event.id!!, OffsetDateTime.now())
                    }
                }
            }

        } catch (e: Exception) {
            logger.error(e) { "Error in outbox processor: ${e.message}" }
        }
    }

    @Transactional
    fun cleanupEventsForCurrentTenant(retentionHours: Long) {
        try {
            val cutoffTime = OffsetDateTime.now().minusHours(retentionHours)
            val deletedCount = outboxRepository.deleteProcessedEventsOlderThan(cutoffTime)

            if (deletedCount > 0) {
                val tenantId = TenantContext.getTenant()
                logger.info { "Cleaned up $deletedCount processed outbox events for tenant $tenantId older than $cutoffTime" }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error during outbox cleanup: ${e.message}" }
        }
    }

    private fun publishNotificationEvent(payload: String, tenantId: String?) {
        val notification = objectMapper.readValue(payload, NotificationStreamData::class.java)

        val record = ProducerRecord<String, Any>(kafkaProperties.notificationTopic!!, notification)
        tenantId?.let {
            record.headers().add(RecordHeader("tenantId", it.toByteArray()))
        }

        kafkaTemplate.send(record).get()
    }
}
