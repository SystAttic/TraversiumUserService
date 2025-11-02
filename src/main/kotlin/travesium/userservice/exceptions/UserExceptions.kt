package travesium.userservice.exceptions

/**
 * @author Maja Razinger
 */
class UserExceptions {
    class UserNotFoundException : RuntimeException("User not found")

    class UserAlreadyExistsException(message: String) : RuntimeException(message)

    class InvalidUserDataException(message: String) : RuntimeException("Invalid user data: $message")

    class UnauthorizedException(message: String) : RuntimeException(message)

    class RemoteServiceException(serviceName: String) : RuntimeException("Error communicating with remote service: $serviceName")
}