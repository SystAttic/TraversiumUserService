package travesium.userservice.service

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import org.apache.logging.log4j.kotlin.logger
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import traversium.commonmultitenancy.TenantContext
import traversium.moderation.textmoderation.ModerateTextRequest
import traversium.moderation.textmoderation.ModerateTextResponse
import traversium.moderation.textmoderation.TextModerationServiceGrpc

@Service
class ModerationServiceGrpcClient(
    private val textModerationStub: TextModerationServiceGrpc.TextModerationServiceBlockingStub
) {

    @CircuitBreaker(name = "moderationServiceGrpc")
    @Retry(name = "moderationServiceGrpc")
    fun moderateText(text: String): ModerateTextResponse {
        logger.info("Attempting gRPC call to ModerationService for text moderation")

        val request = ModerateTextRequest.newBuilder()
            .setText(text)
            .build()

        val firebaseToken = SecurityContextHolder.getContext().authentication.credentials as String

        val metadata = Metadata()
        metadata.put(
            Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER),
            "Bearer $firebaseToken"
        )
        metadata.put(
            Metadata.Key.of("X-Tenant-Id", Metadata.ASCII_STRING_MARSHALLER),
            TenantContext.getTenant()
        )

        val response = textModerationStub
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
            .moderateText(request)

        logger.info("gRPC call successful, allowed: ${response.allowed}, max_severity: ${response.maxSeverity}")
        return response
    }

    fun isTextAllowed(text: String): Boolean {
        val response = moderateText(text)
        return response.allowed
    }
}
