package travesium.userservice.security

import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import traversium.tripservice.removeblocked.RemoveBlockedServiceGrpc
import traversium.tripservice.removeblocked.RemoveResponse

/**
 * @author Maja Razinger
 */
@TestConfiguration
class MockGrpcConfig {

    @Bean
    @Primary
    fun removeBlockedStub(): RemoveBlockedServiceGrpc.RemoveBlockedServiceBlockingStub {
        val mockStub = mock(RemoveBlockedServiceGrpc.RemoveBlockedServiceBlockingStub::class.java)

        val mockResponse = RemoveResponse.newBuilder()
            .setMessage("SUCCESS")
            .build()

        `when`(mockStub.withInterceptors(any())).thenReturn(mockStub)
        `when`(mockStub.removeBlockedUserRelations(any())).thenReturn(mockResponse)

        return mockStub
    }
}
