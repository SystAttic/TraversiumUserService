package travesium.userservice.grpc

import com.fasterxml.jackson.annotation.JsonProperty

data class KeycloakTokenResponse(
    @param:JsonProperty("access_token")
    val accessToken: String,

    @param:JsonProperty("expires_in")
    val expiresIn: Long,

    @param:JsonProperty("token_type")
    val tokenType: String,

    @param:JsonProperty("scope")
    val scope: String? = null
)
