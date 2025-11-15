package travesium.userservice.event

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import travesium.userservice.kafka.KafkaProperties
import travesium.userservice.kafka.data.ReportingStreamData
import java.util.concurrent.TimeUnit

/**
 * @author Maja Razinger
 */
@Component
@ConditionalOnProperty(name = ["spring.kafka.reporting-topic"])
class UserEventListener(
    private val kafkaTemplate: KafkaTemplate<String, Any>,
    private val kafkaProperties: KafkaProperties
)  {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun sendReportingDataToKafka(event: ReportingStreamData) {
        kafkaTemplate.send(
            kafkaProperties.reportingTopic!!,
            event,
        )[kafkaProperties.clientConfirmationTimeout, TimeUnit.SECONDS]
    }
}