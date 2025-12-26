package travesium.userservice

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.listener.KafkaMessageListenerContainer
import org.springframework.kafka.listener.MessageListener
import org.springframework.kafka.support.TopicPartitionOffset
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import traversium.audit.kafka.AuditStreamData
import traversium.audit.kafka.UserActivityAction
import traversium.notification.kafka.ActionType
import traversium.notification.kafka.NotificationStreamData
import travesium.userservice.db.model.User
import travesium.userservice.db.repository.UserRepository
import travesium.userservice.dto.UserDto
import travesium.userservice.kafka.data.ReportingStreamData
import travesium.userservice.kafka.data.UserEvent
import travesium.userservice.security.BaseSecuritySetup
import travesium.userservice.security.MockFirebaseConfig
import travesium.userservice.security.MockGrpcConfig
import travesium.userservice.security.TestMultitenancyConfig
import travesium.userservice.service.UserService
import java.util.concurrent.LinkedBlockingQueue
import kotlin.test.Test


/**
 * @author Maja Razinger
 */
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = ["test-datastream", "test-notifications", "test-audit"], bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@TestPropertySource(
    properties = [
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.reporting-topic=test-datastream",
        "spring.kafka.notification-topic=test-notifications",
        "spring.kafka.audit-topic=test-audit",
        "spring.kafka.consumer.group-id=user-service-tests",
        "spring.cloud.config.enabled=false"
    ]
)
@ContextConfiguration(classes = [KafkaTests.KafkaConsumerConfiguration::class, MockFirebaseConfig::class, TestMultitenancyConfig::class, MockGrpcConfig::class])
@ActiveProfiles("test")
class KafkaTests() : BaseSecuritySetup() {

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    lateinit var reportingKafkaConsumer: ReportingKafkaConsumer

    @Autowired
    lateinit var auditingKafkaConsumer: AuditingKafkaConsumer

    @Autowired
    lateinit var notificationKafkaConsumer: NotificationKafkaConsumer

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var mockFirebaseConfig: MockFirebaseConfig

    @BeforeEach
    fun beforeEach() {
        reportingKafkaConsumer.clearMessages()
        auditingKafkaConsumer.clearMessages()
        notificationKafkaConsumer.clearMessages()
        userRepository.deleteAll()

        mockFirebaseConfig.setTokenData("token1", "user1UID", "user1@example.com")
        mockFirebaseConfig.setTokenData("token2", "user2UID", "user2@example.com")
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun createUser() {
        val userDto = UserDto(username = "test", email = email, firebaseId = firebaseId)
        userService.createUser(userDto)

        waitForSize(1) { reportingKafkaConsumer.getMessages().size }
        val messages = reportingKafkaConsumer.getMessages()
        assert(messages.size == 1)
        val receivedData = messages[0] as ReportingStreamData
        assert(receivedData.action == UserEvent.USER_CREATED)
    }

    @Test
    @Transactional
    fun createUserRollback() {
        val userDto = UserDto(username = "test", email = email, firebaseId = firebaseId)
        userService.createUser(userDto)

        waitForSize(0) { reportingKafkaConsumer.getMessages().size }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun deleteUserByUsername() {
        val userDto = UserDto(username = "test", email = email, firebaseId = firebaseId)
        userService.createUser(userDto)
        waitForSize(1) { auditingKafkaConsumer.getMessages().size }
        auditingKafkaConsumer.clearMessages()
        userService.deleteUser()
        waitForSize(1) { auditingKafkaConsumer.getMessages().size }
        val messages = auditingKafkaConsumer.getMessages()
        assert(messages.size == 1)
        val receivedData = messages[0] as AuditStreamData
        assert(receivedData.action == UserActivityAction.USER_DELETED.name)
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun followUserSendsNotification() {
        userRepository.save(User(username = "alice", email = "user1@example.com", firebaseId = "user1UID"))
        userRepository.save(User(username = "bob", email = "user2@example.com", firebaseId = "user2UID"))

        val auth = UsernamePasswordAuthenticationToken("principal", "token1")
        SecurityContextHolder.getContext().authentication = auth

        userService.followUser("bob")

        waitForSize(1) { notificationKafkaConsumer.getMessages().size }

        val messages = notificationKafkaConsumer.getMessages()
        assert(messages.size == 1)

        val notification = messages[0] as NotificationStreamData
        assert(notification.senderId == "alice")
        assert(notification.receiverIds.contains("bob"))
        assert(notification.action == ActionType.FOLLOW)
    }

    @Test
    @Transactional
    fun followUserRollbackNoNotification() {
        userRepository.save(User(username = "alice", email = "user1@example.com", firebaseId = "user1UID"))
        userRepository.save(User(username = "bob", email = "user2@example.com", firebaseId = "user2UID"))

        val auth = UsernamePasswordAuthenticationToken("principal", "token1")
        SecurityContextHolder.getContext().authentication = auth

        userService.followUser("bob")

        waitForSize(0) { notificationKafkaConsumer.getMessages().size }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun createUserSendsAuditEvent() {
        val userDto = UserDto(username = "test", email = email, firebaseId = firebaseId)
        userService.createUser(userDto)

        waitForSize(1) { auditingKafkaConsumer.getMessages().size }
        val messages = auditingKafkaConsumer.getMessages()
        assert(messages.size == 1)
        val receivedData = messages[0] as AuditStreamData
        assert(receivedData.action == UserActivityAction.USER_CREATED.name)
        assert(receivedData.userId == firebaseId)
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun followUserSendsAuditEvent() {
        userRepository.save(User(username = "alice", email = "user1@example.com", firebaseId = "user1UID"))
        userRepository.save(User(username = "bob", email = "user2@example.com", firebaseId = "user2UID"))

        val auth = UsernamePasswordAuthenticationToken("principal", "token1")
        SecurityContextHolder.getContext().authentication = auth

        userService.followUser("bob")

        waitForSize(1) { auditingKafkaConsumer.getMessages().size }

        val messages = auditingKafkaConsumer.getMessages()
        assert(messages.size == 1)

        val auditData = messages[0] as AuditStreamData
        assert(auditData.action == UserActivityAction.USER_FOLLOWED.name)
        assert(auditData.userId == "user1UID")
        assert(auditData.metadata?.get("followedUserId") == "user2UID")
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun unfollowUserSendsAuditEvent() {
        val alice = userRepository.save(User(username = "alice", email = "user1@example.com", firebaseId = "user1UID"))
        val bob = userRepository.save(User(username = "bob", email = "user2@example.com", firebaseId = "user2UID"))

        alice.following.add(bob)
        userRepository.save(alice)

        val auth = UsernamePasswordAuthenticationToken("principal", "token1")
        SecurityContextHolder.getContext().authentication = auth

        userService.unfollowUser("bob")

        waitForSize(1) { auditingKafkaConsumer.getMessages().size }

        val messages = auditingKafkaConsumer.getMessages()
        assert(messages.size == 1)

        val auditData = messages[0] as AuditStreamData
        assert(auditData.action == UserActivityAction.USER_UNFOLLOWED.name)
        assert(auditData.userId == "user1UID")
        assert(auditData.metadata?.get("unfollowedUserId") == "user2UID")
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun blockUserSendsAuditEvent() {
        userRepository.save(User(username = "alice", email = "user1@example.com", firebaseId = "user1UID"))
        userRepository.save(User(username = "bob", email = "user2@example.com", firebaseId = "user2UID"))

        val auth = UsernamePasswordAuthenticationToken("principal", "token1")
        SecurityContextHolder.getContext().authentication = auth

        userService.blockUser("bob")

        waitForSize(1) { auditingKafkaConsumer.getMessages().size }

        val messages = auditingKafkaConsumer.getMessages()
        assert(messages.size == 1)

        val auditData = messages[0] as AuditStreamData
        assert(auditData.action == UserActivityAction.USER_BLOCKED.name)
        assert(auditData.userId == "user1UID")
        assert(auditData.metadata?.get("blockedUserId") == "user2UID")
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun unblockUserSendsAuditEvent() {
        val alice = userRepository.save(User(username = "alice", email = "user1@example.com", firebaseId = "user1UID"))
        val bob = userRepository.save(User(username = "bob", email = "user2@example.com", firebaseId = "user2UID"))

        alice.blocked.add(bob)
        userRepository.save(alice)

        val auth = UsernamePasswordAuthenticationToken("principal", "token1")
        SecurityContextHolder.getContext().authentication = auth

        userService.unblockUser("bob")

        waitForSize(1) { auditingKafkaConsumer.getMessages().size }

        val messages = auditingKafkaConsumer.getMessages()
        assert(messages.size == 1)

        val auditData = messages[0] as AuditStreamData
        assert(auditData.action == UserActivityAction.USER_UNBLOCKED.name)
        assert(auditData.userId == "user1UID")
        assert(auditData.metadata?.get("unblockedUserId") == "user2UID")
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun updateUserDisplayNameSendsAuditEvent() {
        val userDto = UserDto(username = "alice", email = email, firebaseId = firebaseId)
        val createdUser = userService.createUser(userDto)

        waitForSize(1) { auditingKafkaConsumer.getMessages().size }
        auditingKafkaConsumer.clearMessages()

        val updateDto = UserDto(userId = createdUser.userId, displayName = "Alice Updated")
        userService.updateUser(updateDto)

        waitForSize(1) { auditingKafkaConsumer.getMessages().size }

        val messages = auditingKafkaConsumer.getMessages()
        assert(messages.size == 1)

        val auditData = messages[0] as AuditStreamData
        assert(auditData.action == UserActivityAction.USER_DISPLAY_NAME_CHANGED.name)
        assert(auditData.userId == firebaseId)
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun updateUserMultipleFieldsSendsMultipleAuditEvents() {
        val userDto = UserDto(username = "alice", email = email, firebaseId = firebaseId)
        val createdUser = userService.createUser(userDto)

        waitForSize(1) { auditingKafkaConsumer.getMessages().size }
        auditingKafkaConsumer.clearMessages()

        val updateDto = UserDto(
            userId = createdUser.userId,
            displayName = "Alice Updated",
            description = "New description",
            firstName = "Alice"
        )
        userService.updateUser(updateDto)

        waitForSize(3) { auditingKafkaConsumer.getMessages().size }

        val messages = auditingKafkaConsumer.getMessages()
        assert(messages.size == 3)

        val actions = messages.map { (it as AuditStreamData).action }
        assert(actions.contains(UserActivityAction.USER_DISPLAY_NAME_CHANGED.name))
        assert(actions.contains(UserActivityAction.USER_DESCRIPTION_CHANGED.name))
        assert(actions.contains(UserActivityAction.USER_FIRST_NAME_CHANGED.name))
    }

    class ReportingKafkaConsumer : MessageListener<String, ReportingStreamData> {
        private val messages = LinkedBlockingQueue<Any>()

        fun getMessages(): List<Any> = messages.toList()

        fun clearMessages() = messages.clear()

        override fun onMessage(data: ConsumerRecord<String, ReportingStreamData>) {
            messages.add(data.value())
        }
    }

    class AuditingKafkaConsumer : MessageListener<String, AuditStreamData> {
        private val messages = LinkedBlockingQueue<Any>()

        fun getMessages(): List<Any> = messages.toList()

        fun clearMessages() = messages.clear()

        override fun onMessage(data: ConsumerRecord<String, AuditStreamData>) {
            messages.add(data.value())
        }
    }

    class NotificationKafkaConsumer : MessageListener<String, NotificationStreamData> {
        private val messages = LinkedBlockingQueue<Any>()

        fun getMessages(): List<Any> = messages.toList()

        fun clearMessages() = messages.clear()

        override fun onMessage(data: ConsumerRecord<String, NotificationStreamData>) {
            messages.add(data.value())
        }
    }

    fun waitForSize(target: Int, source: () -> Int) {
        val start = System.currentTimeMillis()
        while (source.invoke() != target && (System.currentTimeMillis() - start) <= 15000) {
            try {
                Thread.sleep(50)
            } catch (e: InterruptedException) {
                throw Error(e)
            }
        }
        if ((System.currentTimeMillis() - start) >= 15000) {
            throw IllegalStateException("Size ${source.invoke()} is != target $target")
        }
    }

    @TestConfiguration
    class KafkaConsumerConfiguration {

        @Bean
        fun reportingKafkaConsumer() = ReportingKafkaConsumer()

        @Bean
        fun notificationKafkaConsumer() = NotificationKafkaConsumer()

        @Bean
        fun auditingKafkaConsumer() = AuditingKafkaConsumer()

        @Bean
        fun reportingKafkaListenerContainer(
            objectMapper: ObjectMapper,
            reportingKafkaConsumer: ReportingKafkaConsumer,
            @Value("\${spring.kafka.bootstrap-servers}") bootstrapServers: String,
            @Value("\${spring.kafka.reporting-topic}") topic: String,
            @Value("\${spring.kafka.consumer.group-id}") groupId: String) =
            KafkaMessageListenerContainer(
                reportingConsumerFactory(objectMapper, bootstrapServers, groupId),
                kafkaContainerProperties(topic, emptySet(), reportingKafkaConsumer))
                .apply {
                    commonErrorHandler = DefaultErrorHandler()
                }

        @Bean
        fun auditingKafkaListenerContainer(
            objectMapper: ObjectMapper,
            auditingKafkaConsumer: AuditingKafkaConsumer,
            @Value("\${spring.kafka.bootstrap-servers}") bootstrapServers: String,
            @Value("\${spring.kafka.audit-topic}") topic: String,
            @Value("\${spring.kafka.consumer.group-id}") groupId: String) =
            KafkaMessageListenerContainer(
                auditingConsumerFactory(objectMapper, bootstrapServers, groupId),
                kafkaContainerProperties(topic, emptySet(), auditingKafkaConsumer))
                .apply {
                    commonErrorHandler = DefaultErrorHandler()
                }

        @Bean
        fun notificationKafkaListenerContainer(
            objectMapper: ObjectMapper,
            notificationKafkaConsumer: NotificationKafkaConsumer,
            @Value("\${spring.kafka.bootstrap-servers}") bootstrapServers: String,
            @Value("\${spring.kafka.notification-topic}") topic: String,
            @Value("\${spring.kafka.consumer.group-id}") groupId: String) =
            KafkaMessageListenerContainer(
                notificationConsumerFactory(objectMapper, bootstrapServers, groupId),
                kafkaContainerProperties(topic, emptySet(), notificationKafkaConsumer))
                .apply {
                    commonErrorHandler = DefaultErrorHandler()
                }

        private fun kafkaContainerProperties(topic: String, partitions: Set<Int>, listener: MessageListener<String, *>): ContainerProperties {
            val topics = partitions.map { TopicPartitionOffset(topic, it) }.toTypedArray()
            return (if (topics.isNotEmpty()) ContainerProperties(*topics) else ContainerProperties(topic)).apply {
                isSyncCommits = true
                ackMode = ContainerProperties.AckMode.RECORD
                messageListener = listener
            }
        }

        fun reportingConsumerFactory(
            objectMapper: ObjectMapper,
            bootstrapServers: String,
            groupId: String
        ): DefaultKafkaConsumerFactory<String, ReportingStreamData> =
            DefaultKafkaConsumerFactory(
                kafkaConsumerConfig(bootstrapServers, groupId, 1048576, 1048576, ReportingStreamData::class.java),
                StringDeserializer(),
                JsonDeserializer(ReportingStreamData::class.java, objectMapper)
            )

        fun auditingConsumerFactory(
            objectMapper: ObjectMapper,
            bootstrapServers: String,
            groupId: String
        ): DefaultKafkaConsumerFactory<String, AuditStreamData> =
            DefaultKafkaConsumerFactory(
                kafkaConsumerConfig(bootstrapServers, groupId, 1048576, 1048576, ReportingStreamData::class.java),
                StringDeserializer(),
                JsonDeserializer(AuditStreamData ::class.java, objectMapper)
            )

        fun notificationConsumerFactory(
            objectMapper: ObjectMapper,
            bootstrapServers: String,
            groupId: String
        ): DefaultKafkaConsumerFactory<String, NotificationStreamData> =
            DefaultKafkaConsumerFactory(
                kafkaConsumerConfig(bootstrapServers, groupId, 1048576, 1048576, NotificationStreamData::class.java),
                StringDeserializer(),
                JsonDeserializer(NotificationStreamData::class.java, objectMapper)
            )

        private fun kafkaConsumerConfig(
            bootstrapServer: String,
            groupId: String,
            fetchMaxBytes: Int,
            maxPartitionFetchBytes: Int,
            className : Class<*>): MutableMap<String, Any> =
            mutableMapOf<String, Any>().apply {
                this[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServer
                this[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = false
                this[ConsumerConfig.ISOLATION_LEVEL_CONFIG] = "read_committed"
                this[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
                this[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = ErrorHandlingDeserializer::class.java
                this[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = ErrorHandlingDeserializer::class.java
                this[ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS] = org.apache.kafka.common.serialization.StringDeserializer::class.java
                this[ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS] = JsonDeserializer::class.java
                this[ConsumerConfig.FETCH_MAX_BYTES_CONFIG] = fetchMaxBytes
                this[ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG] = maxPartitionFetchBytes
                this[JsonDeserializer.VALUE_DEFAULT_TYPE] = className
                this[ConsumerConfig.GROUP_ID_CONFIG] = groupId
            }
    }
}