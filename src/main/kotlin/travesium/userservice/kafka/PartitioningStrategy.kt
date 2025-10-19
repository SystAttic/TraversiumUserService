package travesium.userservice.kafka

/**
 * @author Maja Razinger
 */
enum class PartitioningStrategy {
    ROUND_ROBIN,
    FIXED,
    PER_MESSAGE_KEY
}