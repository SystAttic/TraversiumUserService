package travesium.userservice.mapper

import travesium.userservice.db.model.User
import travesium.userservice.dto.UserDto

/**
 * @author Maja Razinger
 */
object UserMapper {

    fun toDto(user: User, shallow: Boolean = false): UserDto {
        return UserDto(
            userId = user.userId,
            username = user.username,
            email = user.email,
            displayName = user.displayName,
            photoReference = user.avatarPhotoReference,
            deleted = user.deleted,
            following = if (!shallow) user.following.map { toDto(it, true) }.toSet() else emptySet(),
            followers = if (!shallow) user.followers.map { toDto(it, true) }.toSet() else emptySet(),
            blocked = if (!shallow) user.blocked.map { toDto(it, true) }.toSet() else emptySet()
        )
    }

    fun toEntity(dto: UserDto, shallow: Boolean = false): User {
        return User(
            userId = dto.userId,
            username = dto.username,
            email = dto.email,
            displayName = dto.displayName,
            avatarPhotoReference = dto.photoReference,
            deleted = dto.deleted,
            following = if (!shallow) dto.following.map { toEntity(it, true) }.toMutableSet() else mutableSetOf(),
            followers = if (!shallow) dto.followers.map { toEntity(it, true) }.toMutableSet() else mutableSetOf(),
            blocked = if (!shallow) dto.blocked.map { toEntity(it, true) }.toMutableSet() else mutableSetOf()
        )
    }
}