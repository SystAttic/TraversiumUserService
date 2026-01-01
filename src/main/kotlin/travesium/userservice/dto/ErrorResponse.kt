package travesium.userservice.dto

import java.time.OffsetDateTime

/**
 * @author Maja Razinger
 */
data class ErrorResponse(
    val message: String,
    val status: Int,
    val errorCode: String? = null,
    val timestamp: OffsetDateTime = OffsetDateTime.now(),
    val path: String? = null
)
