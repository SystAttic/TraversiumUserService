package travesium.userservice.service

import com.google.firebase.auth.FirebaseAuth
import org.springframework.stereotype.Service

/**
 * @author Maja Razinger
 */
@Service
//@Profile("auth")
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
}