package travesium.userservice.dto

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
    val description: String? = null,
    val following: Set<UserDto> = emptySet(),
    val followers: Set<UserDto> = emptySet(),
    val blocked: Set<UserDto> = emptySet(),
    val deleted: Boolean = false
)