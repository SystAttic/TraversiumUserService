package travesium.userservice.dto

import travesium.userservice.db.model.User

/**
 * @author Maja Razinger
 */
data class UserDto(
    val userId: Long? = null,
    val username: String,
    val email: String,
    val displayName: String = username,
    val photoReference: String? = null,
    val following: Set<UserDto> = emptySet(),
    val followers: Set<UserDto> = emptySet(),
    val blocked: Set<UserDto> = emptySet(),
    val deleted: Boolean = false
)