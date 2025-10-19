package travesium.userservice.kafka.data

import java.time.YearMonth

/**
 * @author Maja Razinger
 */
data class ReportingStreamData(
    val timestamp: YearMonth,
    val action: UserEvent
)
