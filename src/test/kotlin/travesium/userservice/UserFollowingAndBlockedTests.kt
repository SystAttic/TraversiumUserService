package travesium.userservice

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.transaction.annotation.Transactional
import travesium.userservice.db.model.User
import travesium.userservice.db.repository.UserRepository
import travesium.userservice.exceptions.UserExceptions
import travesium.userservice.service.UserService
import java.time.OffsetDateTime

/**
 * @author Maja Razinger
 */
@AutoConfigureTestDatabase
@ActiveProfiles("test")
@SpringBootTest(classes = [UserServiceApplication::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension::class)
@Transactional
@DirtiesContext
class UserFollowingAndBlockedTests@Autowired constructor(
    private val userRepository: UserRepository,
    @Autowired private val userService: UserService,
    service: UserService
) {

    private lateinit var dejan: User
    private lateinit var jure: User
    private lateinit var ozbej: User
    private lateinit var maja: User

    @BeforeEach
    fun setup() {
        userRepository.deleteAll()

        dejan = userRepository.save(User(username = "dejan", email = "dejanjarc@gmail.com"))
        jure = userRepository.save(User(username = "jure", email = "jurezupancic@gmail.com"))
        ozbej = userRepository.save(User(username = "ozbej", email = "ozbejpavc@gmail.com"))
        maja = userRepository.save(User(username = "maja", email = "majarazinger@gmail.com"))
    }

    @Test
    fun testFollowing() {
        userService.followUser("dejan", "jure")
        userService.followUser("dejan", "ozbej")

        val dejanFollowers = userService.getFollowers("dejan")
        val dejanFollowing = userService.getFollowing("dejan")

        assert(dejanFollowers.isEmpty())
        assert(dejanFollowing.size == 2)

        assert(dejanFollowing.any { it.username == "jure" })
        assert(dejanFollowing.any { it.username == "ozbej" })

        val jureFollowers = userService.getFollowers("jure")
        assert(jureFollowers.size == 1)
        assert(jureFollowers.any { it.username == "dejan" })
    }

    @Test
    fun testBlocking() {
        userService.blockUser("maja", "ozbej")
        userService.blockUser("maja", "jure")

        val majaBlocked = userService.getBlockedUsers("maja")
        assert(majaBlocked.size == 2)
        assert(majaBlocked.any { it.username == "ozbej" })
        assert(majaBlocked.any { it.username == "jure" })
    }

    @Test
    fun testFollowBlockedUser() {
        userService.blockUser("maja", "ozbej")
        assertThrows<UserExceptions.InvalidUserDataException> {
            userService.followUser("ozbej", "maja")
        }
    }

    @Test
    fun testUnfollowUser() {
        userService.followUser("dejan", "jure")
        var dejanFollowing = userService.getFollowing("dejan")
        assert(dejanFollowing.size == 1)

        userService.unfollowUser("dejan", "jure")
        dejanFollowing = userService.getFollowing("dejan")
        assert(dejanFollowing.isEmpty())
    }

    @Test
    fun testUnblockUser() {
        userService.blockUser("maja", "ozbej")
        var majaBlocked = userService.getBlockedUsers("maja")
        assert(majaBlocked.size == 1)

        userService.unblockUser("maja", "ozbej")
        majaBlocked = userService.getBlockedUsers("maja")
        assert(majaBlocked.isEmpty())
    }

    @Test
    fun followThenBlock() {
        userService.followUser("dejan", "jure")
        var dejanFollowing = userService.getFollowing("dejan")
        assert(dejanFollowing.size == 1)

        userService.blockUser("dejan", "jure")
        dejanFollowing = userService.getFollowing("dejan")
        assert(dejanFollowing.isEmpty())

        val jureFollowers = userService.getFollowers("jure")
        assert(jureFollowers.isEmpty())
    }

    @Test
    fun selfBlock() {
        assertThrows<UserExceptions.InvalidUserDataException> {
            userService.blockUser("maja", "maja")
        }
    }

    @Test
    fun selfFollow() {
        assertThrows<UserExceptions.InvalidUserDataException> {
            userService.followUser("jure", "jure")
        }
    }

//    @Test
//    fun testFindFollowingOn5kUsers() {
//         (1 .. 5000).map {
//            userRepository.save(User(username = "user$it", email = "email$it"))
//            userService.followUser("user$it", "jure")
//        }
//
//        print("All users created and following jure.")
//        println(OffsetDateTime.now().toString())
//        val jureFollowers = userService.getFollowers("jure")
//        println(OffsetDateTime.now().toString())
//        assert(jureFollowers.size == 5000)
//
//        println(OffsetDateTime.now().toString())
//        val user2500Following = userService.getFollowing("user2500")
//        println(OffsetDateTime.now().toString())
//        assert(user2500Following.size == 1)
//        assert(user2500Following[0].username == "jure")
//    }
}