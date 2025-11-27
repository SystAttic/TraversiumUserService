package travesium.userservice

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import traversium.commonmultitenancy.TenantContext
import traversium.notification.kafka.NotificationStreamData
import travesium.userservice.db.model.Outbox
import travesium.userservice.db.model.User
import travesium.userservice.db.repository.OutboxRepository
import travesium.userservice.db.repository.UserRepository
import travesium.userservice.outbox.OutboxProcessor
import travesium.userservice.security.MockFirebaseConfig
import travesium.userservice.security.TestMultitenancyConfig
import travesium.userservice.service.UserService
import java.time.OffsetDateTime

/**
 * @author Maja Razinger
 */
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@SpringBootTest(classes = [UserServiceApplication::class])
@EmbeddedKafka(
    partitions = 1,
    topics = ["test-notifications"],
    bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
@TestPropertySource(
    properties = [
        "spring.kafka.notification-topic=test-notifications",
    ]
)
@ContextConfiguration(classes = [MockFirebaseConfig::class, TestMultitenancyConfig::class])
@DirtiesContext
class OutboxTests @Autowired constructor(
    private val userRepository: UserRepository,
    private val outboxRepository: OutboxRepository,
    private val userService: UserService,
    private val firebaseConfig: MockFirebaseConfig,
    private val objectMapper: ObjectMapper,
    private val outboxProcessor: OutboxProcessor
) {

    private lateinit var alice: User
    private lateinit var bob: User

    @BeforeEach
    fun setup() {
        userRepository.deleteAll()
        outboxRepository.deleteAll()

        TenantContext.setTenant("test-tenant")

        firebaseConfig.setTokenData("token1", "aliceUID", "alice@example.com")
        firebaseConfig.setTokenData("token2", "bobUID", "bob@example.com")
        firebaseConfig.setTokenData("token3", "charlieUID", "charlie@example.com")
        firebaseConfig.setTokenData("token4", "daveUID", "dave@example.com")

        alice = userRepository.save(User(username = "alice", email = "alice@example.com", firebaseId = "aliceUID"))
        bob = userRepository.save(User(username = "bob", email = "bob@example.com", firebaseId = "bobUID"))
        userRepository.save(User(username = "charlie", email = "charlie@example.com", firebaseId = "charlieUID"))
        userRepository.save(User(username = "dave", email = "dave@example.com", firebaseId = "daveUID"))
    }

    @AfterEach
    fun cleanup() {
        TenantContext.clear()
    }

    @Test
    fun followUserSavesEventToOutboxTableTest() {
        val auth = UsernamePasswordAuthenticationToken("principal", "token1")
        SecurityContextHolder.getContext().authentication = auth

        userService.followUser("bob")

        val outboxEvents = outboxRepository.findAll()
        assert(outboxEvents.size == 1)

        val event = outboxEvents[0]
        assert(event.eventType == "NOTIFICATION")
        assert(!event.processed)
        assert(event.tenantId == "test-tenant")

        val notification = objectMapper.readValue(event.payload, NotificationStreamData::class.java)
        assert(notification.senderId == "alice")
        assert(notification.receiverIds.contains("bob"))
        assert(notification.action == "FOLLOW")
    }

    @Test
    fun followUserRollbackShouldNotSaveToOutboxTableTest() {
        val auth = UsernamePasswordAuthenticationToken("principal", "token1")
        SecurityContextHolder.getContext().authentication = auth

        try {
            userService.followUser("alice")
        } catch (e: Exception) {
            // Expected exception due to self-follow
        }

        val outboxEvents = outboxRepository.findAll()
        assert(outboxEvents.isEmpty())
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun outboxProcessorMarksAsProcessedTest() {
        val notification = NotificationStreamData(
            senderId = "alice",
            receiverIds = listOf("bob"),
            action = "FOLLOW",
            timestamp = OffsetDateTime.now(),
            collectionReferenceId = null,
            nodeReferenceId = null,
            commentReferenceId = null
        )

        val outboxEvent = Outbox(
            eventType = "NOTIFICATION",
            payload = objectMapper.writeValueAsString(notification),
            tenantId = "test-tenant",
            processed = false
        )
        val saved = outboxRepository.save(outboxEvent)

        outboxProcessor.processOutboxEvents()

        val processedEvent = outboxRepository.findById(saved.id!!).get()
        assert(processedEvent.processed)
        assert(processedEvent.processedAt != null)
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun cleanUpTest() {
        val oldNotification = NotificationStreamData(
            senderId = "alice",
            receiverIds = listOf("bob"),
            action = "FOLLOW",
            timestamp = OffsetDateTime.now().minusDays(8),
            collectionReferenceId = null,
            nodeReferenceId = null,
            commentReferenceId = null
        )

        val oldEvent = Outbox(
            eventType = "NOTIFICATION",
            payload = objectMapper.writeValueAsString(oldNotification),
            tenantId = "test-tenant",
            processed = true,
            processedAt = OffsetDateTime.now().minusDays(8)
        )
        outboxRepository.save(oldEvent)

        val recentNotification = NotificationStreamData(
            senderId = "alice",
            receiverIds = listOf("bob"),
            action = "FOLLOW",
            timestamp = OffsetDateTime.now(),
            collectionReferenceId = null,
            nodeReferenceId = null,
            commentReferenceId = null
        )

        val recentEvent = Outbox(
            eventType = "NOTIFICATION",
            payload = objectMapper.writeValueAsString(recentNotification),
            tenantId = "test-tenant",
            processed = true,
            processedAt = OffsetDateTime.now()
        )
        outboxRepository.save(recentEvent)

        outboxProcessor.cleanupProcessedEvents()

        val remainingEvents = outboxRepository.findAll()
        assert(remainingEvents.size == 1)
        assert(remainingEvents[0].processedAt?.isAfter(OffsetDateTime.now().minusDays(7)) == true)
    }

    @Test
    @Transactional
    fun multipleFollowersTest() {
        val auth = UsernamePasswordAuthenticationToken("principal", "token1")
        SecurityContextHolder.getContext().authentication = auth

        userService.followUser("bob")
        userService.followUser("charlie")
        userService.followUser("dave")

        val outboxEvents = outboxRepository.findAll()
        assert(outboxEvents.size == 3)
        assert(outboxEvents.all { !it.processed })
    }
}
