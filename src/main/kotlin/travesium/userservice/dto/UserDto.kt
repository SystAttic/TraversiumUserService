package travesium.userservice.dto

import java.time.OffsetDateTime

/**
 * @author Maja Razinger
 */
data class UserDto(
    val userId: Long? = null,
    val username: String? = null,
    val email: String? = null,
    val displayName: String? = username,
    val avatarPhotoReference: String? = null,
    val coverPhotoReference: String? = null,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val description: String? = null,
    val following: Set<UserDto> = emptySet(),
    val followers: Set<UserDto> = emptySet(),
    val blocked: Set<UserDto> = emptySet(),
    val deleted: Boolean = false
)