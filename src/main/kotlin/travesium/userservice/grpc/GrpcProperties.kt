package travesium.userservice.grpc

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * @author Maja Razinger
 */

@ConfigurationProperties(prefix = "grpc")
class GrpcProperties(
    val trip: ServerConfig = ServerConfig(port = 9091),
    val moderation: ServerConfig = ServerConfig(port = 9090)
)

data class ServerConfig(
    val host: String = "localhost",
    val port: Int
)