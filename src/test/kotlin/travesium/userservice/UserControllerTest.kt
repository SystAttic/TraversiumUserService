package travesium.userservice

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import travesium.userservice.db.model.User
import travesium.userservice.db.repository.UserRepository
import travesium.userservice.dto.UserDto
import travesium.userservice.security.BaseSecuritySetup
import travesium.userservice.security.MockFirebaseConfig
import travesium.userservice.security.MockGrpcConfig

/**
 * @author Maja Razinger
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@ContextConfiguration(classes = [MockFirebaseConfig::class, MockGrpcConfig::class])
class UserControllerTest : BaseSecuritySetup() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var mockFirebaseConfig: MockFirebaseConfig

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.clearContext()
        setupDefaultAuth()
        userRepository.deleteAll()
        mockFirebaseConfig.setTokenData(token, firebaseId, email)
    }

    @AfterEach
    fun tearDown() {
        userRepository.deleteAll()
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `POST users creates user successfully`() {
        val userDto = UserDto(username = "testuser", email = "test@example.com", firebaseId = firebaseId)

        mockMvc.perform(
            post("/rest/v1/users")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userDto))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.username").value("testuser"))
            .andExpect(jsonPath("$.email").value("test@example.com"))
    }

    @Test
    fun `POST users returns 409 when username already exists`() {
        userRepository.save(User(username = "testuser", email = "other@example.com", firebaseId = "other123"))

        val userDto = UserDto(username = "testuser", email = "test@example.com", firebaseId = firebaseId)

        mockMvc.perform(
            post("/rest/v1/users")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userDto))
        )
            .andExpect(status().isConflict)
    }

    @Test
    fun `GET users by username returns user successfully`() {
        userRepository.save(User(username = "testuser", email = "test@example.com", firebaseId = firebaseId))

        mockMvc.perform(
            get("/rest/v1/users")
                .header("Authorization", "Bearer $token")
                .param("username", "testuser")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.username").value("testuser"))
            .andExpect(jsonPath("$.email").value("test@example.com"))
    }

    @Test
    fun `GET users by email returns user successfully`() {
        userRepository.save(User(username = "testuser", email = "test@example.com", firebaseId = firebaseId))

        mockMvc.perform(
            get("/rest/v1/users")
                .header("Authorization", "Bearer $token")
                .param("email", "test@example.com")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.username").value("testuser"))
            .andExpect(jsonPath("$.email").value("test@example.com"))
    }

    @Test
    fun `GET users returns 404 when user not found`() {
        mockMvc.perform(
            get("/rest/v1/users")
                .header("Authorization", "Bearer $token")
                .param("username", "nonexistent")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `GET users returns 400 when neither username nor email provided`() {
        mockMvc.perform(
            get("/rest/v1/users")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `PUT users updates user successfully`() {
        val user = userRepository.save(User(username = "testuser", email = email, firebaseId = firebaseId, description = "old"))

        val updateDto = UserDto(userId = user.userId, description = "new")

        mockMvc.perform(
            put("/rest/v1/users")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.description").value("new"))
    }

    @Test
    fun `PUT users returns 404 when user not found`() {
        val updateDto = UserDto(userId = 999, description = "new")

        mockMvc.perform(
            put("/rest/v1/users")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto))
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `DELETE users marks user as deleted successfully`() {
        userRepository.save(User(username = "testuser", email = email, firebaseId = firebaseId))

        mockMvc.perform(
            delete("/rest/v1/users")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `DELETE users returns 404 when user not found`() {
        mockMvc.perform(
            delete("/rest/v1/users")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `POST userList returns matching users`() {
        userRepository.save(User(username = "alice", email = "alice@example.com", firebaseId = "firebase1"))
        userRepository.save(User(username = "bob", email = "bob@example.com", firebaseId = "firebase2"))

        val usernames = listOf("alice", "bob")

        mockMvc.perform(
            post("/rest/v1/users/userList")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(usernames))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
    }

    @Test
    fun `POST follow follows user successfully`() {
        userRepository.save(User(username = "alice", email = email, firebaseId = firebaseId))
        userRepository.save(User(username = "bob", email = "bob@example.com", firebaseId = "firebase2"))

        mockMvc.perform(
            post("/rest/v1/users/follow/bob")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `POST follow returns 404 when user not found`() {
        userRepository.save(User(username = "alice", email = email, firebaseId = firebaseId))

        mockMvc.perform(
            post("/rest/v1/users/follow/nonexistent")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `POST follow returns 400 when trying to follow self`() {
        userRepository.save(User(username = "alice", email = email, firebaseId = firebaseId))

        mockMvc.perform(
            post("/rest/v1/users/follow/alice")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `POST unfollow unfollows user successfully`() {
        val alice = userRepository.save(User(username = "alice", email = email, firebaseId = firebaseId))
        val bob = userRepository.save(User(username = "bob", email = "bob@example.com", firebaseId = "firebase2"))

        alice.following.add(bob)
        userRepository.save(alice)

        mockMvc.perform(
            post("/rest/v1/users/unfollow/bob")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `POST unfollow returns 404 when user not found`() {
        userRepository.save(User(username = "alice", email = email, firebaseId = firebaseId))

        mockMvc.perform(
            post("/rest/v1/users/unfollow/nonexistent")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `GET followers returns followers list`() {
        val alice = userRepository.save(User(username = "alice", email = "alice@example.com", firebaseId = "firebase1"))
        val bob = userRepository.save(User(username = "bob", email = "bob@example.com", firebaseId = "firebase2"))

        bob.following.add(alice)
        userRepository.save(bob)

        mockMvc.perform(
            get("/rest/v1/users/alice/followers")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].username").value("bob"))
    }

    @Test
    fun `GET followers handles pagination`() {
        val alice = userRepository.save(User(username = "alice", email = "alice@example.com", firebaseId = "firebase1"))
        val bob = userRepository.save(User(username = "bob", email = "bob@example.com", firebaseId = "firebase2"))
        val charlie = userRepository.save(User(username = "charlie", email = "charlie@example.com", firebaseId = "firebase3"))

        bob.following.add(alice)
        charlie.following.add(alice)
        userRepository.save(bob)
        userRepository.save(charlie)

        mockMvc.perform(
            get("/rest/v1/users/alice/followers")
                .header("Authorization", "Bearer $token")
                .param("offset", "0")
                .param("limit", "1")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
    }

    @Test
    fun `GET followers returns 404 when user not found`() {
        mockMvc.perform(
            get("/rest/v1/users/nonexistent/followers")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `GET following returns following list`() {
        val alice = userRepository.save(User(username = "alice", email = "alice@example.com", firebaseId = firebaseId))
        val bob = userRepository.save(User(username = "bob", email = "bob@example.com", firebaseId = "firebase2"))

        alice.following.add(bob)
        userRepository.save(alice)

        mockMvc.perform(
            get("/rest/v1/users/alice/following")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].username").value("bob"))
    }

    @Test
    fun `GET following handles pagination`() {
        val alice = userRepository.save(User(username = "alice", email = "alice@example.com", firebaseId = firebaseId))
        val bob = userRepository.save(User(username = "bob", email = "bob@example.com", firebaseId = "firebase2"))
        val charlie = userRepository.save(User(username = "charlie", email = "charlie@example.com", firebaseId = "firebase3"))

        alice.following.add(bob)
        alice.following.add(charlie)
        userRepository.save(alice)

        mockMvc.perform(
            get("/rest/v1/users/alice/following")
                .header("Authorization", "Bearer $token")
                .param("offset", "0")
                .param("limit", "1")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
    }

    @Test
    fun `GET following returns 404 when user not found`() {
        mockMvc.perform(
            get("/rest/v1/users/nonexistent/following")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `GET followers count returns correct count`() {
        userRepository.save(User(username = "alice", email = "alice@example.com", firebaseId = "firebase1"))

        mockMvc.perform(
            get("/rest/v1/users/alice/followers/count")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isNumber)
    }

    @Test
    fun `GET followers count returns 404 when user not found`() {
        mockMvc.perform(
            get("/rest/v1/users/nonexistent/followers/count")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isNotFound)
    }

    // Count Following Tests

    @Test
    fun `GET following count returns correct count`() {
        val alice = userRepository.save(User(username = "alice", email = "alice@example.com", firebaseId = firebaseId))
        val bob = userRepository.save(User(username = "bob", email = "bob@example.com", firebaseId = "firebase2"))

        alice.following.add(bob)
        userRepository.save(alice)

        mockMvc.perform(
            get("/rest/v1/users/alice/following/count")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(content().string("1"))
    }

    @Test
    fun `GET following count returns 404 when user not found`() {
        mockMvc.perform(
            get("/rest/v1/users/nonexistent/following/count")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isNotFound)
    }

    // Block User Tests

    @Test
    fun `POST block blocks user successfully`() {
        userRepository.save(User(username = "alice", email = email, firebaseId = firebaseId))
        userRepository.save(User(username = "bob", email = "bob@example.com", firebaseId = "firebase2"))

        mockMvc.perform(
            post("/rest/v1/users/block/bob")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `POST block returns 404 when user not found`() {
        userRepository.save(User(username = "alice", email = email, firebaseId = firebaseId))

        mockMvc.perform(
            post("/rest/v1/users/block/nonexistent")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `POST block returns 400 when trying to block self`() {
        userRepository.save(User(username = "alice", email = email, firebaseId = firebaseId))

        mockMvc.perform(
            post("/rest/v1/users/block/alice")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `POST unblock unblocks user successfully`() {
        val alice = userRepository.save(User(username = "alice", email = email, firebaseId = firebaseId))
        val bob = userRepository.save(User(username = "bob", email = "bob@example.com", firebaseId = "firebase2"))

        alice.blocked.add(bob)
        userRepository.save(alice)

        mockMvc.perform(
            post("/rest/v1/users/unblock/bob")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `POST unblock returns 404 when user not found`() {
        userRepository.save(User(username = "alice", email = email, firebaseId = firebaseId))

        mockMvc.perform(
            post("/rest/v1/users/unblock/nonexistent")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `GET blocked returns blocked users list`() {
        val alice = userRepository.save(User(username = "alice", email = email, firebaseId = firebaseId))
        val bob = userRepository.save(User(username = "bob", email = "bob@example.com", firebaseId = "firebase2"))

        alice.blocked.add(bob)
        userRepository.save(alice)

        mockMvc.perform(
            get("/rest/v1/users/blocked")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].username").value("bob"))
    }

    @Test
    fun `GET blocked handles pagination`() {
        val alice = userRepository.save(User(username = "alice", email = email, firebaseId = firebaseId))
        val bob = userRepository.save(User(username = "bob", email = "bob@example.com", firebaseId = "firebase2"))
        val charlie = userRepository.save(User(username = "charlie", email = "charlie@example.com", firebaseId = "firebase3"))

        alice.blocked.add(bob)
        alice.blocked.add(charlie)
        userRepository.save(alice)

        mockMvc.perform(
            get("/rest/v1/users/blocked")
                .header("Authorization", "Bearer $token")
                .param("offset", "0")
                .param("limit", "1")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
    }

    @Test
    fun `GET blocked count returns correct count`() {
        val alice = userRepository.save(User(username = "alice", email = email, firebaseId = firebaseId))
        val bob = userRepository.save(User(username = "bob", email = "bob@example.com", firebaseId = "firebase2"))

        alice.blocked.add(bob)
        userRepository.save(alice)

        mockMvc.perform(
            get("/rest/v1/users/blocked/count")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(content().string("1"))
    }


    @Test
    fun `GET exists returns true when user exists by username`() {
        userRepository.save(User(username = "testuser", email = "test@example.com", firebaseId = firebaseId))

        mockMvc.perform(
            get("/rest/v1/users/exists")
                .header("Authorization", "Bearer $token")
                .param("username", "testuser")
        )
            .andExpect(status().isOk)
            .andExpect(content().string("true"))
    }

    @Test
    fun `GET exists returns false when user does not exist`() {
        mockMvc.perform(
            get("/rest/v1/users/exists")
                .header("Authorization", "Bearer $token")
                .param("username", "nonexistent")
        )
            .andExpect(status().isOk)
            .andExpect(content().string("false"))
    }

    @Test
    fun `GET exists returns 400 when neither username nor email provided`() {
        mockMvc.perform(
            get("/rest/v1/users/exists")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `GET search returns matching users`() {
        userRepository.save(User(username = "testuser1", email = "test1@example.com", firebaseId = "firebase1"))
        userRepository.save(User(username = "testuser2", email = "test2@example.com", firebaseId = "firebase2"))

        mockMvc.perform(
            get("/rest/v1/users/search")
                .header("Authorization", "Bearer $token")
                .param("query", "test")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
    }

    @Test
    fun `GET search returns empty list for blank query`() {
        mockMvc.perform(
            get("/rest/v1/users/search")
                .header("Authorization", "Bearer $token")
                .param("query", "")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun `GET search handles pagination`() {
        userRepository.save(User(username = "testuser1", email = "test1@example.com", firebaseId = "firebase1"))
        userRepository.save(User(username = "testuser2", email = "test2@example.com", firebaseId = "firebase2"))
        userRepository.save(User(username = "testuser3", email = "test3@example.com", firebaseId = "firebase3"))

        mockMvc.perform(
            get("/rest/v1/users/search")
                .header("Authorization", "Bearer $token")
                .param("query", "test")
                .param("offset", "0")
                .param("limit", "2")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
    }

    @Test
    fun `GET search excludes deleted users`() {
        userRepository.save(User(username = "activeuser", email = "active@example.com", firebaseId = "firebase1", deleted = false))
        userRepository.save(User(username = "deleteduser", email = "deleted@example.com", firebaseId = "firebase2", deleted = true))

        mockMvc.perform(
            get("/rest/v1/users/search")
                .header("Authorization", "Bearer $token")
                .param("query", "user")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].username").value("activeuser"))
    }
}
