package travesium.userservice.service

import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.PageRequest
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import travesium.userservice.db.model.User
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
    private val eventPublisher: ApplicationEventPublisher,
    private val firebaseService: FirebaseService) {

    @Transactional
    fun createUser(userDto: UserDto): UserDto {
        if (userDto.username == null || userDto.email == null || userDto.userId != null || userDto.firebaseId == null) {
            throw UserExceptions.InvalidUserDataException("Username,email and firebase id cannot be null. New user cannot have userId")
        }

        checkAuthorization(userDto.firebaseId, userDto.email)

        if (userRepository.findByUsername(userDto.username).isPresent || userRepository.findByEmail(userDto.email).isPresent) {
            throw UserExceptions.UserAlreadyExistsException()
        }

        return UserMapper.toEntity(userDto).let { user ->
            val savedUser = userRepository.save(user)
            publishUserEvent(UserEvent.USER_CREATED)
            UserMapper.toDto(savedUser)
        }
    }

    fun getUser(username: String?, email: String?): UserDto {
        if (username == null && email == null) {
            throw UserExceptions.InvalidUserDataException("Username or email must be provided.")
        }

        return if (username != null) {
            getUserByUsername(username)
        } else {
            getUserByEmail(email!!)
        }
    }

    private fun getUserByUsername(username: String): UserDto =
        UserMapper.toDto(userRepository.findByUsername(username).orElseThrow { UserExceptions.UserNotFoundException() })

    private fun getUserByEmail(email: String): UserDto =
        UserMapper.toDto(userRepository.findByEmail(email).orElseThrow { UserExceptions.UserNotFoundException() })

    @Transactional
    fun deleteUser() {
        val user = getUserFromContext()

        val deletedUser = user.copy(deleted = true)
        userRepository.save(deletedUser)

        publishUserEvent(UserEvent.USER_DELETED)
    }

    @Transactional
    fun updateUser(userDto: UserDto): UserDto {
        if (userDto.userId == null) {
            throw UserExceptions.InvalidUserDataException("User UID cannot be null for update.")
        }
        val existingUser = userRepository.findByUserId(userDto.userId).orElseThrow { UserExceptions.UserNotFoundException() }

        checkAuthorization(existingUser.firebaseId!!, existingUser.email!!)

        val updatedUser = existingUser.copy(
            displayName = userDto.displayName ?: existingUser.displayName,
            description = userDto.description ?: existingUser.description,
            avatarPhotoReference = userDto.avatarPhotoReference ?: existingUser.avatarPhotoReference,
            coverPhotoReference = userDto.coverPhotoReference ?: existingUser.coverPhotoReference,
            firstName = userDto.firstName ?: existingUser.firstName,
            lastName = userDto.lastName ?: existingUser.lastName,
            countryOfOrigin = userDto.countryOfOrigin ?: existingUser.countryOfOrigin,
            gender =  userDto.gender ?: existingUser.gender
        )

        userRepository.save(updatedUser)
        return UserMapper.toDto(updatedUser)
    }

    @Transactional
    fun getUsersByUsernames(usernames: List<String>, offset: Int, limit: Int): List<UserDto> {
        if (usernames.isEmpty()) return emptyList()

        val users = userRepository.findByUsernames(usernames, PageRequest.of(offset / limit, limit))

        return users.map { UserMapper.toDto(it) }
    }

    @Transactional
    fun followUser(followedUsername: String) {
        val followed = userRepository.findByUsername(followedUsername)
            .orElseThrow { UserExceptions.UserNotFoundException() }

        val follower = getUserFromContext()

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
    fun unfollowUser(followedUsername: String) {
        val followed = userRepository.findByUsername(followedUsername)
            .orElseThrow { UserExceptions.UserNotFoundException() }

        val follower = getUserFromContext()

        if (follower.userId == followed.userId) {
            throw UserExceptions.InvalidUserDataException("User cannot unfollow themselves.")
        }

        if (followed in follower.following) {
            follower.following.remove(followed)
        }
    }

    fun getFollowers(username: String, offset: Int, limit: Int): List<UserDto> {
        val user = userRepository.findByUsername(username)
            .orElseThrow { UserExceptions.UserNotFoundException() }

        val pageable = PageRequest.of(offset / limit, limit)

        val followers = userRepository.findFollowers(user.userId!!, pageable)

        return followers.map { UserMapper.toDto(it) }
    }

    fun getFollowing(username: String, offset: Int, limit: Int): List<UserDto> {
        val user = userRepository.findByUsername(username)
            .orElseThrow { UserExceptions.UserNotFoundException() }

        val pageable = PageRequest.of(offset / limit, limit)

        val following = userRepository.findFollowing(user.userId!!, pageable)

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
    fun blockUser(blockedUsername: String) {
        val blocked = userRepository.findByUsername(blockedUsername)
            .orElseThrow { UserExceptions.UserNotFoundException() }

        val blocker = getUserFromContext()

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
    fun unblockUser(blockedUsername: String) {
        val blocked = userRepository.findByUsername(blockedUsername)
            .orElseThrow { UserExceptions.UserNotFoundException() }

        val blocker = getUserFromContext()

        if (blocked in blocker.blocked) {
            blocker.blocked.remove(blocked)
        }
    }

    fun getBlockedUsers(offset: Int, limit: Int): List<UserDto> {
        val user = getUserFromContext()

        val pageable = PageRequest.of(offset / limit, limit)

        val blockedList = userRepository.findBlocked(user.userId!!, pageable)

        return blockedList.map { UserMapper.toDto(it) }
    }

    fun countBlockedUsers(): Int {
        val user = getUserFromContext()

        return user.blocked.size
    }

    fun checkIfUserExists(username: String?, email: String?): Boolean {
        if (username == null && email == null) {
            throw UserExceptions.InvalidUserDataException("Username or email must be provided.")
        }

        return (username?.let { userRepository.findByUsername(it).isPresent } == true) ||
            (email?.let { userRepository.findByEmail(it).isPresent } == true)
    }

    private fun publishUserEvent(action: UserEvent) {
        val event = ReportingStreamData(
            timestamp = YearMonth.now(),
            action = action
        )
        eventPublisher.publishEvent(event)
    }

    private fun checkAuthorization(userFireBaseId: String, userEmail: String) {
        val firebaseId = firebaseService.extractUidFromToken(SecurityContextHolder.getContext().authentication.credentials as String)
        val emailFromToken = firebaseService.extractEmailFromToken(SecurityContextHolder.getContext().authentication.credentials as String)
        if (firebaseId != userFireBaseId && emailFromToken != userEmail) {
            throw UserExceptions.UnauthorizedException("User is not authorized to perform this action.")
        }
    }

    private fun getUserFromContext(): User {
        val firebaseId = firebaseService.extractUidFromToken(SecurityContextHolder.getContext().authentication.credentials as String)
        return userRepository.findByFirebaseId(firebaseId)
            .orElseThrow { UserExceptions.UserNotFoundException() }
    }
}