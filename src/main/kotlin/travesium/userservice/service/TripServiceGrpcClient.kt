package travesium.userservice.service

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import org.apache.logging.log4j.kotlin.logger
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import traversium.commonmultitenancy.TenantContext
import traversium.commonmultitenancy.TenantUtils
import traversium.tripservice.removeblocked.RemoveBlockedServiceGrpc
import traversium.tripservice.removeblocked.RemoveRequest

@Service
class TripServiceGrpcClient(
    private val removeBlockedStub: RemoveBlockedServiceGrpc.RemoveBlockedServiceBlockingStub
) {

    @CircuitBreaker(name = "tripServiceGrpc")
    @Retry(name = "tripServiceGrpc")
    fun removeUserRelations(blockerId: String, blockedId: String): Boolean {
        logger.info("Attempting gRPC call to TripService for blocker: $blockerId, blocked: $blockedId")

        val request = RemoveRequest.newBuilder()
            .setBlockerId(blockerId)
            .setBlockedId(blockedId)
            .build()

        val firebaseToken = SecurityContextHolder.getContext().authentication.credentials as String

        val metadata = Metadata()
        metadata.put(
            Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER),
            "Bearer $firebaseToken"
        )
        metadata.put(
            Metadata.Key.of("X-Tenant-Id", Metadata.ASCII_STRING_MARSHALLER),
            TenantUtils.desanitizeTenantIdFromSchema(TenantContext.getTenant())
        )

        val response = removeBlockedStub
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
            .removeBlockedUserRelations(request)

        logger.info("gRPC call successful, response: ${response.message}")
        return response.message == "SUCCESS"
    }
}
