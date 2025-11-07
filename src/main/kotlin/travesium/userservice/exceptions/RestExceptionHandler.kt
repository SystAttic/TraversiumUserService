package travesium.userservice.exceptions

import org.apache.logging.log4j.kotlin.Logging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import travesium.userservice.dto.ErrorResponse
import java.time.OffsetDateTime

/**
 * @author Maja Razinger
 */
@RestControllerAdvice
class RestExceptionHandler : Logging {

    @ExceptionHandler(UserExceptions.UserNotFoundException::class)
    fun handleUserNotFoundException(
        ex: UserExceptions.UserNotFoundException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.info("User not found: ${ex.message}")
        val errorResponse = ErrorResponse(
            message = ex.message ?: "User not found",
            status = HttpStatus.NOT_FOUND.value(),
            timestamp = OffsetDateTime.now(),
            path = request.getDescription(false).removePrefix("uri=")
        )
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse)
    }

    @ExceptionHandler(UserExceptions.UserAlreadyExistsException::class)
    fun handleUserAlreadyExistsException(
        ex: UserExceptions.UserAlreadyExistsException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.info("User already exists: ${ex.message}")
        val errorResponse = ErrorResponse(
            message = ex.message!!,
            status = HttpStatus.CONFLICT.value(),
            timestamp = OffsetDateTime.now(),
            path = request.getDescription(false).removePrefix("uri=")
        )
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse)
    }

    @ExceptionHandler(UserExceptions.InvalidUserDataException::class)
    fun handleInvalidUserDataException(
        ex: UserExceptions.InvalidUserDataException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.info("Invalid user data: ${ex.message}")
        val errorResponse = ErrorResponse(
            message = ex.message ?: "Invalid user data provided",
            status = HttpStatus.BAD_REQUEST.value(),
            timestamp = OffsetDateTime.now(),
            path = request.getDescription(false).removePrefix("uri=")
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }

    @ExceptionHandler(UserExceptions.UnauthorizedException::class)
    fun handleUnauthorizedException(
        ex: UserExceptions.UnauthorizedException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.info("Unauthorized: ${ex.message}")
        val errorResponse = ErrorResponse(
            message = ex.message ?: "Unauthorized",
            status = HttpStatus.FORBIDDEN.value(),
            timestamp = OffsetDateTime.now(),
            path = request.getDescription(false).removePrefix("uri=")
        )
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse)
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        ex: Exception,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected error: ${ex.message}", ex)
        val errorResponse = ErrorResponse(
            message = "An unexpected error occurred: ${ex.message}",
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            timestamp = OffsetDateTime.now(),
            path = request.getDescription(false).removePrefix("uri=")
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
    }
}
