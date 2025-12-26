package travesium.userservice.grpc

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import traversium.moderation.textmoderation.TextModerationServiceGrpc
import traversium.tripservice.removeblocked.RemoveBlockedServiceGrpc

/**
 * @author Maja Razinger
 */
@Configuration
@EnableConfigurationProperties(GrpcProperties::class)
class GrpcClientConfig(
    private val grpcProperties: GrpcProperties,
) {

    @Bean(name = ["tripGrpcChannel"])
    fun tripGrpcChannel(): ManagedChannel {
        return ManagedChannelBuilder
            .forAddress(grpcProperties.trip.host, grpcProperties.trip.port)
            .usePlaintext()
            .build()
    }

    @Bean(name = ["moderationGrpcChannel"])
    fun moderationGrpcChannel(): ManagedChannel {
        return ManagedChannelBuilder
            .forAddress(grpcProperties.moderation.host, grpcProperties.moderation.port)
            .usePlaintext()
            .build()
    }

    @Bean
    fun removeBlockedStub(tripGrpcChannel: ManagedChannel): RemoveBlockedServiceGrpc.RemoveBlockedServiceBlockingStub =
        RemoveBlockedServiceGrpc
            .newBlockingStub(tripGrpcChannel)

    @Bean
    fun textModerationStub(
        moderationGrpcChannel: ManagedChannel,
        authInterceptor: GrpcAuthClientInterceptor): TextModerationServiceGrpc.TextModerationServiceBlockingStub =
        TextModerationServiceGrpc
            .newBlockingStub(moderationGrpcChannel)
            .withInterceptors(authInterceptor)
}