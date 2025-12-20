//package travesium.userservice
//
//import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
//import io.grpc.Status
//import io.grpc.StatusRuntimeException
//import org.junit.jupiter.api.Assertions.*
//import org.junit.jupiter.api.BeforeEach
//import org.junit.jupiter.api.Test
//import org.mockito.Mockito.*
//import org.mockito.kotlin.any
//import org.springframework.beans.factory.annotation.Autowired
//import org.springframework.boot.test.context.SpringBootTest
//import org.springframework.test.annotation.DirtiesContext
//import org.springframework.test.context.ActiveProfiles
//import org.springframework.test.context.bean.override.mockito.MockitoBean
//import traversium.tripservice.removeblocked.RemoveBlockedServiceGrpc
//import traversium.tripservice.removeblocked.RemoveRequest
//import traversium.tripservice.removeblocked.RemoveResponse
//import travesium.userservice.security.BaseSecuritySetup
//import travesium.userservice.service.FirebaseService
//import travesium.userservice.service.TripServiceGrpcClient
//
//@SpringBootTest
//@ActiveProfiles("test")
//@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
//class CircuitBreakerRetryTest : BaseSecuritySetup() {
//
//    @Autowired
//    private lateinit var tripServiceGrpcClient: TripServiceGrpcClient
//
//    @MockitoBean
//    private lateinit var removeBlockedStub: RemoveBlockedServiceGrpc.RemoveBlockedServiceBlockingStub
//
//    @MockitoBean
//    private lateinit var firebaseService: FirebaseService
//
//    @Autowired
//    private lateinit var circuitBreakerRegistry: CircuitBreakerRegistry
//
//    private lateinit var stubWithInterceptors: RemoveBlockedServiceGrpc.RemoveBlockedServiceBlockingStub
//
//    @BeforeEach
//    fun setup() {
//        setupDefaultFirebaseMocks()
//
//        val circuitBreaker = circuitBreakerRegistry.circuitBreaker("tripServiceGrpc")
//        circuitBreaker.reset()
//        circuitBreaker.transitionToClosedState()
//
//        stubWithInterceptors = mock(RemoveBlockedServiceGrpc.RemoveBlockedServiceBlockingStub::class.java)
//        `when`(removeBlockedStub.withInterceptors(any())).thenReturn(stubWithInterceptors)
//    }
//
//    @Test
//    fun `successful call returns true`() {
//        val successResponse = RemoveResponse.newBuilder()
//            .setMessage("SUCCESS")
//            .build()
//
//        `when`(stubWithInterceptors.removeBlockedUserRelations(any<RemoveRequest>()))
//            .thenReturn(successResponse)
//
//        val result = tripServiceGrpcClient.removeUserRelations("blocker123", "blocked456")
//
//        assertTrue(result)
//        verify(stubWithInterceptors, times(1)).removeBlockedUserRelations(any<RemoveRequest>())
//    }
//
//    @Test
//    fun `retries on failure and succeeds on second attempt`() {
//        val successResponse = RemoveResponse.newBuilder()
//            .setMessage("SUCCESS")
//            .build()
//
//        `when`(stubWithInterceptors.removeBlockedUserRelations(any<RemoveRequest>()))
//            .thenThrow(StatusRuntimeException(Status.UNAVAILABLE))
//            .thenReturn(successResponse)
//
//        val result = tripServiceGrpcClient.removeUserRelations("blocker123", "blocked456")
//
//        assertTrue(result)
//        verify(stubWithInterceptors, times(2)).removeBlockedUserRelations(any<RemoveRequest>())
//    }
//
//    @Test
//    fun `retries max 3 times then throws exception`() {
//        `when`(stubWithInterceptors.removeBlockedUserRelations(any<RemoveRequest>()))
//            .thenThrow(StatusRuntimeException(Status.UNAVAILABLE))
//
//        assertThrows(StatusRuntimeException::class.java) {
//            tripServiceGrpcClient.removeUserRelations("blocker123", "blocked456")
//        }
//
//        verify(stubWithInterceptors, times(3)).removeBlockedUserRelations(any<RemoveRequest>())
//    }
//
//    @Test
//    fun `succeeds on third attempt`() {
//        val successResponse = RemoveResponse.newBuilder()
//            .setMessage("SUCCESS")
//            .build()
//
//        `when`(stubWithInterceptors.removeBlockedUserRelations(any<RemoveRequest>()))
//            .thenThrow(StatusRuntimeException(Status.UNAVAILABLE))
//            .thenThrow(StatusRuntimeException(Status.UNAVAILABLE))
//            .thenReturn(successResponse)
//
//        val result = tripServiceGrpcClient.removeUserRelations("blocker123", "blocked456")
//
//        assertTrue(result)
//        verify(stubWithInterceptors, times(3)).removeBlockedUserRelations(any<RemoveRequest>())
//    }
//
//    @Test
//    fun `returns false on non-SUCCESS response`() {
//        val failureResponse = RemoveResponse.newBuilder()
//            .setMessage("FAILURE")
//            .build()
//
//        `when`(stubWithInterceptors.removeBlockedUserRelations(any<RemoveRequest>()))
//            .thenReturn(failureResponse)
//
//        val result = tripServiceGrpcClient.removeUserRelations("blocker123", "blocked456")
//
//        assertFalse(result)
//        verify(stubWithInterceptors, times(1)).removeBlockedUserRelations(any<RemoveRequest>())
//    }
//
//    @Test
//    fun `circuit breaker records failures`() {
//        `when`(stubWithInterceptors.removeBlockedUserRelations(any<RemoveRequest>()))
//            .thenThrow(StatusRuntimeException(Status.UNAVAILABLE))
//
//        val circuitBreaker = circuitBreakerRegistry.circuitBreaker("tripServiceGrpc")
//
//        repeat(5) {
//            try {
//                tripServiceGrpcClient.removeUserRelations("blocker$it", "blocked$it")
//            } catch (e: Exception) {
//                // Expected: StatusRuntimeException or CallNotPermittedException
//            }
//        }
//
//        val metrics = circuitBreaker.metrics
//        assertTrue(metrics.numberOfFailedCalls >= 3)
//    }
//
//    @Test
//    fun `circuit breaker opens after threshold`() {
//        `when`(stubWithInterceptors.removeBlockedUserRelations(any<RemoveRequest>()))
//            .thenThrow(StatusRuntimeException(Status.UNAVAILABLE))
//
//        val circuitBreaker = circuitBreakerRegistry.circuitBreaker("tripServiceGrpc")
//
//        repeat(10) {
//            try {
//                tripServiceGrpcClient.removeUserRelations("blocker$it", "blocked$it")
//            } catch (e: Exception) {
//                // Expected - either StatusRuntimeException or CallNotPermittedException when circuit opens
//            }
//        }
//
//        val metrics = circuitBreaker.metrics
//        assertTrue(metrics.numberOfFailedCalls >= 5)
//        assertTrue(metrics.failureRate >= 50.0f)
//    }
//
//    @Test
//    fun `circuit breaker remains closed with successful calls`() {
//        val successResponse = RemoveResponse.newBuilder()
//            .setMessage("SUCCESS")
//            .build()
//
//        `when`(stubWithInterceptors.removeBlockedUserRelations(any<RemoveRequest>()))
//            .thenReturn(successResponse)
//
//        val circuitBreaker = circuitBreakerRegistry.circuitBreaker("tripServiceGrpc")
//
//        repeat(10) {
//            val result = tripServiceGrpcClient.removeUserRelations("blocker$it", "blocked$it")
//            assertTrue(result)
//        }
//
//        assertEquals(io.github.resilience4j.circuitbreaker.CircuitBreaker.State.CLOSED,
//                     circuitBreaker.state)
//        val metrics = circuitBreaker.metrics
//        assertEquals(0.0f, metrics.failureRate)
//    }
//
//    private fun setupDefaultFirebaseMocks() {
//        lenient().`when`(firebaseService.extractUidFromToken(token)).thenReturn(firebaseId)
//        lenient().`when`(firebaseService.extractEmailFromToken(token)).thenReturn(email)
//    }
//}
