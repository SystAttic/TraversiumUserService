package travesium.userservice.service

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import travesium.userservice.db.repository.UserRepository
import travesium.userservice.dto.UserDto
import travesium.userservice.exceptions.UserExceptions
import travesium.userservice.kafka.data.ReportingStreamData
import travesium.userservice.kafka.data.UserEvent
import travesium.userservice.mapper.UserMapper
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
        if (userDto.username == null || userDto.email == null || userDto.userId != null) {
            throw UserExceptions.InvalidUserDataException("Username and email cannot be null. New user cannot have userId")
        }

        if (userRepository.findByUsername(userDto.username).isPresent || userRepository.findByEmail(userDto.email).isPresent) {
            throw UserExceptions.UserAlreadyExistsException()
        }

        return UserMapper.toEntity(userDto).let { user ->
            val savedUser = userRepository.save(user)
            publishUserEvent(UserEvent.USER_CREATED)
            UserMapper.toDto(savedUser)
        }
    }

    fun getUserByUsername(username: String): UserDto = UserMapper.toDto(userRepository.findByUsername(username).orElseThrow { UserExceptions.UserNotFoundException() })

    fun getUserByEmail(email: String): UserDto = UserMapper.toDto(userRepository.findByEmail(email).orElseThrow { UserExceptions.UserNotFoundException() })

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

    @Transactional
    fun updateUser(userDto: UserDto): UserDto {
        if (userDto.userId == null) {
            throw UserExceptions.InvalidUserDataException("User UID cannot be null for update.")
        }
        val existingUser = userRepository.findByUserId(userDto.userId).orElseThrow { UserExceptions.UserNotFoundException() }

        val updatedUser = existingUser.copy(
            displayName = userDto.displayName ?: existingUser.displayName,
            description = userDto.description ?: existingUser.description,
            avatarPhotoReference = userDto.avatarPhotoReference ?: existingUser.avatarPhotoReference,
            coverPhotoReference = userDto.coverPhotoReference ?: existingUser.coverPhotoReference
        )

        userRepository.save(updatedUser)
        return UserMapper.toDto(updatedUser)
    }

    @Transactional
    fun followUser(followerUsername: String, followedUsername: String) {
        val follower = userRepository.findByUsername(followerUsername)
            .orElseThrow { UserExceptions.UserNotFoundException() }
        val followed = userRepository.findByUsername(followedUsername)
            .orElseThrow { UserExceptions.UserNotFoundException() }

        if (follower.userId == followed.userId) {
            throw UserExceptions.InvalidUserDataException("User cannot follow themselves.")
        }

        if (follower in followed.blocked) {
            throw UserExceptions.InvalidUserDataException("Cannot follow a user who has blocked you.")
        }

        if (followed !in follower.following) {
            follower.following.add(followed)
        }
    }

    @Transactional
    fun unfollowUser(followerUsername: String, followedUsername: String) {
        val follower = userRepository.findByUsername(followerUsername)
            .orElseThrow { UserExceptions.UserNotFoundException() }
        val followed = userRepository.findByUsername(followedUsername)
            .orElseThrow { UserExceptions.UserNotFoundException() }

        if (follower.userId == followed.userId) {
            throw UserExceptions.InvalidUserDataException("User cannot unfollow themselves.")
        }

        if (followed in follower.following) {
            follower.following.remove(followed)
        }
    }

    fun getFollowers(username: String): List<UserDto> {
        val user = userRepository.findByUsername(username)
            .orElseThrow { UserExceptions.UserNotFoundException() }

        val followers = userRepository.findFollowers(user.userId!!)

        return followers.map { UserMapper.toDto(it) }
    }

    fun getFollowing(username: String): List<UserDto> {
        val user = userRepository.findByUsername(username)
            .orElseThrow { UserExceptions.UserNotFoundException() }

        val following = userRepository.findFollowing(user.userId!!)

        return following.map { UserMapper.toDto(it) }
    }

    fun countFollowers(username: String): Int {
        val user = userRepository.findByUsername(username)
            .orElseThrow { UserExceptions.UserNotFoundException() }
        return user.followers.size
    }

    fun countFollowing(username: String): Int {
        val user = userRepository.findByUsername(username)
            .orElseThrow { UserExceptions.UserNotFoundException() }
        return user.following.size
    }

    @Transactional
    fun blockUser(blockerUsername: String, blockedUsername: String) {
        val blocker = userRepository.findByUsername(blockerUsername)
            .orElseThrow { UserExceptions.UserNotFoundException() }
        val blocked = userRepository.findByUsername(blockedUsername)
            .orElseThrow { UserExceptions.UserNotFoundException() }

        if (blocker.userId == blocked.userId) throw UserExceptions.InvalidUserDataException("Cannot block self.")

        if (blocked !in blocker.blocked) {
            blocker.blocked.add(blocked)
        }

        blocker.following.remove(blocked)
        blocker.followers.remove(blocked)
        blocked.following.remove(blocker)
        blocked.followers.remove(blocker)
    }

    @Transactional
    fun unblockUser(blockerUsername: String, blockedUsername: String) {
        val blocker = userRepository.findByUsername(blockerUsername)
            .orElseThrow { UserExceptions.UserNotFoundException() }
        val blocked = userRepository.findByUsername(blockedUsername)
            .orElseThrow { UserExceptions.UserNotFoundException() }

        if (blocked in blocker.blocked) {
            blocker.blocked.remove(blocked)
        }
    }

    fun getBlockedUsers(username: String): List<UserDto> {
        val user = userRepository.findByUsername(username)
            .orElseThrow { UserExceptions.UserNotFoundException() }

        val blockedList = userRepository.findBlocked(user.userId!!)

        return blockedList.map { UserMapper.toDto(it) }
    }

    fun countBlockedUsers(username: String): Int {
        val user = userRepository.findByUsername(username)
            .orElseThrow { UserExceptions.UserNotFoundException() }
        return user.blocked.size
    }

    private fun publishUserEvent(action: UserEvent) {
        val event = ReportingStreamData(
            timestamp = YearMonth.now(),
            action = action
        )
        eventPublisher.publishEvent(event)
    }
}