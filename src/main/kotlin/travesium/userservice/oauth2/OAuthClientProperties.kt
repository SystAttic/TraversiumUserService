package travesium.userservice.oauth2

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.cloud.context.config.annotation.RefreshScope
import org.springframework.context.annotation.Configuration

@Configuration
@RefreshScope
@ConfigurationProperties(prefix = "security.oauth2.client")
class OAuthClientProperties(
    var tokenUri: String = "",
    var clientId: String = "",
    var clientSecret: String = "",
    var grantType: String = "",
    var refreshSkewSeconds: Long = 30
)
