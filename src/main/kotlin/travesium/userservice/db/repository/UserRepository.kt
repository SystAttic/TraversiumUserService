package travesium.userservice.db.repository

import org.springframework.data.jpa.repository.JpaRepository
import travesium.userservice.db.model.User
import java.util.*

/**
 * @author Maja Razinger
 */

interface UserRepository : JpaRepository<User, String> {

    fun findByUsername(username : String) : Optional<User>

    fun findByEmail(email : String) : Optional<User>

    fun findByUid(uid : String) : Optional<User>
}
