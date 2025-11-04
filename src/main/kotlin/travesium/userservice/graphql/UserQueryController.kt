package travesium.userservice.graphql

import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller
import travesium.userservice.dto.UserDto
import travesium.userservice.service.UserService

/**
 * @author Maja Razinger
 */
@Controller
class UserQueryController(
    private val userService: UserService
)  {

    @QueryMapping
    fun user(
        @Argument username: String?,
        @Argument email: String?): UserDto? {

        if (username == null && email == null) {
            throw IllegalArgumentException("Must provide username or email")
        }

        return userService.getUser(username, email)
    }
}