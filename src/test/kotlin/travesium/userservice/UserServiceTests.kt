package travesium.userservice

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import travesium.userservice.db.model.User
import travesium.userservice.db.repository.UserRepository
import travesium.userservice.dto.UserDto
import travesium.userservice.exceptions.UserExceptions
import travesium.userservice.security.BaseSecuritySetup
import travesium.userservice.security.MockFirebaseConfig
import travesium.userservice.service.UserService

/**
 * User Service Tests with H2 Database (no repository mocking)
 * @author Maja Razinger
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(MockFirebaseConfig::class)
class UserServiceTests : BaseSecuritySetup() {

    @TestConfiguration
    class TestConfig {
        @Bean
        @Primary
        fun mockEventPublisher(): ApplicationEventPublisher = mock(ApplicationEventPublisher::class.java)
    }

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var eventPublisher: ApplicationEventPublisher

    @Autowired
    private lateinit var userService: UserService

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.clearContext()
        setupDefaultAuth()
        userRepository.deleteAll()
    }

    @AfterEach
    fun tearDown() {
        userRepository.deleteAll()
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `createUser success`() {
        val userDto = UserDto(username = "test", email = email, firebaseId = firebaseId)

        val result = userService.createUser(userDto)

        assertEquals(userDto.username, result.username)
        assertEquals(userDto.email, result.email)
        assertEquals(firebaseId, result.firebaseId)

        val savedUser = userRepository.findByUsername("test")
        assertEquals(true, savedUser.isPresent)
        assertEquals("test", savedUser.get().username)
    }

    @Test
    fun `createUser username exists`() {
        userRepository.save(User(username = "test", email = "other@example.com", firebaseId = "other123"))

        val userDto = UserDto(username = "test", email = email, firebaseId = firebaseId)

        assertThrows(UserExceptions.UserAlreadyExistsException::class.java) {
            userService.createUser(userDto)
        }
    }

    @Test
    fun `createUser email exists`() {
        userRepository.save(User(username = "other", email = email, firebaseId = "other123"))

        val userDto = UserDto(username = "test", email = email, firebaseId = firebaseId)

        assertThrows(UserExceptions.UserAlreadyExistsException::class.java) {
            userService.createUser(userDto)
        }
    }

    @Test
    fun `getUserByUsername success`() {
        userRepository.save(User(username = "test", email = "test@example.com", firebaseId = "firebase123"))

        val result = userService.getUser("test", null)

        assertEquals("test", result.username)
        assertEquals("test@example.com", result.email)
    }

    @Test
    fun `getUserByUsername not found`() {
        assertThrows(UserExceptions.UserNotFoundException::class.java) {
            userService.getUser("test", null)
        }
    }

    @Test
    fun `getUserByEmail success`() {
        userRepository.save(User(username = "test", email = "test@example.com", firebaseId = "firebase123"))

        val result = userService.getUser(null, "test@example.com")

        assertEquals("test", result.username)
        assertEquals("test@example.com", result.email)
    }

    @Test
    fun `getUserByEmail not found`() {
        assertThrows(UserExceptions.UserNotFoundException::class.java) {
            userService.getUser(null, "test@example.com")
        }
    }

    @Test
    fun `deleteUser success`() {
        userRepository.save(User(username = "test", email = "test@example.com", firebaseId = firebaseId))

        userService.deleteUser()

        val deletedUser = userRepository.findByFirebaseId(firebaseId)
        assertEquals(true, deletedUser.isPresent)
        assertEquals(true, deletedUser.get().deleted)
    }

    @Test
    fun `deleteUser not found`() {
        assertThrows(UserExceptions.UserNotFoundException::class.java) {
            userService.deleteUser()
        }
    }

    @Test
    fun `updateUser success`() {
        val savedUser = userRepository.save(
            User(
                email = email,
                description = "old",
                firebaseId = firebaseId,
                username = "testuser"
            )
        )

        val updatedDto = UserDto(
            userId = savedUser.userId,
            email = email,
            description = "new",
            firebaseId = firebaseId,
            username = "testuser"
        )

        val result = userService.updateUser(updatedDto)

        assertEquals("new", result.description)

        val dbUser = userRepository.findByUserId(savedUser.userId!!)
        assertEquals(true, dbUser.isPresent)
        assertEquals("new", dbUser.get().description)
    }

    @Test
    fun `updateUser not found`() {
        val updatedDto = UserDto(
            userId = 999,
            username = "new",
            email = "test@example.com",
            firebaseId = "firebase123"
        )

        assertThrows(UserExceptions.UserNotFoundException::class.java) {
            userService.updateUser(updatedDto)
        }
    }

    @Test
    fun `username exists`() {
        userRepository.save(
            User(
                email = email,
                description = "old",
                firebaseId = firebaseId,
                username = "testuser"
            )
        )

        val result = userService.checkIfUserExists("testuser", null)
        assertEquals(true, result)
    }

    @Test
    fun `email exists`() {
        userRepository.save(
            User(
                email = email,
                description = "old",
                firebaseId = firebaseId,
                username = "testuser"
            )
        )

        val result = userService.checkIfUserExists(null, email)
        assertEquals(true, result)
    }

    @Test
    fun `searchUsersByUsername returns matching users`() {
        userRepository.save(User(username = "testuser1", email = "test1@example.com", firebaseId = "firebase1"))
        userRepository.save(User(username = "testuser2", email = "test2@example.com", firebaseId = "firebase2"))

        val result = userService.searchUsersByUsername("test", 0, 10)

        assertEquals(2, result.size)
        assertEquals(true, result.any { it.username == "testuser1" })
        assertEquals(true, result.any { it.username == "testuser2" })
    }

    @Test
    fun `searchUsersByUsername returns empty list when no matches found`() {
        userRepository.save(User(username = "alice", email = "alice@example.com", firebaseId = "firebase1"))
        userRepository.save(User(username = "bob", email = "bob@example.com", firebaseId = "firebase2"))

        val result = userService.searchUsersByUsername("nonexistent", 0, 10)

        assertEquals(0, result.size)
    }

    @Test
    fun `searchUsersByUsername returns empty list for blank query`() {
        val result = userService.searchUsersByUsername("", 0, 10)

        assertEquals(0, result.size)
    }

    @Test
    fun `searchUsersByUsername returns empty list for whitespace query`() {
        val result = userService.searchUsersByUsername("   ", 0, 10)

        assertEquals(0, result.size)
    }

    @Test
    fun `searchUsersByUsername handles pagination correctly`() {
        userRepository.save(User(username = "testuser1", email = "test1@example.com", firebaseId = "firebase1"))
        userRepository.save(User(username = "testuser2", email = "test2@example.com", firebaseId = "firebase2"))
        userRepository.save(User(username = "testuser3", email = "test3@example.com", firebaseId = "firebase3"))

        val result = userService.searchUsersByUsername("test", 0, 2)

        assertEquals(2, result.size)
    }

    @Test
    fun `searchUsersByUsername performs partial matching`() {
        userRepository.save(User(username = "johndoe", email = "john@example.com", firebaseId = "firebase1"))
        userRepository.save(User(username = "janedoe", email = "jane@example.com", firebaseId = "firebase2"))

        val result = userService.searchUsersByUsername("doe", 0, 10)

        assertEquals(2, result.size)
        assertEquals(true, result.any { it.username == "johndoe" })
        assertEquals(true, result.any { it.username == "janedoe" })
    }

    @Test
    fun `searchUsersByUsername handles single character query`() {
        userRepository.save(User(username = "alice", email = "alice@example.com", firebaseId = "firebase1"))

        val result = userService.searchUsersByUsername("a", 0, 10)

        assertEquals(1, result.size)
        assertEquals("alice", result[0].username)
    }

    @Test
    fun `searchUsersByUsername excludes deleted users`() {
        userRepository.save(User(username = "activeuser", email = "active@example.com", firebaseId = "firebase1", deleted = false))
        userRepository.save(User(username = "deleteduser", email = "deleted@example.com", firebaseId = "firebase2", deleted = true))

        val result = userService.searchUsersByUsername("user", 0, 10)

        assertEquals(1, result.size)
        assertEquals("activeuser", result[0].username)
    }
}
