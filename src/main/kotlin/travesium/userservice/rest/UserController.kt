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

    @GetMapping("/username/{username}")
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
    fun getUserByUsername(@PathVariable username: String): ResponseEntity<Any> {
        return try {
            val user = userService.getUserByUsername(username)
            logger.info("User with the username ${user.username} found.")
            ResponseEntity.ok(user)
        } catch (_: UserExceptions.UserNotFoundException){
            logger.info("User with the username $username not found.")
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to "User not found"))
        }
    }

    @GetMapping("/email/{email}")
    @Operation(
        operationId = "getUserByEmail",
        tags = ["User"],
        summary = "Get a user by email.",
        description = "Get a user by email.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved the user by email.",
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
    fun getUserByEmail(@PathVariable email: String): ResponseEntity<Any> {
        return try {
            val user = userService.getUserByEmail(email)
            logger.info("User with the email ${user.email} found.")
            ResponseEntity.ok(user)
        } catch (_: UserExceptions.UserNotFoundException){
            logger.info("User with the email $email not found.")
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                mapOf("message" to "User not found")
            )
        }
    }

    @DeleteMapping("/username/{username}")
    @Operation(
        operationId = "deleteUserByUsername",
        tags = ["User"],
        summary = "Delete a user by username.",
        description = "Delete a user by username.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully deleted the user by username."
            ),
            ApiResponse(
                responseCode = "404",
                description = "Not found - User not found."
            )
        ]
    )
    fun deleteUserByUsername(@PathVariable username: String): ResponseEntity<Any> {
        return try {
            userService.deleteUserByUsername(username)
            logger.info("User with the username $username deleted.")
            ResponseEntity.ok().body(
                mapOf("message" to "User deleted successfully")
            )
        } catch (_: UserExceptions.UserNotFoundException){
            logger.info("User with the username $username not found.")
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                mapOf("message" to "User not found")
            )
        }
    }

    @DeleteMapping("/email/{email}")
    @Operation(
        operationId = "deleteUserByEmail",
        tags = ["User"],
        summary = "Delete a user by email.",
        description = "Delete a user by email.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully deleted the user by email."
            ),
            ApiResponse(
                responseCode = "404",
                description = "Not found - User not found."
            )
        ]
    )
    fun deleteUserByEmail(@PathVariable email: String): ResponseEntity<Any> {
        return try {
            userService.deleteUserByEmail(email)
            logger.info("User with the email $email deleted.")
            ResponseEntity.ok().body(
                mapOf("message" to "User deleted successfully")
            )
        } catch (_: UserExceptions.UserNotFoundException){
            logger.info("User with the email $email not found.")
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

    @PostMapping("/{username}/follow/{toFollowUsername}")
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
        @PathVariable username: String,
        @PathVariable toFollowUsername: String
    ): ResponseEntity<Any> {
        return try {
            userService.followUser(username, toFollowUsername)
            logger.info("User $username followed user $toFollowUsername.")
            ResponseEntity.ok().body(mapOf("message" to "Successfully followed user"))
        } catch (e: UserExceptions.UserNotFoundException) {
            logger.info("User not found while trying to follow: ${e.message}")
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to "User not found"))
        } catch (e: UserExceptions.InvalidUserDataException) {
            logger.info("Invalid follow operation: ${e.message}")
            ResponseEntity.badRequest().body(mapOf("message" to "Invalid follow operation: ${e.message}"))
        }
    }

    @PostMapping("/{username}/unfollow/{toUnfollowUsername}")
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
        @PathVariable username: String,
        @PathVariable toUnfollowUsername: String
    ): ResponseEntity<Any> {
        return try {
            userService.unfollowUser(username, toUnfollowUsername)
            logger.info("User $username unfollowed user $toUnfollowUsername.")
            ResponseEntity.ok().body(mapOf("message" to "Successfully unfollowed user"))
        } catch (e: UserExceptions.UserNotFoundException) {
            logger.info("User not found while trying to unfollow: ${e.message}")
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to "User not found"))
        } catch (e: UserExceptions.InvalidUserDataException) {
            logger.info("Invalid unfollow operation: ${e.message}")
            ResponseEntity.badRequest().body(mapOf("message" to "Invalid unfollow operation: ${e.message}"))
        }
    }

    // TODO: pagination?
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
        @PathVariable username: String
    ): ResponseEntity<List<UserDto>> {
        try {
            return ResponseEntity.ok(userService.getFollowers(username))
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
        @PathVariable username: String
    ): ResponseEntity<List<UserDto>> {
        try {
            return ResponseEntity.ok(userService.getFollowing(username))
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

    @PostMapping("/{blocker}/block/{blocked}")
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
        @PathVariable blocker: String,
        @PathVariable blocked: String
    ): ResponseEntity<String> {
        try {
            userService.blockUser(blocker, blocked)
            return ResponseEntity.ok("$blocker blocked $blocked")
        } catch (e: UserExceptions.UserNotFoundException) {
            logger.info("User not found while trying to block: ${e.message}")
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found")
        } catch (e: UserExceptions.InvalidUserDataException) {
            logger.info("Invalid block operation: ${e.message}")
            return ResponseEntity.badRequest().body("Invalid block operation: ${e.message}")
        }
    }


    @PostMapping("/{blocker}/unblock/{blocked}")
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
        @PathVariable blocker: String,
        @PathVariable blocked: String
    ): ResponseEntity<String> {
        try {
            userService.unblockUser(blocker, blocked)
            return ResponseEntity.ok("$blocker unblocked $blocked")
        } catch (e: UserExceptions.UserNotFoundException) {
            logger.info("User not found while trying to unblock: ${e.message}")
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found")
        } catch (e: UserExceptions.InvalidUserDataException) {
            logger.info("Invalid unblock operation: ${e.message}")
            return ResponseEntity.badRequest().body("Invalid unblock operation: ${e.message}")
        }
    }

    @GetMapping("/{username}/blocked")
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
                responseCode = "404",
                description = "Not found - User not found."
            )
        ]
    )
    fun getBlockedUsers(
        @PathVariable username: String
    ): ResponseEntity<List<UserDto>> {
        try {
            return ResponseEntity.ok(userService.getBlockedUsers(username))
        } catch (e: UserExceptions.UserNotFoundException) {
            logger.info("User with the username $username not found.")
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(emptyList())
        }
    }

    @GetMapping("/{username}/blocked/count")
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
                responseCode = "404",
                description = "Not found - User not found."
            )
        ]
    )
    fun countBlockedUsers(@PathVariable username: String): ResponseEntity<Int> {
        try {
            return ResponseEntity.ok(userService.countBlockedUsers(username))
        } catch (e: UserExceptions.UserNotFoundException) {
            logger.info("User with the username $username not found.")
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(0)
        }
    }

    // TODO: add firebase
    // TODO: delete users da se tudi iz collection izbriše (da tm k se collectioni prkazujejo, ne prikaže teh k so izbrisani, oke tole samo če bo čas)
    // TODO: when user is blocked make a request to trip service to remove user's trips from feed of the blocker
}