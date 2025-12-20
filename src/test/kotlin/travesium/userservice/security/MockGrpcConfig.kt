package travesium.userservice.security

import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import traversium.moderation.textmoderation.ModerateTextResponse
import traversium.moderation.textmoderation.TextModerationServiceGrpc
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

    @Bean
    @Primary
    fun textModerationStub(): TextModerationServiceGrpc.TextModerationServiceBlockingStub {
        val mockStub = mock(TextModerationServiceGrpc.TextModerationServiceBlockingStub::class.java)

        val mockResponse = ModerateTextResponse.newBuilder()
            .setAllowed(true)
            .setMaxSeverity(0)
            .setDecisionReason("Text is clean")
            .build()

        `when`(mockStub.withInterceptors(any())).thenReturn(mockStub)
        `when`(mockStub.moderateText(any())).thenReturn(mockResponse)

        return mockStub
    }
}
