package travesium.userservice.db.model

import jakarta.persistence.*

/**
 * @author Maja Razinger
 */
@Entity
@Table(name = User.TABLE_NAME)
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id", unique = true, nullable = false, updatable = false, length = 36)
    val userId: Long? = null,

    @Column(name = "username", unique = true)
    val username: String? = null,

    @Column(name = "email", unique = true)
    val email: String? = null,

    @Column(name = "description")
    val description: String? = null,

    @Column(name = "display_name")
    val displayName: String? = username,

    @Column(name = "avatar_photo_reference")
    val avatarPhotoReference: String? = null,

    @Column(name = "cover_photo_reference")
    val coverPhotoReference: String? = null,

    @ManyToMany
    @JoinTable(
        name = "user_followers",
        joinColumns = [JoinColumn(name = "follower_id")],
        inverseJoinColumns = [JoinColumn(name = "followed_id")]
    )
    val following: MutableSet<User> = mutableSetOf(),

    @ManyToMany(mappedBy = "following")
    val followers: MutableSet<User> = mutableSetOf(),

    @ManyToMany
    @JoinTable(
        name = "blocked",
        joinColumns = [JoinColumn(name = "user_id")],
        inverseJoinColumns = [JoinColumn(name = "blocked_user__id")]
    )
    val blocked: MutableSet<User> = mutableSetOf(),

    @Column(name = "deleted")
    val deleted: Boolean = false
) {
    companion object {
        const val TABLE_NAME = "user_table"
    }
}