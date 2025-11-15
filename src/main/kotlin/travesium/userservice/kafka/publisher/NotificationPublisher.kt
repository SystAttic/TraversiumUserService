package travesium.userservice.kafka.publisher

import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import traversium.commonmultitenancy.TenantContext
import traversium.notification.kafka.NotificationStreamData
import travesium.userservice.kafka.KafkaProperties

/**
 * @author Maja Razinger
 */
@Component
@ConditionalOnProperty(name = ["spring.kafka.notification-topic"])
class NotificationPublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>,
    private val kafkaProperties: KafkaProperties
) {

    fun publish(notification: NotificationStreamData) {
        val tenantId = TenantContext.getTenant()

        val record = ProducerRecord<String, Any>(kafkaProperties.notificationTopic!!, notification)
        tenantId.let {
            record.headers().add(RecordHeader("tenantId", it.toByteArray()))
        }

        kafkaTemplate.send(record)
    }
}