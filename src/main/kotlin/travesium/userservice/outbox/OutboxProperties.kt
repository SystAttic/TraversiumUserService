package travesium.userservice.outbox

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * @author Maja Razinger
 */
@Configuration
@ConfigurationProperties(prefix = "outbox")
data class OutboxProperties(
    var processor: ProcessorConfig = ProcessorConfig(),
    var cleanup: CleanupConfig = CleanupConfig()
) {
    data class ProcessorConfig(
        var interval: Long = 5000
    )

    data class CleanupConfig(
        var cron: String = "0 0 * * * *",
        var retention: RetentionConfig = RetentionConfig()
    )

    data class RetentionConfig(
        var hours: Long = 168
    )
}