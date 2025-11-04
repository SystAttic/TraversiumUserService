package travesium.userservice.rest


import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import travesium.userservice.dto.UserDto
import travesium.userservice.exceptions.UserExceptions
import travesium.userservice.service.UserService


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
                description = "Bad Request - Invalid user data provided."
            ),
            ApiResponse(
                responseCode = "403",
                description = "Forbidden - Unauthorized to create user."
            ),
            ApiResponse(
                responseCode = "409",
                description = "Conflict - User with the same username or email already exists."
            )
        ]
    )
    fun createUser(@RequestBody userDto: UserDto): ResponseEntity<Any> {
        return try {
            val savedUser = userService.createUser(userDto)
            logger.info("User with UID ${savedUser.userId} created or updated")
            ResponseEntity.ok(savedUser)
        } catch (_: UserExceptions.UserAlreadyExistsException) {
            logger.info("User with username ${userDto.username} or email ${userDto.email} already exists")
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(mapOf("message" to "User with this username or email already exists"))
        } catch (_: Exception) {
            logger.info("Invalid user data provided for user creation")
            ResponseEntity.badRequest().body(mapOf("message" to "Invalid user data provided (email, username must not be null, userId must be null)"))
        }
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
                description = "Not found - User not found."
            )
        ]
    )
    fun getUser(
        @RequestParam username: String?,
        @RequestParam email: String?,
    ): ResponseEntity<Any> {
        if (username.isNullOrBlank() && email.isNullOrBlank()) {
            logger.info("Neither username nor email provided for existence check.")
            return ResponseEntity.badRequest().body(mapOf("message" to "Either username or email must be provided"))
        }

        return try {
            val user = userService.getUser(username, email)
            logger.info("User with the username  ${username ?: "N/A"}} or email  ${email ?: "N/A"} found.")
            ResponseEntity.ok(user)
        } catch (_: UserExceptions.UserNotFoundException){
            logger.info("User with the username  ${username ?: "N/A"} or email  ${email ?: "N/A"} not found.")
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to "User not found"))
        }
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
                description = "Forbidden - Unauthorized to delete user."
            ),
            ApiResponse(
                responseCode = "404",
                description = "Not found - User not found."
            )
        ]
    )
    fun deleteUserByUsername(): ResponseEntity<Any> {
        return try {
            userService.deleteUser()
            logger.info("User  deleted.")
            ResponseEntity.ok().body(
                mapOf("message" to "User deleted successfully")
            )
        } catch (_: UserExceptions.UserNotFoundException){
            logger.info("User not found.")
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                mapOf("message" to "User not found")
            )
        }
    }

    @PutMapping()
    @Operation(
        operationId = "updateUserByEmail",
        tags = ["User"],
        summary = "Update a user by email.",
        description = "Update a user by email.",
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
                description = "Forbidden - Unauthorized to update user."
            ),
            ApiResponse(
                responseCode = "404",
                description = "Not found - User not found."
            )
        ]
    )
    fun updateUser(@RequestBody userDto: UserDto): ResponseEntity<Any> {
        return try {
            val updatedUser = userService.updateUser(userDto)
            logger.info("User with the username ${userDto.username} updated.")
            ResponseEntity.ok(updatedUser)
        } catch (_: UserExceptions.UserNotFoundException) {
            logger.info("User with the email ${userDto.username} not found.")
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to "User not found"))
        }
    }

    @PostMapping("/userList")
    @Operation(
        operationId = "userList",
        tags = ["User"],
        summary = "Get a list of users by usernames.",
        description = "Return users named in the list of usernames.",
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
                description = "Bad Request - Invalid list of usernames provided."
            )
        ]
    )
    fun listOfUsers(
        @RequestBody usernames: List<String>,
        @RequestParam offset: Int,
        @RequestParam limit: Int
        ): ResponseEntity<Any> {
        return try {
            val users = userService.getUsersByUsernames(usernames, offset, limit)
            logger.info("Retrieved list of users for provided usernames.")
            ResponseEntity.ok(users)
        } catch (e: Exception) {
            logger.info("Invalid list of usernames provided: ${e.message}")
            ResponseEntity.badRequest().body(mapOf("message" to "Invalid list of usernames provided"))
        }
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
                description = "Forbidden - Unauthorized to follow user."
            ),
            ApiResponse(
                responseCode = "404",
                description = "Not found - User not found."
            ),
            ApiResponse(
                responseCode = "400",
                description = "Bad Request - Invalid follow operation."
            )
        ]
    )
    fun followUser(
        @PathVariable toFollowUsername: String
    ): ResponseEntity<Any> {
        return try {
            userService.followUser(toFollowUsername)
            logger.info("Followed user $toFollowUsername.")
            ResponseEntity.ok().body(mapOf("message" to "Successfully followed user"))
        } catch (e: UserExceptions.UserNotFoundException) {
            logger.info("User not found while trying to follow: ${e.message}")
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to "User not found"))
        } catch (e: UserExceptions.InvalidUserDataException) {
            logger.info("Invalid follow operation: ${e.message}")
            ResponseEntity.badRequest().body(mapOf("message" to "Invalid follow operation: ${e.message}"))
        }
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
                description = "Forbidden - Unauthorized to follow user."
            ),
            ApiResponse(
                responseCode = "404",
                description = "Not found - User not found."
            ),
            ApiResponse(
                responseCode = "400",
                description = "Bad Request - Invalid unfollow operation."
            )
        ]
    )
    fun unfollowUser(
        @PathVariable toUnfollowUsername: String
    ): ResponseEntity<Any> {
        return try {
            userService.unfollowUser(toUnfollowUsername)
            logger.info("Unfollowed user $toUnfollowUsername.")
            ResponseEntity.ok().body(mapOf("message" to "Successfully unfollowed user"))
        } catch (e: UserExceptions.UserNotFoundException) {
            logger.info("User not found while trying to unfollow: ${e.message}")
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to "User not found"))
        } catch (e: UserExceptions.InvalidUserDataException) {
            logger.info("Invalid unfollow operation: ${e.message}")
            ResponseEntity.badRequest().body(mapOf("message" to "Invalid unfollow operation: ${e.message}"))
        }
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
                description = "Not found - User not found."
            )
        ]
    )
    fun getFollowers(
        @PathVariable username: String,
        @RequestParam offset: Int,
        @RequestParam limit: Int
    ): ResponseEntity<List<UserDto>> {
        try {
            return ResponseEntity.ok(userService.getFollowers(username, offset, limit))
        } catch (e: UserExceptions.UserNotFoundException) {
            logger.info("User with the username $username not found.")
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(emptyList())
        }
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
                description = "Not found - User not found."
            )
        ]
    )
    fun getFollowing(
        @PathVariable username: String,
        @RequestParam offset: Int,
        @RequestParam limit: Int
    ): ResponseEntity<List<UserDto>> {
        try {
            return ResponseEntity.ok(userService.getFollowing(username, offset, limit))
        } catch (e: UserExceptions.UserNotFoundException) {
            logger.info("User with the username $username not found.")
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(emptyList())
        }
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
                description = "Not found - User not found."
            )
        ]
    )
    fun countFollowers(@PathVariable username: String): ResponseEntity<Int> {
        try {
            return ResponseEntity.ok(userService.countFollowers(username))
        } catch (e: UserExceptions.UserNotFoundException) {
            logger.info("User with the username $username not found.")
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(0)
        }
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
                description = "Not found - User not found."
            )
        ]
    )
    fun countFollowing(@PathVariable username: String): ResponseEntity<Int> {
        try {
            return ResponseEntity.ok(userService.countFollowing(username))
        } catch (e: UserExceptions.UserNotFoundException) {
            logger.info("User with the username $username not found.")
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(0)
        }
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
                description = "Forbidden - Unauthorized to follow user."
            ),
            ApiResponse(
                responseCode = "404",
                description = "Not found - User not found."
            ),
            ApiResponse(
                responseCode = "400",
                description = "Bad Request - Invalid block operation."
            )
        ]
    )
    fun blockUser(
        @PathVariable blocked: String
    ): ResponseEntity<String> {
        try {
            userService.blockUser(blocked)
            return ResponseEntity.ok("User blocked successfully")
        } catch (e: UserExceptions.UserNotFoundException) {
            logger.info("User not found while trying to block: ${e.message}")
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found")
        } catch (e: UserExceptions.InvalidUserDataException) {
            logger.info("Invalid block operation: ${e.message}")
            return ResponseEntity.badRequest().body("Invalid block operation: ${e.message}")
        }
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
                description = "Forbidden - Unauthorized to follow user."
            ),
            ApiResponse(
                responseCode = "404",
                description = "Not found - User not found."
            ),
            ApiResponse(
                responseCode = "400",
                description = "Bad Request - Invalid unblock operation."
            )
        ]
    )
    fun unblockUser(
        @PathVariable blocked: String
    ): ResponseEntity<String> {
        try {
            userService.unblockUser(blocked)
            return ResponseEntity.ok("User unblocked successfully")
        } catch (e: UserExceptions.UserNotFoundException) {
            logger.info("User not found while trying to unblock: ${e.message}")
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found")
        } catch (e: UserExceptions.InvalidUserDataException) {
            logger.info("Invalid unblock operation: ${e.message}")
            return ResponseEntity.badRequest().body("Invalid unblock operation: ${e.message}")
        }
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
                description = "Forbidden - Unauthorized to follow user."
            ),
            ApiResponse(
                responseCode = "404",
                description = "Not found - User not found."
            )
        ]
    )
    fun getBlockedUsers(
        @RequestParam offset: Int,
        @RequestParam limit: Int
    ): ResponseEntity<List<UserDto>> {
        try {
            return ResponseEntity.ok(userService.getBlockedUsers(offset, limit))
        } catch (e: UserExceptions.UserNotFoundException) {
            logger.info("User not found.")
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(emptyList())
        }
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
                description = "Forbidden - Unauthorized to follow user."
            ),
            ApiResponse(
                responseCode = "404",
                description = "Not found - User not found."
            )
        ]
    )
    fun countBlockedUsers(): ResponseEntity<Int> {
        try {
            return ResponseEntity.ok(userService.countBlockedUsers())
        } catch (e: UserExceptions.UserNotFoundException) {
            logger.info("User not found.")
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(0)
        }
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
                description = "Bad Request - Neither username nor email provided."
            )
        ]
    )
    fun checkIfUserExists(
        @RequestParam(required = false) username: String?,
        @RequestParam(required = false) email: String?
    ): ResponseEntity<Any> {
        if (username.isNullOrBlank() && email.isNullOrBlank()) {
            logger.info("Neither username nor email provided for existence check.")
            return ResponseEntity.badRequest().body(mapOf("message" to "Either username or email must be provided"))
        }

        val exists = userService.checkIfUserExists(username, email)
        return ResponseEntity.ok(mapOf("exists" to exists))
    }

    // TODO: delete users da se tudi iz collection izbriše (da tm k se collectioni prkazujejo, ne prikaže teh k so izbrisani, oke tole samo če bo čas)
    // TODO: when user is blocked make a request to trip service to remove user's trips from feed of the blocker
}