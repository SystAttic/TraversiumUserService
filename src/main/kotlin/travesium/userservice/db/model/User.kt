package travesium.userservice.db.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import travesium.userservice.dto.UserDto

/**
 * @author Maja Razinger
 */
@Entity
@Table(name = User.TABLE_NAME)
data class User(
    @Id
    val uid: String = "",

    @Column(name = "username", unique = true)
    val username: String = "",

    @Column(name = "email", unique = true)
    val email: String = "",

    @Column(name = "display_name")
    val displayName: String = username,

    @Column(name = "photo_reference")
    val photoReference: String? = null,

    @Column(name = "followers")
    val followers: List<String>? = null,

    @Column(name = "blocked")
    val blocked: List<String>? = null,

    @Column(name = "deleted")
    val deleted: Boolean = false
) {
    companion object {
        const val TABLE_NAME = "user_table"
    }

    fun toDto() = UserDto(uid, username, email, displayName, photoReference, followers, blocked, deleted)
}