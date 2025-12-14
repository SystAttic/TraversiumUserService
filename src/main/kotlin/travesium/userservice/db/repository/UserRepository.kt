package travesium.userservice.db.repository

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import travesium.userservice.db.model.User
import java.util.*

/**
 * @author Maja Razinger
 */

interface UserRepository : JpaRepository<User, String> {

    fun findByUsername(@Param("username") username : String) : Optional<User>

    fun findByEmail(email: String): Optional<User>

    fun findByUserId(userId : Long) : Optional<User>

    fun findByFirebaseId(firebaseId: String) : Optional<User>

    @Query("SELECT u.followers FROM User u WHERE u.userId = :userId")
    fun findFollowers(@Param("userId") userId: Long, pageable: Pageable): List<User>

    @Query("SELECT u.following FROM User u WHERE u.userId = :userId")
    fun findFollowing(@Param("userId") userId: Long, pageable: Pageable): List<User>

    @Query("SELECT u.blocked FROM User u WHERE u.userId = :userId")
    fun findBlocked(@Param("userId") userId: Long, pageable: Pageable): List<User>

    @Query("SELECT u FROM User u WHERE u.username IN :usernames")
    fun findByUsernames(@Param("usernames") usernames: List<String>, pageable: Pageable): List<User>

    @Query(
        value = "SELECT EXISTS(SELECT 1 FROM user_followers WHERE follower_id = :followerId AND followed_id = :followedId)",
        nativeQuery = true
    )
    fun checkIfUserAIsFollowingUserB(@Param("followerId") followerId: Long, @Param("followedId") followedId: Long): Boolean

    @Modifying
    @Transactional
    @Query(
        value = """
        DELETE FROM user_followers 
        WHERE follower_id = :followerId 
          AND followed_id = :followedId
    """,
        nativeQuery = true
    )
    fun removeFollowerByUserId(
        @Param("followerId") followerId: Long,
        @Param("followedId") followedId: Long
    )



    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) AND u.deleted = false")
    fun searchUsersByUsername(@Param("query") query: String, pageable: Pageable): List<User>
}
