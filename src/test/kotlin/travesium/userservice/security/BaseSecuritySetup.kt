package travesium.userservice.security

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

/**
 * @author Maja Razinger
 */
@ExtendWith(MockitoExtension::class)
abstract class BaseSecuritySetup() {


    protected val token = "mockToken"
    protected val firebaseId = "firebase123"
    protected val email = "test@example.com"

    @BeforeEach
    fun baseSetup() {
        SecurityContextHolder.clearContext()
        setupDefaultAuth()
    }

    protected fun setupDefaultAuth() {
        val auth = UsernamePasswordAuthenticationToken("principal", token)
        SecurityContextHolder.getContext().authentication = auth
    }

}