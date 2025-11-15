package travesium.userservice.mapper

import travesium.userservice.db.model.User
import travesium.userservice.dto.UserDto

/**
 * @author Maja Razinger
 */
object UserMapper {

    fun toDto(user: User): UserDto {
        return UserDto(
            userId = user.userId,
            username = user.username,
            email = user.email,
            displayName = user.displayName,
            avatarPhotoReference = user.avatarPhotoReference,
            coverPhotoReference = user.coverPhotoReference,
            createdAt = user.createdAt,
            firstName = user.firstName,
            lastName = user.lastName,
            countryOfOrigin = user.countryOfOrigin,
            gender = user.gender,
            description = user.description,
            firebaseId = user.firebaseId,
            deleted = user.deleted
        )
    }

    fun toEntity(dto: UserDto): User {
        return User(
            userId = dto.userId,
            username = dto.username,
            email = dto.email,
            displayName = dto.displayName,
            avatarPhotoReference = dto.avatarPhotoReference,
            coverPhotoReference = dto.coverPhotoReference,
            createdAt = dto.createdAt,
            firstName = dto.firstName,
            lastName = dto.lastName,
            countryOfOrigin = dto.countryOfOrigin,
            gender = dto.gender,
            description = dto.description,
            firebaseId = dto.firebaseId,
            deleted = dto.deleted
        )
    }
}