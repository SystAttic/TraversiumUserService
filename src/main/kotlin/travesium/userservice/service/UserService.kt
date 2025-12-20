package travesium.userservice.service

import org.apache.logging.log4j.kotlin.logger
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.PageRequest
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import traversium.audit.kafka.ActivityType
import traversium.audit.kafka.AuditStreamData
import traversium.audit.kafka.EntityType
import traversium.audit.kafka.UserActivityAction
import traversium.notification.kafka.ActionType
import traversium.notification.kafka.NotificationStreamData
import travesium.userservice.db.model.User
import travesium.userservice.db.repository.UserRepository
import travesium.userservice.dto.UserDto
import travesium.userservice.exceptions.UserExceptions
import travesium.userservice.kafka.data.ReportingStreamData
import travesium.userservice.kafka.data.UserEvent
import travesium.userservice.mapper.UserMapper
import java.time.OffsetDateTime
import java.time.YearMonth

/**
 * @author Maja Razinger
 */
@Service
class UserService(
    private val userRepository: UserRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val tripServiceGrpcClient: TripServiceGrpcClient,
    private val moderationServiceGrpcClient: ModerationServiceGrpcClient,
    private val firebaseService: FirebaseService)
{

    @Transactional
    fun createUser(userDto: UserDto): UserDto {
        if (userDto.username == null || userDto.email == null || userDto.userId != null || userDto.firebaseId == null) {
            throw UserExceptions.InvalidUserDataException("Username,email and firebase id cannot be null. New user cannot have userId")
        }

        checkAuthorization(userDto.firebaseId, userDto.email)

        if (userRepository.findByUsername(userDto.username).isPresent || userRepository.findByEmail(userDto.email).isPresent) {
            throw UserExceptions.UserAlreadyExistsException("User with username '${userDto.username}' or email '${userDto.email}' already exists")
        }

        validateUserContentModeration(userDto.description, userDto.displayName, userDto.firstName, userDto.lastName)

        return UserMapper.toEntity(userDto).let { user ->
            val savedUser = userRepository.save(user)
            publishUserEvent(UserEvent.USER_CREATED)
            publishAuditEvent(savedUser.firebaseId!!, UserActivityAction.USER_CREATED.name, savedUser.userId!!)
            UserMapper.toDto(savedUser)
        }
    }

    @Transactional
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

        publishAuditEvent(user.firebaseId!!, UserActivityAction.USER_DELETED.name, user.userId!!)
    }

    @Transactional
    fun updateUser(userDto: UserDto): UserDto {
        if (userDto.userId == null) {
            throw UserExceptions.InvalidUserDataException("User UID cannot be null for update.")
        }
        val existingUser = userRepository.findByUserId(userDto.userId).orElseThrow { UserExceptions.UserNotFoundException() }

        checkAuthorization(existingUser.firebaseId!!, existingUser.email!!)

        validateUserContentModeration(userDto.description, userDto.displayName, userDto.firstName, userDto.lastName)

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

        val changedFields = getChangedFields(UserMapper.toDto(existingUser), UserMapper.toDto(updatedUser))

        userRepository.save(updatedUser)
        changedFields.forEach { action ->
            publishAuditEvent(existingUser.firebaseId, action, existingUser.userId!!)
        }

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

        if (followed in follower.blocked) {
            throw UserExceptions.InvalidUserDataException("Cannot follow a user you have blocked.")
        }

        if (!userRepository.checkIfUserAIsFollowingUserB(follower.userId!!, followed.userId!!)) {
            follower.following.add(followed)
            publishFollowNotification(follower.username!!, followed.username!!)
            publishAuditEvent(follower.firebaseId!!, UserActivityAction.USER_FOLLOWED.name, follower.userId, "followedUserId" to followed.firebaseId!!)
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

        if (userRepository.checkIfUserAIsFollowingUserB(follower.userId!!, followed.userId!!)) {
            userRepository.removeFollowerByUserId(follower.userId, followed.userId)
            publishAuditEvent(follower.firebaseId!!, UserActivityAction.USER_UNFOLLOWED.name, follower.userId, "unfollowedUserId" to followed.firebaseId!!)
            logger.info { "User $followedUsername has been unfollowed" }
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

        if (blocked in blocker.blocked) {
            return
        }

        if (blocker.userId == blocked.userId)
            throw UserExceptions.InvalidUserDataException("Cannot block self.")

        val success = try {
            tripServiceGrpcClient.removeUserRelations(blocker.firebaseId!!, blocked.firebaseId!!)
        } catch (e: Exception) {
            logger.error("Failed to remove user relations in TripService after retries: ${e.message}")
            throw UserExceptions.RemoteServiceException("TripService")
        }

        if (blocked !in blocker.blocked && success) {
            blocker.blocked.add(blocked)
            userRepository.removeFollowerByUserId(blocker.userId!!, blocked.userId!!)
            userRepository.removeFollowerByUserId(blocked.userId, blocker.userId)

            publishAuditEvent(blocker.firebaseId!!, UserActivityAction.USER_BLOCKED.name, blocker.userId, "blockedUserId" to blocked.firebaseId!!)
        }
    }


    @Transactional
    fun unblockUser(blockedUsername: String) {
        val blocked = userRepository.findByUsername(blockedUsername)
            .orElseThrow { UserExceptions.UserNotFoundException() }

        val blocker = getUserFromContext()

        if (blocked in blocker.blocked) {
            blocker.blocked.remove(blocked)
            publishAuditEvent(blocker.firebaseId!!, UserActivityAction.USER_UNBLOCKED.name, blocker.userId!!, "unblockedUserId" to blocked.firebaseId!!)
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

    fun searchUsersByUsername(query: String, offset: Int, limit: Int): List<UserDto> {
        if (query.isBlank()) {
            return emptyList()
        }

        val pageable = PageRequest.of(offset / limit, limit)
        val users = userRepository.searchUsersByUsername(query, pageable)

        return users.map { UserMapper.toDto(it) }
    }

    private fun publishUserEvent(action: UserEvent) {
        val event = ReportingStreamData(
            timestamp = YearMonth.now(),
            action = action
        )
        eventPublisher.publishEvent(event)
    }

    private fun publishFollowNotification(followerUsername: String, followedUsername: String) {
        val event = NotificationStreamData(
            senderId = followerUsername,
            receiverIds = listOf(followedUsername),
            action = ActionType.FOLLOW,
            timestamp = OffsetDateTime.now(),
            collectionReferenceId = null,
            nodeReferenceId = null,
            commentReferenceId = null,
            mediaReferenceId = null,
            mediaCount = null
        )

        eventPublisher.publishEvent(event)
    }

    private fun getChangedFields(existingUser: UserDto, userDto: UserDto): List<String> {
        val changedFields = mutableListOf<String>()

        if (userDto.displayName != null && userDto.displayName != existingUser.displayName) {
            changedFields.add(UserActivityAction.USER_DISPLAY_NAME_CHANGED.name)
        }
        if (userDto.description != null && userDto.description != existingUser.description) {
            changedFields.add(UserActivityAction.USER_DESCRIPTION_CHANGED.name)
        }
        if (userDto.avatarPhotoReference != null && userDto.avatarPhotoReference != existingUser.avatarPhotoReference) {
            changedFields.add(UserActivityAction.USER_AVATAR_PHOTO_CHANGED.name)
        }
        if (userDto.coverPhotoReference != null && userDto.coverPhotoReference != existingUser.coverPhotoReference) {
            changedFields.add(UserActivityAction.USER_COVER_PHOTO_CHANGED.name)
        }
        if (userDto.firstName != null && userDto.firstName != existingUser.firstName) {
            changedFields.add(UserActivityAction.USER_FIRST_NAME_CHANGED.name)
        }
        if (userDto.lastName != null && userDto.lastName != existingUser.lastName) {
            changedFields.add(UserActivityAction.USER_LAST_NAME_CHANGED.name)
        }
        if (userDto.countryOfOrigin != null && userDto.countryOfOrigin != existingUser.countryOfOrigin) {
            changedFields.add(UserActivityAction.USER_COUNTRY_OF_ORIGIN_CHANGED.name)
        }
        if (userDto.gender != null && userDto.gender != existingUser.gender) {
            changedFields.add(UserActivityAction.USER_GENDER_CHANGED.name)
        }

        return changedFields
    }

    private fun validateUserContentModeration(description: String?, displayName: String?, firstName: String?, lastName: String?) {
        val allowed = try {
            val textToModerate = buildString {
                description?.let { append(it).append(" ") }
                displayName?.let { append(it).append(" ") }
                firstName?.let { append(it).append(" ") }
                lastName?.let { append(it) }
            }
            if (textToModerate.isNotBlank()) {
                moderationServiceGrpcClient.isTextAllowed(textToModerate)
            } else {
                true
            }
        } catch (e: Exception) {
            throw UserExceptions.UserModerationException("Moderation service unavailable", e)
        }

        if (!allowed) {
            throw UserExceptions.UserModerationException("User content violates moderation policy!")
        }
    }

    private fun publishAuditEvent(firebaseId: String, action: String, userId: Long, vararg metadata: Pair<String, String>) {
        val auditEvent = AuditStreamData(
            timestamp = OffsetDateTime.now(),
            userId = firebaseId,
            activityType = ActivityType.USER_ACTIVITY,
            action = action,
            entityType = EntityType.USER,
            entityId = userId,
            tripId = null,
            metadata = mapOf(
                *metadata,
                "userId" to userId,
                "entityType" to "USER",
                "action" to action
            )
        )

        eventPublisher.publishEvent(auditEvent)
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