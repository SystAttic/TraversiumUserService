package travesium.userservice

import com.fasterxml.jackson.databind.ObjectMapper
import io.grpc.Status
import io.grpc.StatusRuntimeException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import traversium.moderation.textmoderation.ModerateTextResponse
import traversium.moderation.textmoderation.TextModerationServiceGrpc
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
class UserModerationTest : BaseSecuritySetup() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var mockFirebaseConfig: MockFirebaseConfig

    @Autowired
    private lateinit var textModerationStub: TextModerationServiceGrpc.TextModerationServiceBlockingStub

    private lateinit var stubWithInterceptors: TextModerationServiceGrpc.TextModerationServiceBlockingStub

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.clearContext()
        setupDefaultAuth()
        userRepository.deleteAll()
        mockFirebaseConfig.setTokenData(token, firebaseId, email)

        stubWithInterceptors = org.mockito.Mockito.mock(TextModerationServiceGrpc.TextModerationServiceBlockingStub::class.java)
        `when`(textModerationStub.withInterceptors(any())).thenReturn(stubWithInterceptors)
    }

    @AfterEach
    fun tearDown() {
        userRepository.deleteAll()
        SecurityContextHolder.clearContext()
    }


    @Test
    fun `POST users creates user successfully when moderation allows`() {
        val allowedResponse = ModerateTextResponse.newBuilder()
            .setAllowed(true)
            .setMaxSeverity(0)
            .setDecisionReason("Text is clean")
            .build()

        `when`(stubWithInterceptors.moderateText(any())).thenReturn(allowedResponse)

        val userDto = UserDto(
            username = "testuser",
            email = "test@example.com",
            firebaseId = firebaseId,
            displayName = "Test User",
            description = "This is a clean description",
            firstName = "Test",
            lastName = "User"
        )

        mockMvc.perform(
            post("/rest/v1/users")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userDto))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.username").value("testuser"))
            .andExpect(jsonPath("$.displayName").value("Test User"))
    }

    @Test
    fun `POST users fails when moderation rejects text`() {
        val rejectedResponse = ModerateTextResponse.newBuilder()
            .setAllowed(false)
            .setMaxSeverity(5)
            .setDecisionReason("Contains offensive content")
            .build()

        `when`(stubWithInterceptors.moderateText(any())).thenReturn(rejectedResponse)

        val userDto = UserDto(
            username = "testuser",
            email = "test@example.com",
            firebaseId = firebaseId,
            displayName = "Offensive Name",
            description = "Bad content"
        )

        mockMvc.perform(
            post("/rest/v1/users")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userDto))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `POST users fails when moderation service unavailable`() {
        `when`(stubWithInterceptors.moderateText(any()))
            .thenThrow(StatusRuntimeException(Status.UNAVAILABLE))

        val userDto = UserDto(
            username = "testuser",
            email = "test@example.com",
            firebaseId = firebaseId,
            description = "Some description"
        )

        mockMvc.perform(
            post("/rest/v1/users")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userDto))
        )
            .andExpect(status().isInternalServerError)
    }

    @Test
    fun `POST users succeeds when minimal text fields provided`() {
        val allowedResponse = ModerateTextResponse.newBuilder()
            .setAllowed(true)
            .setMaxSeverity(0)
            .setDecisionReason("Text is clean")
            .build()

        `when`(stubWithInterceptors.moderateText(any())).thenReturn(allowedResponse)

        val userDto = UserDto(
            username = "testuser",
            email = "test@example.com",
            firebaseId = firebaseId
        )

        mockMvc.perform(
            post("/rest/v1/users")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userDto))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.username").value("testuser"))
    }

    @Test
    fun `POST users moderates combined text fields`() {
        val allowedResponse = ModerateTextResponse.newBuilder()
            .setAllowed(true)
            .setMaxSeverity(0)
            .setDecisionReason("Text is clean")
            .build()

        `when`(stubWithInterceptors.moderateText(any())).thenReturn(allowedResponse)

        val userDto = UserDto(
            username = "testuser",
            email = "test@example.com",
            firebaseId = firebaseId,
            displayName = "Display",
            description = "Description",
            firstName = "First",
            lastName = "Last"
        )

        mockMvc.perform(
            post("/rest/v1/users")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userDto))
        )
            .andExpect(status().isOk)

        org.mockito.Mockito.verify(stubWithInterceptors).moderateText(any())
    }


    @Test
    fun `PUT users updates user successfully when moderation allows`() {
        val user = userRepository.save(
            User(
                username = "testuser",
                email = email,
                firebaseId = firebaseId,
                description = "old description"
            )
        )

        val allowedResponse = ModerateTextResponse.newBuilder()
            .setAllowed(true)
            .setMaxSeverity(0)
            .setDecisionReason("Text is clean")
            .build()

        `when`(stubWithInterceptors.moderateText(any())).thenReturn(allowedResponse)

        val updateDto = UserDto(
            userId = user.userId,
            description = "new clean description",
            displayName = "Clean Display Name"
        )

        mockMvc.perform(
            put("/rest/v1/users")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.description").value("new clean description"))
            .andExpect(jsonPath("$.displayName").value("Clean Display Name"))
    }

    @Test
    fun `PUT users fails when moderation rejects updated text`() {
        val user = userRepository.save(
            User(
                username = "testuser",
                email = email,
                firebaseId = firebaseId,
                description = "old description"
            )
        )

        val rejectedResponse = ModerateTextResponse.newBuilder()
            .setAllowed(false)
            .setMaxSeverity(5)
            .setDecisionReason("Contains offensive content")
            .build()

        `when`(stubWithInterceptors.moderateText(any())).thenReturn(rejectedResponse)

        val updateDto = UserDto(
            userId = user.userId,
            description = "offensive content"
        )

        mockMvc.perform(
            put("/rest/v1/users")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `PUT users fails when moderation service unavailable during update`() {
        val user = userRepository.save(
            User(
                username = "testuser",
                email = email,
                firebaseId = firebaseId,
                description = "old description"
            )
        )

        `when`(stubWithInterceptors.moderateText(any()))
            .thenThrow(StatusRuntimeException(Status.UNAVAILABLE))

        val updateDto = UserDto(
            userId = user.userId,
            description = "new description"
        )

        mockMvc.perform(
            put("/rest/v1/users")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto))
        )
            .andExpect(status().isInternalServerError)
    }

    @Test
    fun `PUT users succeeds when updating only non-moderated fields`() {
        val user = userRepository.save(
            User(
                username = "testuser",
                email = email,
                firebaseId = firebaseId,
                description = "description"
            )
        )

        // Update only avatar and cover photo (not moderated)
        val updateDto = UserDto(
            userId = user.userId,
            avatarPhotoReference = "new-avatar.jpg",
            coverPhotoReference = "new-cover.jpg"
        )

        mockMvc.perform(
            put("/rest/v1/users")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.avatarPhotoReference").value("new-avatar.jpg"))
            .andExpect(jsonPath("$.coverPhotoReference").value("new-cover.jpg"))
    }

    @Test
    fun `PUT users moderates only updated text fields`() {
        val user = userRepository.save(
            User(
                username = "testuser",
                email = email,
                firebaseId = firebaseId,
                description = "old description",
                displayName = "Old Name"
            )
        )

        val allowedResponse = ModerateTextResponse.newBuilder()
            .setAllowed(true)
            .setMaxSeverity(0)
            .setDecisionReason("Text is clean")
            .build()

        `when`(stubWithInterceptors.moderateText(any())).thenReturn(allowedResponse)

        val updateDto = UserDto(
            userId = user.userId,
            firstName = "NewFirst",
            lastName = "NewLast"
        )

        mockMvc.perform(
            put("/rest/v1/users")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.firstName").value("NewFirst"))
            .andExpect(jsonPath("$.lastName").value("NewLast"))

        org.mockito.Mockito.verify(stubWithInterceptors).moderateText(any())
    }

    @Test
    fun `PUT users handles high severity moderation response`() {
        val user = userRepository.save(
            User(
                username = "testuser",
                email = email,
                firebaseId = firebaseId
            )
        )

        val highSeverityResponse = ModerateTextResponse.newBuilder()
            .setAllowed(false)
            .setMaxSeverity(10)
            .setDecisionReason("Contains hate speech")
            .addCategories(
                traversium.moderation.textmoderation.CategoryResult.newBuilder()
                    .setCategory("HATE_SPEECH")
                    .setSeverity(10)
                    .build()
            )
            .build()

        `when`(stubWithInterceptors.moderateText(any())).thenReturn(highSeverityResponse)

        val updateDto = UserDto(
            userId = user.userId,
            description = "hate speech content"
        )

        mockMvc.perform(
            put("/rest/v1/users")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `PUT users handles blocklist hit response`() {
        val user = userRepository.save(
            User(
                username = "testuser",
                email = email,
                firebaseId = firebaseId
            )
        )

        val blocklistResponse = ModerateTextResponse.newBuilder()
            .setAllowed(false)
            .setMaxSeverity(8)
            .setDecisionReason("Matched blocklist")
            .addBlocklistHits(
                traversium.moderation.textmoderation.BlocklistHit.newBuilder()
                    .setBlocklistName("profanity_list")
                    .setMatchedText("badword")
                    .build()
            )
            .build()

        `when`(stubWithInterceptors.moderateText(any())).thenReturn(blocklistResponse)

        val updateDto = UserDto(
            userId = user.userId,
            description = "contains badword"
        )

        mockMvc.perform(
            put("/rest/v1/users")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto))
        )
            .andExpect(status().isBadRequest)
    }
}
