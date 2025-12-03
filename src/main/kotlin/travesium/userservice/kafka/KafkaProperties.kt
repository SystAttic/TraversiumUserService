package travesium.userservice.kafka

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding

/**
 * @author Maja Razinger
 */
@ConfigurationProperties(prefix = "spring.kafka")
class KafkaProperties @ConstructorBinding constructor(
    val bootstrapServers: String,
    val reportingTopic: String?,
    val notificationTopic: String?,
    val auditTopic: String?,
    partition: Int? = null,
    partitioningStrategy: PartitioningStrategy = PartitioningStrategy.PER_MESSAGE_KEY,
    val clientConfirmationTimeout: Long = 10L
) {

    init {
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