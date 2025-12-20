package travesium.userservice.event

import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import traversium.audit.kafka.AuditStreamData
import traversium.commonmultitenancy.TenantContext
import travesium.userservice.kafka.KafkaProperties

/**
 * @author Maja Razinger
 */
@Component
@ConditionalOnProperty(name = ["spring.kafka.audit-topic"])
class AuditEventListener(
    private val kafkaTemplate: KafkaTemplate<String, Any>,
    private val kafkaProperties: KafkaProperties
): Logging {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun sendAuditDataToKafka(auditDto: AuditStreamData) {
        logger.info { "Sending audit data to kafka" }
        val tenantId = TenantContext.getTenant()

        val record = ProducerRecord<String, Any>(kafkaProperties.auditTopic!!, auditDto)
        tenantId.let {
            record.headers().add(RecordHeader("tenantId", it.toByteArray()))
        }

        kafkaTemplate.send(record)
        logger.info { "Successfully sent audit data to kafka" }
    }
}