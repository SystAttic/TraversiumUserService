package travesium.userservice.service

import com.google.firebase.auth.FirebaseAuth
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.stereotype.Service

/**
 * @author Maja Razinger
 */
@Service
class FirebaseService(
    private val firebaseAuth: FirebaseAuth
) : Logging {

    fun extractUidFromToken(token: String): String {
        val decodedToken = firebaseAuth.verifyIdToken(token)
        return decodedToken.uid
    }

    fun extractEmailFromToken(token: String): String? {
        val decodedToken = firebaseAuth.verifyIdToken(token)
        return decodedToken.email
    }
}