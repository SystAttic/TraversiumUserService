package travesium.userservice

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
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
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import travesium.userservice.dto.UserDto
import travesium.userservice.kafka.data.ReportingStreamData
import travesium.userservice.kafka.data.UserEvent
import travesium.userservice.service.UserService
import java.util.concurrent.LinkedBlockingQueue
import kotlin.test.Test


/**
 * @author Maja Razinger
 */
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = ["test-datastream"], bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@TestPropertySource(
    properties = [
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "kafka.reporting-topic=test-datastream",
        "kafka.bootstrap-servers=\${spring.kafka.bootstrap-servers}",
        "spring.kafka.consumer.group-id=user-service-tests",
    ]
)
@ContextConfiguration(classes = [KafkaTests.KafkaConsumerConfiguration::class])
class KafkaTests() {

    @Autowired
    private lateinit var userService: UserService

    @Autowired 
    lateinit var reportingKafkaConsumer: ReportingKafkaConsumer

    @BeforeEach
    fun beforeEach() {
        reportingKafkaConsumer.clearMessages()
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun createUser() {
        val userDto = UserDto(uid = "123", username = "test", email = "test@example.com")
        userService.createUser(userDto)

        waitForSize(1) { reportingKafkaConsumer.getMessages().size }
        val messages = reportingKafkaConsumer.getMessages()
        assert(messages.size == 1)
        val receivedData = messages[0] as ReportingStreamData
        assert(receivedData.action == UserEvent.USER_CREATED)
        userService.deleteUserByUsername("test")
    }

    @Test
    @Transactional
    fun createUserRollback() {
        val userDto = UserDto(uid = "123", username = "test", email = "test@example.com")
        userService.createUser(userDto)

        waitForSize(0) { reportingKafkaConsumer.getMessages().size }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun deleteUserByUsername() {
        val userDto = UserDto(uid = "123", username = "test", email = "nekinekineki@example.com")
        userService.createUser(userDto)
        waitForSize(1) { reportingKafkaConsumer.getMessages().size }
        reportingKafkaConsumer.clearMessages()
        userService.deleteUserByUsername("test")
        waitForSize(1) { reportingKafkaConsumer.getMessages().size }
        val messages = reportingKafkaConsumer.getMessages()
        assert(messages.size == 1)
        val receivedData = messages[0] as ReportingStreamData
        assert(receivedData.action == UserEvent.USER_DELETED)
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun deleteUserByEmail() {
        val userDto = UserDto(uid = "123", username = "test", email = "nekinekineki@example.com")
        userService.createUser(userDto)
        waitForSize(1) { reportingKafkaConsumer.getMessages().size }
        reportingKafkaConsumer.clearMessages()
        userService.deleteUserByEmail("nekinekineki@example.com")
        waitForSize(1) { reportingKafkaConsumer.getMessages().size }
        val messages = reportingKafkaConsumer.getMessages()
        assert(messages.size == 1)
        val receivedData = messages[0] as ReportingStreamData
        assert(receivedData.action == UserEvent.USER_DELETED)
    }

    class ReportingKafkaConsumer : MessageListener<String, ReportingStreamData> {
        private val messages = LinkedBlockingQueue<Any>()

        fun getMessages(): List<Any> = messages.toList()

        fun clearMessages() = messages.clear()

        override fun onMessage(data: ConsumerRecord<String, ReportingStreamData>) {
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
        fun reportingKafkaListenerContainer(
            objectMapper: ObjectMapper,
            reportingKafkaConsumer: ReportingKafkaConsumer,
            @Value("\${kafka.bootstrap-servers}") bootstrapServers: String,
            @Value("\${kafka.reporting-topic}") topic: String,
            @Value("\${spring.kafka.consumer.group-id}") groupId: String) =
            KafkaMessageListenerContainer(
                reportingConsumerFactory(objectMapper, bootstrapServers, groupId),
                kafkaContainerProperties(topic, emptySet(), reportingKafkaConsumer))
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