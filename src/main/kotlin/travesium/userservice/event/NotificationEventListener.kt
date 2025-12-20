package travesium.userservice.event

import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import traversium.commonmultitenancy.TenantContext
import traversium.notification.kafka.NotificationStreamData
import travesium.userservice.kafka.KafkaProperties

/**
 * @author Maja Razinger
 */
@Component
@ConditionalOnProperty(name = ["spring.kafka.notification-topic"])
class NotificationEventListener(
    private val kafkaTemplate: KafkaTemplate<String, Any>,
    private val kafkaProperties: KafkaProperties
): Logging {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun sendNotificationDataToKafka(notification: NotificationStreamData) {
        logger.info { "Sending notification data to kafka" }
        val tenantId = TenantContext.getTenant()

        val record = ProducerRecord<String, Any>(kafkaProperties.notificationTopic!!, notification)
        tenantId.let {
            record.headers().add(RecordHeader("tenantId", it.toByteArray()))
        }

        kafkaTemplate.send(record)
        logger.info { "Successfully sent notification data to kafka" }
    }
}