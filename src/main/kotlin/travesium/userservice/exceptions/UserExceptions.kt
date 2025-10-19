package travesium.userservice.exceptions

/**
 * @author Maja Razinger
 */
class UserExceptions {
    class UserNotFoundException : RuntimeException("User not found")

    class UserAlreadyExistsException() : RuntimeException("User already exists")
    
}