package travesium.userservice.kafka

import jakarta.annotation.PostConstruct
import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * @author Maja Razinger
 */
@ConfigurationProperties(prefix = "spring.kafka")
class KafkaProperties {
    lateinit var bootstrapServers: String
    var reportingTopic: String? = null
    var notificationTopic: String? = null
    var auditTopic: String? = null
    var partition: Int? = null
    var partitioningStrategy: PartitioningStrategy = PartitioningStrategy.PER_MESSAGE_KEY
    var clientConfirmationTimeout: Long = 10L

    @PostConstruct
    fun validate() {
        if (partitioningStrategy === PartitioningStrategy.FIXED && partition == null) {
            throw IllegalArgumentException("Partition number must be defined in case of fixed partitioning strategy.")
        }

        if (partitioningStrategy === PartitioningStrategy.ROUND_ROBIN && partition != null) {
            throw IllegalArgumentException("Partition number must be null in case of Round-robin partitioning strategy.")
        }

        if (partitioningStrategy === PartitioningStrategy.PER_MESSAGE_KEY && partition != null) {
            throw IllegalArgumentException("Partition number must be null in case of Per message key partitioning strategy.")
        }
    }
}