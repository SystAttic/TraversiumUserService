package travesium.userservice

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.context.ApplicationEventPublisher
import org.springframework.kafka.core.KafkaTemplate
import travesium.userservice.db.model.User
import travesium.userservice.db.repository.UserRepository
import travesium.userservice.dto.UserDto
import travesium.userservice.exceptions.UserExceptions
import travesium.userservice.service.UserService
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
        val userDto = UserDto(uid = "123", username = "test", email = "test@example.com")
        whenever(userRepository.save(userDto.toUser())).thenAnswer { it.arguments[0] }

        val result = userService.createUser(userDto)

        assertEquals(userDto.uid, result.uid)
        verify(userRepository).save(userDto.toUser())
    }

    @Test
    fun `createUser username exists`() {
        val userDto = UserDto(uid = "123", username = "test", email = "test@example.com")
        whenever(userRepository.findByUid(userDto.uid)).thenReturn(Optional.empty())
        whenever(userRepository.findByUsername(userDto.username)).thenReturn(Optional.of(mock()))

        assertThrows(UserExceptions.UserAlreadyExistsException::class.java) {
            userService.createUser(userDto)
        }
    }

    @Test
    fun `createUser email exists`() {
        val userDto = UserDto(uid = "123", username = "test", email = "test@example.com")
        whenever(userRepository.findByUid(userDto.uid)).thenReturn(Optional.empty())
        whenever(userRepository.findByUsername(userDto.username)).thenReturn(Optional.empty())
        whenever(userRepository.findByEmail(userDto.email)).thenReturn(Optional.of(mock()))

        assertThrows(UserExceptions.UserAlreadyExistsException::class.java) {
            userService.createUser(userDto)
        }
    }


    @Test
    fun `getUserByUsername success`() {
        val user = User(uid = "123", username = "test", email = "test@example.com")
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
        val user = User(uid = "123", username = "test", email = "test@example.com")
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
        val user = User(uid = "123", username = "test", email = "test@example.com")
        whenever(userRepository.findByUsername("test")).thenReturn(Optional.of(user))
        userService.deleteUserByUsername("test")
        verify(userRepository).delete(user)
    }

    @Test
    fun `deleteUserByUsername not found`() {
        whenever(userRepository.findByUsername("test")).thenReturn(Optional.empty())
        assertThrows(UserExceptions.UserNotFoundException::class.java) {
            userService.deleteUserByUsername("test")
        }
    }

    @Test
    fun `deleteUserByEmail success`() {
        val user = User(uid = "123", username = "test", email = "test@example.com")
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
        val existingUser = User(uid = "123", username = "old", email = "ex@123.com")
        val updatedDto = UserDto(uid = "123", username = "new", email = "ex@123.com")
        whenever(userRepository.findByUid("123")).thenReturn(Optional.of(existingUser))
        whenever(userRepository.save(existingUser)).thenAnswer { it.arguments[0] }
        val result = userService.updateUser(updatedDto)
        assertEquals("new", result.username)
    }

    @Test
    fun `updateUser not found`() {
        val updatedDto = UserDto(uid = "123", username = "new", email = "test@example.com")
        whenever(userRepository.findByUid("123")).thenReturn(Optional.empty())
        assertThrows(UserExceptions.UserNotFoundException::class.java) {
            userService.updateUser(updatedDto)
        }
    }
}
