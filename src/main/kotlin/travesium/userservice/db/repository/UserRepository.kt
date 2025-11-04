package travesium.userservice.db.repository

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import travesium.userservice.db.model.User

import java.util.*

/**
 * @author Maja Razinger
 */

interface UserRepository : JpaRepository<User, String> {

    fun findByUsername(username : String) : Optional<User>

    fun findByEmail(email : String) : Optional<User>

    fun findByUserId(userId : Long) : Optional<User>

    fun findByFirebaseId(firebaseId: String) : Optional<User>

    @Query("SELECT u.followers FROM User u WHERE u.userId = :userId")
    fun findFollowers(@Param("userId") userId: Long, pageable: Pageable): List<User>

    @Query("SELECT u.following FROM User u WHERE u.userId = :userId")
    fun findFollowing(@Param("userId") userId: Long, pageable: Pageable): List<User>

    @Query("SELECT u.blocked FROM User u WHERE u.id = :userId")
    fun findBlocked(@Param("userId") userId: Long, pageable: Pageable): List<User>

    @Query("SELECT u FROM User u WHERE u.username IN :usernames")
    fun findByUsernames(@Param("usernames") usernames: List<String>, pageable: Pageable): List<User>
}
