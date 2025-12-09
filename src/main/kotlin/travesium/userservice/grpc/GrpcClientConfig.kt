package travesium.userservice.grpc

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import traversium.tripservice.removeblocked.RemoveBlockedServiceGrpc

/**
 * @author Maja Razinger
 */
@Configuration
@EnableConfigurationProperties(GrpcProperties::class)
class GrpcClientConfig(
    private val grpcProperties: GrpcProperties,
) {

    @Bean
    fun grpcClient(): ManagedChannel {
        return ManagedChannelBuilder.forAddress(grpcProperties.host, grpcProperties.port).usePlaintext().build()
    }

    @Bean
    fun removeBlockedStub(channel: ManagedChannel): RemoveBlockedServiceGrpc.RemoveBlockedServiceBlockingStub =
        RemoveBlockedServiceGrpc.newBlockingStub(channel)
}