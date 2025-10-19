package travesium.userservice.service

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import travesium.userservice.db.repository.UserRepository
import travesium.userservice.dto.UserDto
import travesium.userservice.exceptions.UserExceptions
import travesium.userservice.kafka.data.ReportingStreamData
import travesium.userservice.kafka.data.UserEvent
import java.time.YearMonth

/**
 * @author Maja Razinger
 */
@Service
class UserService(
    private val userRepository: UserRepository,
    private val eventPublisher: ApplicationEventPublisher) {

    @Transactional
    fun createUser(userDto: UserDto): UserDto {
        if (userRepository.findByUid(userDto.uid).isPresent || userRepository.findByUsername(userDto.username).isPresent || userRepository.findByEmail(userDto.email).isPresent) {
            throw UserExceptions.UserAlreadyExistsException()
        }

        return userDto.toUser().let {
            userRepository.save(it)
            publishUserEvent(UserEvent.USER_CREATED)
            it.toDto()
        }
    }

    fun getUserByUsername(username: String): UserDto {
        val user = userRepository.findByUsername(username).orElseThrow { UserExceptions.UserNotFoundException() }
        return user.toDto()
    }

    fun getUserByEmail(email: String): UserDto {
        val user = userRepository.findByEmail(email).orElseThrow { UserExceptions.UserNotFoundException() }
        return user.toDto()
    }

    @Transactional
    fun deleteUserByUsername(username: String) {
        val user = userRepository.findByUsername(username).orElseThrow { UserExceptions.UserNotFoundException() }
        publishUserEvent(UserEvent.USER_DELETED)
        userRepository.delete(user)
    }

    @Transactional
    fun deleteUserByEmail(email: String) {
        val user = userRepository.findByEmail(email).orElseThrow { UserExceptions.UserNotFoundException() }
        publishUserEvent(UserEvent.USER_DELETED)
        userRepository.delete(user)
    }

    fun updateUser(userDto: UserDto): UserDto {
        val existingUser = userRepository.findByUid(userDto.uid).orElseThrow { UserExceptions.UserNotFoundException() }

        val updatedUser = existingUser.copy(
            username = userDto.username,
            email = userDto.email,
            displayName = userDto.displayName,
            photoReference = userDto.photoReference
        )

        userRepository.save(updatedUser)
        return updatedUser.toDto()
    }

    private fun publishUserEvent(action: UserEvent) {
        val event = ReportingStreamData(
            timestamp = YearMonth.now(),
            action = action
        )
        eventPublisher.publishEvent(event)
    }
}