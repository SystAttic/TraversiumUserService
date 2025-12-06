package travesium.userservice

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.transaction.annotation.Transactional
import travesium.userservice.db.model.User
import travesium.userservice.db.repository.UserRepository
import travesium.userservice.exceptions.UserExceptions
import travesium.userservice.security.MockFirebaseConfig
import travesium.userservice.security.MockGrpcConfig
import travesium.userservice.security.TestMultitenancyConfig
import travesium.userservice.service.UserService

/**
 * @author Maja Razinger
 */
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@SpringBootTest(classes = [UserServiceApplication::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension::class)
@Transactional
@DirtiesContext
@ContextConfiguration(classes = [MockFirebaseConfig::class, MockGrpcConfig::class, TestMultitenancyConfig::class])
class UserFollowingAndBlockedTests@Autowired constructor(
    private val userRepository: UserRepository,
    @Autowired private val userService: UserService,
    @Autowired private val firebaseConfig: MockFirebaseConfig
) {

    private lateinit var dejan: User
    private lateinit var jure: User
    private lateinit var ozbej: User
    private lateinit var maja: User

    @BeforeEach
    fun setup() {
        userRepository.deleteAll()

        firebaseConfig.setTokenData("token1", "dejanUID", "dejanjarc@gmail.com")
        firebaseConfig.setTokenData("token2", "jureUID", "jurezupancic@gmail.com")
        firebaseConfig.setTokenData("token3", "ozbejUID", "ozbejpavc@gmail.com")
        firebaseConfig.setTokenData("token4", "majaUID", "majarazinger@gmail.com")

        dejan = userRepository.save(User(username = "dejan", email = "dejanjarc@gmail.com", firebaseId = "dejanUID"))
        jure = userRepository.save(User(username = "jure", email = "jurezupancic@gmail.com", firebaseId = "jureUID"))
        ozbej = userRepository.save(User(username = "ozbej", email = "ozbejpavc@gmail.com", firebaseId = "ozbejUID"))
        maja = userRepository.save(User(username = "maja", email = "majarazinger@gmail.com", firebaseId = "majaUID"))
    }

    @Test
    fun testFollowing() {
        val auth = UsernamePasswordAuthenticationToken("principal", "token1")
        SecurityContextHolder.getContext().authentication = auth

        userService.followUser( "jure")
        userService.followUser("ozbej")

        val dejanFollowers = userService.getFollowers("dejan", 0, 10)
        val dejanFollowing = userService.getFollowing("dejan", 0, 10)

        assert(dejanFollowers.isEmpty())
        assert(dejanFollowing.size == 2)

        assert(dejanFollowing.any { it.username == "jure" })
        assert(dejanFollowing.any { it.username == "ozbej" })

        val jureFollowers = userService.getFollowers("jure", 0, 10)
        assert(jureFollowers.size == 1)
        assert(jureFollowers.any { it.username == "dejan" })
    }

    @Test
    fun testBlocking() {
        val auth = UsernamePasswordAuthenticationToken("principal", "token4")
        SecurityContextHolder.getContext().authentication = auth

        userService.blockUser("ozbej")
        userService.blockUser("jure")

        val majaBlocked = userService.getBlockedUsers(0, 10)
        assert(majaBlocked.size == 2)
        assert(majaBlocked.any { it.username == "ozbej" })
        assert(majaBlocked.any { it.username == "jure" })
    }

    @Test
    fun testFollowBlockedUser() {
        val auth = UsernamePasswordAuthenticationToken("principal", "token4")
        SecurityContextHolder.getContext().authentication = auth

        userService.blockUser("ozbej")
        assertThrows<UserExceptions.InvalidUserDataException> {
            userService.followUser( "maja")
        }
    }

    @Test
    fun testUnfollowUser() {
        val auth = UsernamePasswordAuthenticationToken("principal", "token1")
        SecurityContextHolder.getContext().authentication = auth

        userService.followUser("jure")
        var dejanFollowing = userService.getFollowing("dejan", 0, 10)
        assert(dejanFollowing.size == 1)

        userService.unfollowUser( "jure")
        dejanFollowing = userService.getFollowing("dejan", 0, 10)
        assert(dejanFollowing.isEmpty())
    }

    @Test
    fun testUnblockUser() {
        val auth = UsernamePasswordAuthenticationToken("principal", "token4")
        SecurityContextHolder.getContext().authentication = auth

        userService.blockUser( "ozbej")
        var majaBlocked = userService.getBlockedUsers(0, 10)
        assert(majaBlocked.size == 1)

        userService.unblockUser( "ozbej")
        majaBlocked = userService.getBlockedUsers(0, 10)
        assert(majaBlocked.isEmpty())
    }

    @Test
    fun followThenBlock() {
        val auth = UsernamePasswordAuthenticationToken("principal", "token1")
        SecurityContextHolder.getContext().authentication = auth

        userService.followUser( "jure")
        var dejanFollowing = userService.getFollowing("dejan", 0, 10)
        assert(dejanFollowing.size == 1)

        userService.blockUser( "jure")
        dejanFollowing = userService.getFollowing("dejan", 0, 10)
        assert(dejanFollowing.isEmpty())

        val jureFollowers = userService.getFollowers("jure", 0, 10)
        assert(jureFollowers.isEmpty())
    }

    @Test
    fun selfBlock() {
        val auth = UsernamePasswordAuthenticationToken("principal", "token4")
        SecurityContextHolder.getContext().authentication = auth

        assertThrows<UserExceptions.InvalidUserDataException> {
            userService.blockUser( "maja")
        }
    }

    @Test
    fun selfFollow() {
        val auth = UsernamePasswordAuthenticationToken("principal", "token2")
        SecurityContextHolder.getContext().authentication = auth

        assertThrows<UserExceptions.InvalidUserDataException> {
            userService.followUser( "jure")
        }
    }

    @Test
    fun getFollowingPagination() {
        val auth = UsernamePasswordAuthenticationToken("principal", "token1")
        SecurityContextHolder.getContext().authentication = auth

        userService.followUser( "jure")
        userService.followUser( "ozbej")
        userService.followUser( "maja")

        val dejanFollowingPage1 = userService.getFollowing("dejan", 0, 2)
        assert(dejanFollowingPage1.size == 2)
        val dejanFollowingPage2 = userService.getFollowing("dejan", 2, 2)
        assert(dejanFollowingPage2.size == 1)

        assert(dejanFollowingPage1.all { it.username in listOf("jure", "ozbej") })
        assert(dejanFollowingPage2.all { it.username == "maja" })
    }

    @Test
    fun getBlockedUsersPagination() {
        val auth = UsernamePasswordAuthenticationToken("principal", "token4")
        SecurityContextHolder.getContext().authentication = auth

        userService.blockUser( "jure")
        userService.blockUser( "ozbej")
        userService.blockUser( "dejan")

        val majaBlockedPage1 = userService.getBlockedUsers(0, 2)
        assert(majaBlockedPage1.size == 2)
        val majaBlockedPage2 = userService.getBlockedUsers(2, 2)
        assert(majaBlockedPage2.size == 1)

        assert(majaBlockedPage1.all { it.username in listOf("jure", "ozbej") })
        assert(majaBlockedPage2.all { it.username == "dejan" })
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