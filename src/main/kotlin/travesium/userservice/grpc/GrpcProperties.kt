package travesium.userservice.grpc

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * @author Maja Razinger
 */

@ConfigurationProperties(prefix = "grpc")
class GrpcProperties {
    var trip: ServerConfig = ServerConfig(host = "localhost", port = 9091)
    var moderation: ServerConfig = ServerConfig(host = "localhost", port = 9090)
}

class ServerConfig(
    var host: String = "localhost",
    var port: Int = 9090
)