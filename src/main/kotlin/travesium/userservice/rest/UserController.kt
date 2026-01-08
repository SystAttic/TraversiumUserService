package travesium.userservice.rest


import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import travesium.userservice.dto.ErrorResponse
import travesium.userservice.dto.UserDto
import travesium.userservice.exceptions.UserExceptions
import travesium.userservice.service.UserService
import io.swagger.v3.oas.annotations.parameters.RequestBody as SwaggerRequestBody


/**
 * @author Maja Razinger
 */
@RestController
@RequestMapping("/rest/v1/users")
class UserController(private val userService: UserService) : Logging {

    @PostMapping
    @Operation(
        operationId = "createUser",
        tags = ["User"],
        summary = "Create a user.",
        description = "Registers a new user.",
        requestBody = SwaggerRequestBody(
            description = "User data for registration",
            required = true,
            content = [Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = Schema(implementation = UserDto::class),
                examples = [
                    ExampleObject(
                        name = "CreateUserExample",
                        summary = "Example user registration",
                        value = """
                        {
                          "username": "johndoe",
                          "email": "johndoe@example.com",
                          "displayName": "John Doe",
                          "firstName": "John",
                          "lastName": "Doe",
                          "countryOfOrigin": "USA",
                          "gender": "Male",
                          "description": "Travel enthusiast exploring the world",
                          "firebaseId": "firebase_uid_123"
                        }
                        """
                    )
                ]
            )]
        ),
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully created the user.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = UserDto::class)
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Bad Request - Invalid user data provided.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        value = """{"message":"Invalid user data: username is required","status":400,"errorCode":"INVALID_USER_DATA","timestamp":"2026-01-01T12:00:00Z","path":"/rest/v1/users"}"""
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "403",
                description = "Forbidden - Unauthorized to create user.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        value = """{"message":"Unauthorized","status":403,"errorCode":"UNAUTHORIZED","timestamp":"2026-01-01T12:00:00Z","path":"/rest/v1/users"}"""
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "409",
                description = "Conflict - User with the same username or email already exists.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        value = """{"message":"User with username 'johndoe' already exists","status":409,"errorCode":"USER_ALREADY_EXISTS","timestamp":"2026-01-01T12:00:00Z","path":"/rest/v1/users"}"""
                    )]
                )]
            )
        ]
    )
    fun createUser(@RequestBody userDto: UserDto): ResponseEntity<UserDto> {
        val savedUser = userService.createUser(userDto)
        logger.info("User with UID ${savedUser.userId} created or updated")
        return ResponseEntity.ok(savedUser)
    }

    @GetMapping
    @Operation(
        operationId = "getUserByUsername",
        tags = ["User"],
        summary = "Get a user by username.",
        description = "Get a user by username.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved the user by username.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = UserDto::class)
                )]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Not found - User not found.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        value = """{"message":"User not found","status":404,"errorCode":"USER_NOT_FOUND","timestamp":"2026-01-01T12:00:00Z","path":"/rest/v1/users"}"""
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Bad Request - Neither username nor email provided.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        value = """{"message":"Invalid user data: Either username or email must be provided","status":400,"errorCode":"INVALID_USER_DATA","timestamp":"2026-01-01T12:00:00Z","path":"/rest/v1/users"}"""
                    )]
                )]
            )
        ]
    )
    fun getUser(
        @RequestParam username: String?,
        @RequestParam email: String?,
    ): ResponseEntity<UserDto> {
        if (username.isNullOrBlank() && email.isNullOrBlank()) {
            logger.info("Neither username nor email provided for existence check.")
            throw UserExceptions.InvalidUserDataException("Either username or email must be provided")
        }

        val user = userService.getUser(username, email)
        logger.info("User with the username  ${username ?: "N/A"}} or email  ${email ?: "N/A"} found.")
        return ResponseEntity.ok(user)
    }

    @GetMapping("/{userId}")
    @Operation(
        operationId = "getUserById",
        tags = ["User"],
        summary = "Get a user by user ID.",
        description = "Get a user by user ID.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved the user by user ID.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = UserDto::class)
                )]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Not found - User not found.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        value = """{"message":"User not found","status":404,"errorCode":"USER_NOT_FOUND","timestamp":"2026-01-01T12:00:00Z","path":"/rest/v1/users/123"}"""
                    )]
                )]
            )
        ]
    )
    fun getUserById(@PathVariable userId: Long): ResponseEntity<UserDto> {
        val user = userService.getUserById(userId)
        logger.info("User with ID $userId found.")
        return ResponseEntity.ok(user)
    }

    @DeleteMapping()
    @Operation(
        operationId = "deleteUser",
        tags = ["User"],
        summary = "Delete a user.",
        description = "Delete a user.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully deleted the user."
            ),
            ApiResponse(
                responseCode = "403",
                description = "Forbidden - Unauthorized to delete user.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        value = """{"message":"Unauthorized","status":403,"errorCode":"UNAUTHORIZED","timestamp":"2026-01-01T12:00:00Z","path":"/rest/v1/users"}"""
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Not found - User not found.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        value = """{"message":"User not found","status":404,"errorCode":"USER_NOT_FOUND","timestamp":"2026-01-01T12:00:00Z","path":"/rest/v1/users"}"""
                    )]
                )]
            )
        ]
    )
    fun deleteUserByUsername(): ResponseEntity<Unit> {
        userService.deleteUser()
        logger.info("User deleted.")
        return ResponseEntity.ok().build()
    }

    @PutMapping()
    @Operation(
        operationId = "updateUserByEmail",
        tags = ["User"],
        summary = "Update a user by email.",
        description = "Update a user by email.",
        requestBody = SwaggerRequestBody(
            description = "Updated user data",
            required = true,
            content = [Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = Schema(implementation = UserDto::class),
                examples = [
                    ExampleObject(
                        name = "UpdateUserExample",
                        summary = "Example user update",
                        value = """
                        {
                          "username": "johndoe",
                          "email": "johndoe@example.com",
                          "displayName": "John D.",
                          "firstName": "John",
                          "lastName": "Doe",
                          "countryOfOrigin": "Canada",
                          "gender": "Male",
                          "description": "Updated bio - Adventure seeker and photographer",
                          "avatarPhotoReference": "profile/johndoe/avatar.jpg",
                          "coverPhotoReference": "profile/johndoe/cover.jpg"
                        }
                        """
                    )
                ]
            )]
        ),
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully updated the user by email.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = UserDto::class)
                )]
            ),
            ApiResponse(
                responseCode = "403",
                description = "Forbidden - Unauthorized to update user.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        value = """{"message":"Unauthorized","status":403,"errorCode":"UNAUTHORIZED","timestamp":"2026-01-01T12:00:00Z","path":"/rest/v1/users"}"""
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Not found - User not found.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        value = """{"message":"User not found","status":404,"errorCode":"USER_NOT_FOUND","timestamp":"2026-01-01T12:00:00Z","path":"/rest/v1/users"}"""
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Bad Request - Invalid user data or moderation policy violation.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "InvalidData",
                        summary = "Invalid user data",
                        value = """{"message":"Invalid user data: description is required","status":400,"errorCode":"INVALID_USER_DATA","timestamp":"2026-01-01T12:00:00Z","path":"/rest/v1/users"}"""
                    ), ExampleObject(
                        name = "ModerationViolation",
                        summary = "Moderation policy violation",
                        value = """{"message":"Content violates moderation policy","status":400,"errorCode":"MODERATION_POLICY_VIOLATION","timestamp":"2026-01-01T12:00:00Z","path":"/rest/v1/users"}"""
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal Server Error - Moderation service unavailable.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        value = """{"message":"Moderation service unavailable","status":500,"errorCode":"MODERATION_SERVICE_UNAVAILABLE","timestamp":"2026-01-01T12:00:00Z","path":"/rest/v1/users"}"""
                    )]
                )]
            )
        ]
    )
    fun updateUser(@RequestBody userDto: UserDto): ResponseEntity<UserDto> {
        val updatedUser = userService.updateUser(userDto)
        logger.info("User with the username ${userDto.username} updated.")
        return ResponseEntity.ok(updatedUser)
    }

    @PostMapping("/userList")
    @Operation(
        operationId = "userList",
        tags = ["User"],
        summary = "Get a list of users by usernames.",
        description = "Return users named in the list of usernames.",
        requestBody = SwaggerRequestBody(
            description = "List of usernames to retrieve",
            required = true,
            content = [Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = [
                    ExampleObject(
                        name = "UsernameListExample",
                        summary = "Example list of usernames",
                        value = """
                        [
                          "johndoe",
                          "janedoe",
                          "traveler123"
                        ]
                        """
                    )
                ]
            )]
        ),
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved the list of users.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = UserDto::class)
                )],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Bad Request - Invalid list of usernames provided.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        value = """{"message":"Invalid user data: username list cannot be empty","status":400,"errorCode":"INVALID_USER_DATA","timestamp":"2026-01-01T12:00:00Z","path":"/rest/v1/users/userList"}"""
                    )]
                )]
            )
        ]
    )
    fun listOfUsers(
        @RequestBody usernames: List<String>,
        @RequestParam(defaultValue = "0") offset: Int,
        @RequestParam(defaultValue = "20") limit: Int
        ): ResponseEntity<List<UserDto>> {
        val users = userService.getUsersByUsernames(usernames, offset, limit)
        logger.info("Retrieved list of users for provided usernames.")
        return ResponseEntity.ok(users)
    }

    @PostMapping("/follow/{toFollowUsername}")
    @Operation(
        operationId = "followUser",
        tags = ["User"],
        summary = "Follow a user.",
        description = "Make the user follow another user.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully followed the user."
            ),
            ApiResponse(
                responseCode = "403",
                description = "Forbidden - Unauthorized to follow user.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        value = """{"message":"Unauthorized","status":403,"errorCode":"UNAUTHORIZED","timestamp":"2026-01-01T12:00:00Z","path":"/rest/v1/users/follow/johndoe"}"""
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Not found - User not found.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        value = """{"message":"User not found","status":404,"errorCode":"USER_NOT_FOUND","timestamp":"2026-01-01T12:00:00Z","path":"/rest/v1/users/follow/johndoe"}"""
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Bad Request - Invalid follow operation.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        value = """{"message":"Invalid user data: Cannot follow yourself","status":400,"errorCode":"INVALID_USER_DATA","timestamp":"2026-01-01T12:00:00Z","path":"/rest/v1/users/follow/johndoe"}"""
                    )]
                )]
            )
        ]
    )
    fun followUser(
        @PathVariable toFollowUsername: String
    ): ResponseEntity<Unit> {
        userService.followUser(toFollowUsername)
        logger.info("Followed user $toFollowUsername.")
        return ResponseEntity.ok().build()
    }

    @PostMapping("/unfollow/{toUnfollowUsername}")
    @Operation(
        operationId = "unfollowUser",
        tags = ["User"],
        summary = "Unfollow a user.",
        description = "Make the user unfollow another user.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully unfollowed the user."
            ),
            ApiResponse(
                responseCode = "403",
                description = "Forbidden - Unauthorized to follow user.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        value = """{"message":"Unauthorized","status":403,"errorCode":"UNAUTHORIZED","timestamp":"2026-01-01T12:00:00Z","path":"/rest/v1/users/unfollow/johndoe"}"""
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Not found - User not found.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        value = """{"message":"User not found","status":404,"errorCode":"USER_NOT_FOUND","timestamp":"2026-01-01T12:00:00Z","path":"/rest/v1/users/unfollow/johndoe"}"""
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Bad Request - Invalid unfollow operation.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        value = """{"message":"Invalid user data: Cannot unfollow yourself","status":400,"errorCode":"INVALID_USER_DATA","timestamp":"2026-01-01T12:00:00Z","path":"/rest/v1/users/unfollow/johndoe"}"""
                    )]
                )]
            )
        ]
    )
    fun unfollowUser(
        @PathVariable toUnfollowUsername: String
    ): ResponseEntity<Unit> {
        userService.unfollowUser(toUnfollowUsername)
        logger.info("Unfollowed user $toUnfollowUsername.")
        return ResponseEntity.ok().build()
    }

    @GetMapping("/{username}/followers")
    @Operation(
        operationId = "getFollowers",
        tags = ["User"],
        summary = "Get followers of a user.",
        description = "Retrieve a paginated list of followers for the specified user.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved the list of followers.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = UserDto::class)
                )]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Not found - User not found.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        value = """{"message":"User not found","status":404,"errorCode":"USER_NOT_FOUND","timestamp":"2026-01-01T12:00:00Z","path":"/rest/v1/users/johndoe/followers"}"""
                    )]
                )]
            )
        ]
    )
    fun getFollowers(
        @PathVariable username: String,
        @RequestParam(defaultValue = "0") offset: Int,
        @RequestParam(defaultValue = "20") limit: Int
    ): ResponseEntity<List<UserDto>> {
        return ResponseEntity.ok(userService.getFollowers(username, offset, limit))
    }

    @GetMapping("/{username}/following")
    @Operation(
        operationId = "getFollowing",
        tags = ["User"],
        summary = "Get following of a user.",
        description = "Retrieve a paginated list of users that the specified user is following.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved the list of following users.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = UserDto::class)
                )]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Not found - User not found.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        value = """{"message":"User not found","status":404,"errorCode":"USER_NOT_FOUND","timestamp":"2026-01-01T12:00:00Z","path":"/rest/v1/users/johndoe/following"}"""
                    )]
                )]
            )
        ]
    )
    fun getFollowing(
        @PathVariable username: String,
        @RequestParam(defaultValue = "0") offset: Int,
        @RequestParam(defaultValue = "20") limit: Int
    ): ResponseEntity<List<UserDto>> {
        return ResponseEntity.ok(userService.getFollowing(username, offset, limit))
    }

    @GetMapping("/{username}/followers/count")
    @Operation(
        operationId = "countFollowers",
        tags = ["User"],
        summary = "Count followers of a user.",
        description = "Retrieve the total number of followers for the specified user.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved the count of followers.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = Int::class)
                )]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Not found - User not found.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        value = """{"message":"User not found","status":404,"errorCode":"USER_NOT_FOUND","timestamp":"2026-01-01T12:00:00Z","path":"/rest/v1/users/johndoe/followers/count"}"""
                    )]
                )]
            )
        ]
    )
    fun countFollowers(@PathVariable username: String): ResponseEntity<Int> {
        return ResponseEntity.ok(userService.countFollowers(username))
    }

    @GetMapping("/{username}/following/count")
    @Operation(
        operationId = "countFollowing",
        tags = ["User"],
        summary = "Count following of a user.",
        description = "Retrieve the total number of users that the specified user is following.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved the count of following users.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = Int::class)
                )]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Not found - User not found.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        value = """{"message":"User not found","status":404,"errorCode":"USER_NOT_FOUND","timestamp":"2026-01-01T12:00:00Z","path":"/rest/v1/users/johndoe/following/count"}"""
                    )]
                )]
            )
        ]
    )
    fun countFollowing(@PathVariable username: String): ResponseEntity<Int> {
        return ResponseEntity.ok(userService.countFollowing(username))
    }

    @PostMapping("/block/{blocked}")
    @Operation(
        operationId = "blockUser",
        tags = ["User"],
        summary = "Block a user.",
        description = "Make the user block another user.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully blocked the user."
            ),
            ApiResponse(
                responseCode = "403",
                description = "Forbidden - Unauthorized to follow user.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        value = """{"message":"Unauthorized","status":403,"errorCode":"UNAUTHORIZED","timestamp":"2026-01-01T12:00:00Z","path":"/rest/v1/users/block/johndoe"}"""
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Not found - User not found.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        value = """{"message":"User not found","status":404,"errorCode":"USER_NOT_FOUND","timestamp":"2026-01-01T12:00:00Z","path":"/rest/v1/users/block/johndoe"}"""
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Bad Request - Invalid block operation.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        value = """{"message":"Invalid user data: Cannot block yourself","status":400,"errorCode":"INVALID_USER_DATA","timestamp":"2026-01-01T12:00:00Z","path":"/rest/v1/users/block/johndoe"}"""
                    )]
                )]
            )
        ]
    )
    fun blockUser(
        @PathVariable blocked: String
    ): ResponseEntity<Unit> {
        userService.blockUser(blocked)
        logger.info("User $blocked blocked successfully.")
        return ResponseEntity.ok().build()
    }


    @PostMapping("/unblock/{blocked}")
    @Operation(
        operationId = "unblockUser",
        tags = ["User"],
        summary = "Unblock a user.",
        description = "Make the user unblock another user.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully unblocked the user."
            ),
            ApiResponse(
                responseCode = "403",
                description = "Forbidden - Unauthorized to follow user.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        value = """{"message":"Unauthorized","status":403,"errorCode":"UNAUTHORIZED","timestamp":"2026-01-01T12:00:00Z","path":"/rest/v1/users/unblock/johndoe"}"""
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Not found - User not found.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        value = """{"message":"User not found","status":404,"errorCode":"USER_NOT_FOUND","timestamp":"2026-01-01T12:00:00Z","path":"/rest/v1/users/unblock/johndoe"}"""
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Bad Request - Invalid unblock operation.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        value = """{"message":"Invalid user data: Cannot unblock yourself","status":400,"errorCode":"INVALID_USER_DATA","timestamp":"2026-01-01T12:00:00Z","path":"/rest/v1/users/unblock/johndoe"}"""
                    )]
                )]
            )
        ]
    )
    fun unblockUser(
        @PathVariable blocked: String
    ): ResponseEntity<Unit> {
        userService.unblockUser(blocked)
        logger.info("User $blocked unblocked successfully.")
        return ResponseEntity.ok().build()
    }

    @GetMapping("/blocked")
    @Operation(
        operationId = "getBlockedUsers",
        tags = ["User"],
        summary = "Get blocked users of a user.",
        description = "Retrieve a paginated list of users that the specified user has blocked.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved the list of blocked users.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = UserDto::class)
                )]
            ),
            ApiResponse(
                responseCode = "403",
                description = "Forbidden - Unauthorized to follow user.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        value = """{"message":"Unauthorized","status":403,"errorCode":"UNAUTHORIZED","timestamp":"2026-01-01T12:00:00Z","path":"/rest/v1/users/blocked"}"""
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Not found - User not found.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        value = """{"message":"User not found","status":404,"errorCode":"USER_NOT_FOUND","timestamp":"2026-01-01T12:00:00Z","path":"/rest/v1/users/blocked"}"""
                    )]
                )]
            )
        ]
    )
    fun getBlockedUsers(
        @RequestParam(defaultValue = "0") offset: Int,
        @RequestParam(defaultValue = "20") limit: Int
    ): ResponseEntity<List<UserDto>> {
        return ResponseEntity.ok(userService.getBlockedUsers(offset, limit))
    }

    @GetMapping("/blocked/count")
    @Operation(
        operationId = "countBlockedUsers",
        tags = ["User"],
        summary = "Count blocked users of a user.",
        description = "Retrieve the total number of users that the specified user has blocked.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved the count of blocked users.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = Int::class)
                )]
            ),
            ApiResponse(
                responseCode = "403",
                description = "Forbidden - Unauthorized to follow user.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        value = """{"message":"Unauthorized","status":403,"errorCode":"UNAUTHORIZED","timestamp":"2026-01-01T12:00:00Z","path":"/rest/v1/users/blocked/count"}"""
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Not found - User not found.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        value = """{"message":"User not found","status":404,"errorCode":"USER_NOT_FOUND","timestamp":"2026-01-01T12:00:00Z","path":"/rest/v1/users/blocked/count"}"""
                    )]
                )]
            )
        ]
    )
    fun countBlockedUsers(): ResponseEntity<Int> {
        return ResponseEntity.ok(userService.countBlockedUsers())
    }

    @GetMapping("/exists")
    @Operation(
        operationId = "checkUserExists",
        tags = ["User"],
        summary = "Check if a user exists by username or email.",
        description = "Check if a user exists by providing either a username or an email.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully checked user existence.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = Boolean::class)
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Bad Request - Neither username nor email provided.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        value = """{"message":"Invalid user data: Either username or email must be provided","status":400,"errorCode":"INVALID_USER_DATA","timestamp":"2026-01-01T12:00:00Z","path":"/rest/v1/users/exists"}"""
                    )]
                )]
            )
        ]
    )
    fun checkIfUserExists(
        @RequestParam(required = false) username: String?,
        @RequestParam(required = false) email: String?
    ): ResponseEntity<Boolean> {
        if (username.isNullOrBlank() && email.isNullOrBlank()) {
            logger.info("Neither username nor email provided for existence check.")
            throw UserExceptions.InvalidUserDataException("Either username or email must be provided")
        }

        val exists = userService.checkIfUserExists(username, email)
        return ResponseEntity.ok(exists)
    }

    @GetMapping("/search")
    @Operation(
        operationId = "searchUsersByUsername",
        tags = ["User"],
        summary = "Search users by username.",
        description = "Search users by username with partial match (case-insensitive). Returns paginated results.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved the list of users.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = UserDto::class)
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Bad Request - Invalid query parameter.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        value = """{"message":"Invalid user data: query parameter cannot be empty","status":400,"errorCode":"INVALID_USER_DATA","timestamp":"2026-01-01T12:00:00Z","path":"/rest/v1/users/search"}"""
                    )]
                )]
            )
        ]
    )
    fun searchUsersByUsername(
        @RequestParam query: String,
        @RequestParam(defaultValue = "0") offset: Int,
        @RequestParam(defaultValue = "20") limit: Int
    ): ResponseEntity<List<UserDto>> {
        if (query.isBlank()) {
            return ResponseEntity.ok(emptyList())
        }

        val users = userService.searchUsersByUsername(query, offset, limit)
        logger.info("Found ${users.size} users matching query '$query'")
        return ResponseEntity.ok(users)
    }

    // TODO: delete users da se tudi iz collection izbriše (da tm k se collectioni prkazujejo, ne prikaže teh k so izbrisani, oke tole samo če bo čas)
}