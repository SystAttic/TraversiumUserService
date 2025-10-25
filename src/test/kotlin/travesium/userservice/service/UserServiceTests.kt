package travesium.userservice.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.context.ApplicationEventPublisher
import travesium.userservice.db.model.User
import travesium.userservice.db.repository.UserRepository
import travesium.userservice.dto.UserDto
import travesium.userservice.exceptions.UserExceptions
import travesium.userservice.mapper.UserMapper
import java.util.*

/**
 * @author Maja Razinger
 */
class UserServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var userService: UserService
    private lateinit var eventPublisher: ApplicationEventPublisher

    @BeforeEach
    fun setUp() {
        userRepository = mock()
        eventPublisher = mock()
        userService = UserService(userRepository, eventPublisher)
    }

    @Test
    fun `createUser success`() {
        val userDto = UserDto(username = "test", email = "test@example.com", firebaseId = "firebase123")
        whenever(userRepository.save(UserMapper.toEntity(userDto))).thenAnswer { it.arguments[0] }

        val result = userService.createUser(userDto)

        assertEquals(userDto.userId, result.userId)
        verify(userRepository).save(UserMapper.toEntity(userDto))
    }

    @Test
    fun `createUser username exists`() {
        val userDto = UserDto(username = "test", email = "test@example.com", firebaseId = "firebase123")
        whenever(userRepository.findByUserId(userDto.userId!!)).thenReturn(Optional.empty())
        whenever(userRepository.findByUsername(userDto.username!!)).thenReturn(Optional.of(mock()))

        assertThrows(UserExceptions.UserAlreadyExistsException::class.java) {
            userService.createUser(userDto)
        }
    }

    @Test
    fun `createUser email exists`() {
        val userDto = UserDto(username = "test", email = "test@example.com")
        whenever(userRepository.findByUsername(userDto.username!!)).thenReturn(Optional.empty())
        whenever(userRepository.findByEmail(userDto.email!!)).thenReturn(Optional.of(mock()))

        assertThrows(UserExceptions.UserAlreadyExistsException::class.java) {
            userService.createUser(userDto)
        }
    }


    @Test
    fun `getUserByUsername success`() {
        val user = User(username = "test", email = "test@example.com", firebaseId = "firebase123")
        whenever(userRepository.findByUsername("test")).thenReturn(Optional.of(user))

        val result = userService.getUserByUsername("test")

        assertEquals(user.username, result.username)
    }

    @Test
    fun `getUserByUsername not found`() {
        whenever(userRepository.findByUsername("test")).thenReturn(Optional.empty())

        assertThrows(UserExceptions.UserNotFoundException::class.java) {
            userService.getUserByUsername("test")
        }
    }

    @Test
    fun `getUserByEmail success`() {
        val user = User(username = "test", email = "test@example.com", firebaseId = "firebase123")
        whenever(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user))

        val result = userService.getUserByEmail("test@example.com")

        assertEquals(user.email, result.email)
    }

    @Test
    fun `getUserByEmail not found`() {
        whenever(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty())

        assertThrows(UserExceptions.UserNotFoundException::class.java) {
            userService.getUserByEmail("test@example.com")
        }
    }

    @Test
    fun `deleteUserByUsername success`() {
        val user = User(username = "test", email = "test@example.com", firebaseId = "firebase123")
        whenever(userRepository.findByUsername("test")).thenReturn(Optional.of(user))
        userService.deleteUser("test")
        verify(userRepository).delete(user)
    }

    @Test
    fun `deleteUserByUsername not found`() {
        whenever(userRepository.findByUsername("test")).thenReturn(Optional.empty())
        assertThrows(UserExceptions.UserNotFoundException::class.java) {
            userService.deleteUser("test")
        }
    }

    @Test
    fun `deleteUserByEmail success`() {
        val user = User(username = "test", email = "test@example.com", firebaseId = "firebase123")
        whenever(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user))
        userService.deleteUserByEmail("test@example.com")
        verify(userRepository).delete(user)
    }

    @Test
    fun `deleteUserByEmail not found`() {
        whenever(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty())
        assertThrows(UserExceptions.UserNotFoundException::class.java) {
            userService.deleteUserByEmail("test@example.com")
        }
    }

    @Test
    fun `updateUser success`() {
        val existingUser = User(userId = 123, email = "ex@123.com", description = "old")
        val updatedDto = UserDto(userId = 123, email = "ex@123.com", description = "new")
        whenever(userRepository.findByUserId(123)).thenReturn(Optional.of(existingUser))
        whenever(userRepository.save(existingUser)).thenAnswer { it.arguments[0] }
        val result = userService.updateUser(updatedDto)
        assertEquals("new", result.description)
    }

    @Test
    fun `updateUser not found`() {
        val updatedDto = UserDto(username = "new", email = "test@example.com", firebaseId = "firebase123")
        whenever(userRepository.findByUserId(123)).thenReturn(Optional.empty())
        assertThrows(UserExceptions.UserNotFoundException::class.java) {
            userService.updateUser(updatedDto)
        }
    }
}
