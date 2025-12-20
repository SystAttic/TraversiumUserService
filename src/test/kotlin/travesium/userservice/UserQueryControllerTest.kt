package travesium.userservice

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.graphql.test.tester.GraphQlTester
import org.springframework.graphql.test.tester.HttpGraphQlTester
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.transaction.annotation.Transactional
import travesium.userservice.dto.UserDto
import travesium.userservice.security.BaseSecuritySetup
import travesium.userservice.security.MockFirebaseConfig
import travesium.userservice.security.MockGrpcConfig
import travesium.userservice.security.TestMultitenancyConfig
import travesium.userservice.service.UserService
import kotlin.test.Test

/**
 * @author Maja Razinger
 */
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [UserServiceApplication::class]
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SpringExtension::class)
@Transactional
@DirtiesContext
@ContextConfiguration(classes = [MockFirebaseConfig::class, TestMultitenancyConfig::class, MockGrpcConfig::class])
class UserQueryControllerTest() : BaseSecuritySetup() {

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var mockFirebaseConfig: MockFirebaseConfig

    @LocalServerPort
    private var port: Int = 0

    private lateinit var graphQlTester: GraphQlTester

    @BeforeAll
    fun setup() {
        SecurityContextHolder.clearContext()
        setupDefaultAuth()

        mockFirebaseConfig.setTokenData(token, firebaseId, email)

        val client = WebTestClient.bindToServer()
            .defaultHeader("Authorization", "Bearer $token")
            .baseUrl("http://localhost:$port/graphql")
            .build()

        graphQlTester = HttpGraphQlTester.create(client)

        userService.createUser(
            UserDto(
                username = "janeDoe",
                email = email,
                description = "Test user",
                displayName = "John Doe",
                avatarPhotoReference = "",
                coverPhotoReference = "",
                deleted = false,
                firebaseId = firebaseId
            )
        )

    }

    @Test
    fun returnUserByUsername() {
        val query = """
            query {
              user(username: "janeDoe") {
                userId
                username
                email
                displayName
              }
            }
        """

        graphQlTester.document(query)
            .execute()
            .path("user.username").entity(String::class.java).isEqualTo("janeDoe")
    }

    @Test
    fun returnUserByEmail() {
        val query = """
            query {
              user(email: "$email") {
                userId
                username
                email
                displayName
              }
            }
           """

        graphQlTester.document(query)
            .execute()
            .path("user.email").entity(String::class.java).isEqualTo(email)

    }

    @Test
    fun failToReturnNonexistentUser() {
        val query = """
            query {
              user(username: "nonexistent") {
                userId
                username
                email
                displayName
              }
            }
        """

        graphQlTester.document(query)
            .execute()
            .errors()
            .satisfy { errors ->
                assert(errors.any { it.message!!.contains("User not found") })
            }
    }
}