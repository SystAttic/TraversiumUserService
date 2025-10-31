package travesium.userservice

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.graphql.test.tester.GraphQlTester
import org.springframework.graphql.test.tester.HttpGraphQlTester
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import travesium.userservice.dto.UserDto
import travesium.userservice.service.UserService
import kotlin.test.Test

/**
 * @author Maja Razinger
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserQueryControllerTest @Autowired constructor(
    @Autowired private val userService: UserService,
) {

    @LocalServerPort
    private var port: Int = 0

    private lateinit var graphQlTester: GraphQlTester

    @BeforeAll
    fun setup() {
        val client = WebTestClient.bindToServer()
            .baseUrl("http://localhost:$port/graphql")
            .build()

        graphQlTester = HttpGraphQlTester.create(client)

        userService.createUser(
            UserDto(
                username = "janeDoe",
                email = "jane@example.com",
                description = "Test user",
                displayName = "John Doe",
                avatarPhotoReference = "",
                coverPhotoReference = "",
                deleted = false
            )
        )

    }

    @AfterAll
    fun tearDown() {
        userService.deleteUserByUsername("janeDoe")
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
              user(email: "jane@example.com") {
                userId
                username
                email
                displayName
              }
            }
           """

        graphQlTester.document(query)
            .execute()
            .path("user.email").entity(String::class.java).isEqualTo("jane@example.com")

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