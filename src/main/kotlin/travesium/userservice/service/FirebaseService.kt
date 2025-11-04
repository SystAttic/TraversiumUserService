package travesium.userservice.service

import com.google.firebase.auth.FirebaseAuth
import org.springframework.stereotype.Service

/**
 * @author Maja Razinger
 */
@Service
class FirebaseService(
    private val firebaseAuth: FirebaseAuth
) {

    fun extractTenantIdFromToken(token: String): String? {
        val decodedToken = firebaseAuth.verifyIdToken(token)
        return decodedToken.tenantId ?: "default"
    }

    fun extractUidFromToken(token: String): String {
        val decodedToken = firebaseAuth.verifyIdToken(token)
        return decodedToken.uid
    }

    fun extractEmailFromToken(token: String): String? {
        val decodedToken = firebaseAuth.verifyIdToken(token)
        return decodedToken.email
    }

    fun extractUsernameFromToken(token: String): String? {
        val decodedToken = firebaseAuth.verifyIdToken(token)
        return decodedToken.name
    }
}