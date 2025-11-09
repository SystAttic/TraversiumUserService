package travesium.userservice.security

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserRecord
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.util.AntPathMatcher
import org.springframework.web.filter.OncePerRequestFilter
import travesium.userservice.service.FirebaseService
import travesium.userservice.service.TenantService

/**
 * @author Maja Razinger
 */
@Component
class FirebaseAuthenticationFilter(
    private val firebaseService: FirebaseService,
    private val tenantService: TenantService,
    private val firebaseAuth: FirebaseAuth,
) : OncePerRequestFilter(){

    private val pathMatcher = AntPathMatcher()

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain) {
        try {
            val authHeader = request.getHeader("Authorization")
            if (authHeader == null) {
                response.status = HttpServletResponse.SC_UNAUTHORIZED
                return
            }
            val token = authHeader.removePrefix("Bearer ").trim()

            val decodedToken = firebaseAuth.verifyIdToken(token)
            val uid = decodedToken.uid

            SecurityContextHolder.getContext().authentication = TraversiumAuthentication(
                userRecordToPrincipal(firebaseAuth.getUser(uid)),
                null,
                emptyList(),
                token
            )

            val tenantId = firebaseService.extractTenantIdFromToken(token)
            tenantService.setCurrentTenant(tenantId)

            filterChain.doFilter(request, response)
        } catch (ex: Exception) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, ex.message)
        } finally {
            tenantService.clear()
        }
    }

    private fun userRecordToPrincipal(userRecord: UserRecord): TraversiumPrincipal = TraversiumPrincipal(
        userRecord.uid,
        userRecord.email,
        //PersonalDataType.GOOGLE,
        userRecord.photoUrl
    )

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI.removePrefix(request.contextPath)

        val exactPaths = setOf(
            "/rest/v1/users/exists",
            "/swagger-ui.html"
        )
        val prefixPaths = listOf(
            "/swagger-ui",
            "/v3/api-docs",
            "/swagger-resources"
        )

        return path in exactPaths || prefixPaths.any { path.startsWith(it) }
    }
}