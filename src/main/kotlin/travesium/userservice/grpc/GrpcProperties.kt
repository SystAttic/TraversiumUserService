package travesium.userservice.grpc

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * @author Maja Razinger
 */

@ConfigurationProperties(prefix = "grpc.server")
class GrpcProperties(
    val host: String = "localhost",
    val port: Int = 9090
)