package travesium.userservice

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.springframework.context.ApplicationEventPublisher
import travesium.userservice.db.model.User
import travesium.userservice.db.repository.UserRepository
import travesium.userservice.dto.UserDto
import travesium.userservice.exceptions.UserExceptions
import travesium.userservice.security.BaseSecuritySetup
import travesium.userservice.service.FirebaseService
import travesium.userservice.service.UserService
import java.util.*

/**
 * @author Maja Razinger
 */
@ExtendWith(MockitoExtension::class)
class UserServiceTest : BaseSecuritySetup() {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var eventPublisher: ApplicationEventPublisher

    @Mock
    private lateinit var firebaseService: FirebaseService

    @InjectMocks
    private lateinit var userService: UserService

    @BeforeEach
    fun setUp() {
        setupDefaultFirebaseMocks()
    }

    @Test
    fun `createUser success`() {
        val userDto = UserDto(username = "test", email = email, firebaseId = firebaseId)

        `when`(userRepository.findByUsername(userDto.username!!)).thenReturn(Optional.empty())
        `when`(userRepository.findByEmail(userDto.email!!)).thenReturn(Optional.empty())
        `when`(userRepository.save(any<User>())).thenAnswer {
            val user = it.arguments[0] as User
            user.copy(userId = 1L)
        }

        val result = userService.createUser(userDto)

        assertEquals(userDto.username, result.username)
        assertEquals(userDto.email, result.email)
        assertEquals(firebaseId, result.firebaseId)

        verify(userRepository).save(any())
    }

    @Test
    fun `createUser username exists`() {
        val userDto = UserDto(username = "test", email = email, firebaseId = firebaseId)

        `when`(userRepository.findByUsername(userDto.username!!)).thenReturn(Optional.of(mock()))

        assertThrows(UserExceptions.UserAlreadyExistsException::class.java) {
            userService.createUser(userDto)
        }
    }

    @Test
    fun `createUser email exists`() {
        val userDto = UserDto(username = "test", email = email, firebaseId = firebaseId)

        `when`(firebaseService.extractUidFromToken(token)).thenReturn(firebaseId)
        `when`(firebaseService.extractEmailFromToken(token)).thenReturn(email)
        `when`(userRepository.findByEmail(userDto.email!!)).thenReturn(Optional.of(mock()))

        assertThrows(UserExceptions.UserAlreadyExistsException::class.java) {
            userService.createUser(userDto)
        }
    }


    @Test
    fun `getUserByUsername success`() {
        val user = User(username = "test", email = "test@example.com", firebaseId = "firebase123")
        `when`(userRepository.findByUsername("test")).thenReturn(Optional.of(user))

        val result = userService.getUser("test", null)

        assertEquals(user.username, result.username)
    }

    @Test
    fun `getUserByUsername not found`() {
        `when`(userRepository.findByUsername("test")).thenReturn(Optional.empty())

        assertThrows(UserExceptions.UserNotFoundException::class.java) {
            userService.getUser("test", null)
        }
    }

    @Test
    fun `getUserByEmail success`() {
        val user = User(username = "test", email = "test@example.com", firebaseId = "firebase123")
        `when`(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user))

        val result = userService.getUser(null, "test@example.com")

        assertEquals(user.email, result.email)
    }

    @Test
    fun `getUserByEmail not found`() {
        `when`(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty())

        assertThrows(UserExceptions.UserNotFoundException::class.java) {
            userService.getUser(null, "test@example.com")
        }
    }

    @Test
    fun `deleteUser success`() {
        val user = User(userId = 1L, username = "test", email = "test@example.com", firebaseId = firebaseId)

        `when`(userRepository.findByFirebaseId(firebaseId)).thenReturn(Optional.of(user))

        userService.deleteUser()
        verify(userRepository).save(user.copy(deleted = true))
    }

    @Test
    fun `deleteUser not found`() {
        `when`(userRepository.findByFirebaseId(firebaseId)).thenReturn(Optional.empty())

        assertThrows(UserExceptions.UserNotFoundException::class.java) {
            userService.deleteUser()
        }

        verify(userRepository, never()).delete(any())
    }

    @Test
    fun `updateUser success`() {
        val existingUser = User(userId = 123, email = email, description = "old", firebaseId = firebaseId, username = "testuser")
        val updatedDto = UserDto(userId = 123, email = email, description = "new", firebaseId = firebaseId, username = "testuser")

        `when`(userRepository.findByUserId(123)).thenReturn(Optional.of(existingUser))
        `when`(userRepository.save(any())).thenAnswer { it.arguments[0] }

        val result = userService.updateUser(updatedDto)

        assertEquals("new", result.description)
    }

    @Test
    fun `updateUser not found`() {
        val updatedDto = UserDto(userId = 123, username = "new", email = "test@example.com", firebaseId = "firebase123")
        `when`(userRepository.findByUserId(123)).thenReturn(Optional.empty())
        assertThrows(UserExceptions.UserNotFoundException::class.java) {
            userService.updateUser(updatedDto)
        }
    }

    @Test
    fun `username exists`() {
        val existingUser = User(userId = 123, email = email, description = "old", firebaseId = firebaseId, username = "testuser")
        `when`(userRepository.findByUsername("testuser")).thenReturn(Optional.of(existingUser))

        val result = userService.checkIfUserExists("testuser", null)
        assertEquals(true, result)
    }

    @Test
    fun `email exists`() {
        val existingUser = User(userId = 123, email = email, description = "old", firebaseId = firebaseId, username = "testuser")
        `when`(userRepository.findByEmail(email)).thenReturn(Optional.of(existingUser))

        val result = userService.checkIfUserExists(null, email)
        assertEquals(true, result)
    }

    private fun setupDefaultFirebaseMocks() {
        lenient().`when`(firebaseService.extractUidFromToken(token)).thenReturn(firebaseId)
        lenient().`when`(firebaseService.extractEmailFromToken(token)).thenReturn(email)
    }
}
