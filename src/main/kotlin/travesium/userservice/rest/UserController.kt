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

    @PostMapping("/create")
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
                description = "Conflict - User with the same uid, username or email already exists."
            )
        ]
    )
    fun createUser(@RequestBody userDto: UserDto): ResponseEntity<UserDto> {
        return try {
            val savedUser = userService.createUser(userDto)
            logger.info("User with UID ${savedUser.uid} created or updated")
            ResponseEntity.ok(savedUser)
        } catch (_: UserExceptions.UserAlreadyExistsException) {
            logger.info("User with username ${userDto.username} or email ${userDto.email} already exists")
            ResponseEntity.status(HttpStatus.CONFLICT).body(null)
        } catch (_: Exception) {
            ResponseEntity.badRequest().build()
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
    fun getUserByUsername(@PathVariable username: String): ResponseEntity<UserDto> {
        return try {
            val user = userService.getUserByUsername(username)
            logger.info("User with the username ${user.username} found.")
            ResponseEntity.ok(user)
        } catch (_: UserExceptions.UserNotFoundException){
            logger.info("User with the username $username not found.")
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(null)
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
    fun getUserByEmail(@PathVariable email: String): ResponseEntity<UserDto> {
        return try {
            val user = userService.getUserByEmail(email)
            logger.info("User with the email ${user.email} found.")
            ResponseEntity.ok(user)
        } catch (_: UserExceptions.UserNotFoundException){
            logger.info("User with the email $email not found.")
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(null)
        }
    }

    // TODO: add fhir base deletion in future
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
    fun deleteUserByUsername(@PathVariable username: String): ResponseEntity<Void> {
        return try {
            userService.deleteUserByUsername(username)
            logger.info("User with the username $username deleted.")
            ResponseEntity.ok().build()
        } catch (_: UserExceptions.UserNotFoundException){
            logger.info("User with the username $username not found.")
            ResponseEntity.status(HttpStatus.NOT_FOUND).build()
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
    fun deleteUserByEmail(@PathVariable email: String): ResponseEntity<Void> {
        return try {
            userService.deleteUserByEmail(email)
            logger.info("User with the email $email deleted.")
            ResponseEntity.ok().build()
        } catch (_: UserExceptions.UserNotFoundException){
            logger.info("User with the email $email not found.")
            ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        }
    }

    // TODO: add fhir base update in future
    @PutMapping("/username/{username}")
    @Operation(
        operationId = "updateUserByUsername",
        tags = ["User"],
        summary = "Update a user by username.",
        description = "Update a user by username.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully updated the user by username.",
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
    fun updateUserByUsername(@PathVariable username: String, @RequestBody userDto: UserDto): ResponseEntity<UserDto> {
        return try {
            val updatedUser = userService.updateUser(userDto)
            logger.info("User with the username $username updated.")
            ResponseEntity.ok(updatedUser)
        } catch (_: UserExceptions.UserNotFoundException){
            logger.info("User with the username $username not found.")
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(null)
        }
    }

    @PutMapping("/email/{email}")
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
    fun updateUserByEmail(@PathVariable email: String,  @RequestBody userDto: UserDto): ResponseEntity<UserDto> {
        return try {
            val updatedUser = userService.updateUser(userDto)
            logger.info("User with the email $email updated.")
            ResponseEntity.ok(updatedUser)
        } catch (_: UserExceptions.UserNotFoundException) {
            logger.info("User with the email $email not found.")
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(null)
        }
    }

    // TODO: add follow, add block, remove follow, remove block, get all followers, get all following, get all blocked users
    // TODO: add firebase
    // TODO: add cors configuration
}