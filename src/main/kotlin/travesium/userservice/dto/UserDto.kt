package travesium.userservice.dto

import travesium.userservice.db.model.User

/**
 * @author Maja Razinger
 */
data class UserDto(
    val uid: String,
    val username: String,
    val email: String,
    val displayName: String = username,
    val photoReference: String? = null,
    val followers: List<String>? = null,
    val blocked: List<String>? = null,
    val deleted: Boolean = false
) {
    fun toUser() = User(
        uid = uid,
        username = username,
        email = email,
        displayName = displayName,
        photoReference = photoReference,
        followers = followers,
        blocked = blocked,
        deleted = deleted
    )
}