package travesium.userservice.security

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseToken
import com.google.firebase.auth.UserRecord
import org.mockito.Answers
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import travesium.userservice.service.FirebaseService

/**
 * @author Maja Razinger
 */
@TestConfiguration
class MockFirebaseConfig {

    private val tokenMap = mutableMapOf<String, Pair<String, String>>() // token -> (uid, email)

    fun setTokenData(token: String, uid: String, email: String) {
        tokenMap[token] = uid to email
    }

    @Bean
    @Primary
    fun initializeFirebase(): FirebaseApp {
        return mock(FirebaseApp::class.java)
    }

    @Bean
    @Primary
    fun firebaseAuth(): FirebaseAuth {
        val mockAuth = mock(FirebaseAuth::class.java, Answers.RETURNS_DEEP_STUBS)
        val mockToken = mock(FirebaseToken::class.java)
        val mockUserRecord = mock(UserRecord::class.java)

        `when`(mockToken.uid).thenReturn("firebase123")
        `when`(mockToken.email).thenReturn("test@example.com")
        `when`(mockToken.tenantId).thenReturn("default")

        `when`(mockUserRecord.uid).thenReturn("firebase123")
        `when`(mockUserRecord.email).thenReturn("test@example.com")
        `when`(mockUserRecord.photoUrl).thenReturn(null)

        `when`(mockAuth.verifyIdToken(any())).thenReturn(mockToken)
        `when`(mockAuth.getUser(any())).thenReturn(mockUserRecord)

        `when`(mockAuth.tenantManager.getAuthForTenant(any()).getUser(any())).thenReturn(mockUserRecord)

        return mockAuth
    }

    @Bean
    @Primary
    fun firebaseService(firebaseAuth: FirebaseAuth): FirebaseService {
        return object : FirebaseService(firebaseAuth) {
            override fun extractUidFromToken(token: String): String {
                return tokenMap[token]?.first ?: "firebase123"
            }

            override fun extractEmailFromToken(token: String): String {
                return tokenMap[token]?.second ?: "test@example.com"
            }
        }
    }
}
