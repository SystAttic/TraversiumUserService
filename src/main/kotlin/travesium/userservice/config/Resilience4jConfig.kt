package travesium.userservice.config

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.retry.RetryRegistry
import jakarta.annotation.PostConstruct
import org.apache.logging.log4j.kotlin.logger
import org.springframework.context.annotation.Configuration

@Configuration
class Resilience4jConfig(
    private val circuitBreakerRegistry: CircuitBreakerRegistry,
    private val retryRegistry: RetryRegistry
) {

    @PostConstruct
    fun registerEventListeners() {
        retryRegistry.retry("tripServiceGrpc").eventPublisher
            .onRetry { event ->
                logger.warn("RETRY ATTEMPT #${event.numberOfRetryAttempts} for tripServiceGrpc")
            }
            .onSuccess { event ->
                logger.info("RETRY SUCCESSFUL after ${event.numberOfRetryAttempts} attempts")
            }
            .onError { event ->
                logger.error("RETRY FAILED after ${event.numberOfRetryAttempts} attempts: ${event.lastThrowable!!.message}")
            }

        circuitBreakerRegistry.circuitBreaker("tripServiceGrpc").eventPublisher
            .onStateTransition { event ->
                logger.warn("CIRCUIT BREAKER STATE CHANGE: ${event.stateTransition.fromState} -> ${event.stateTransition.toState}")
            }
            .onFailureRateExceeded { event ->
                logger.error("CIRCUIT BREAKER failure rate exceeded: ${event.failureRate}%")
            }
            .onCallNotPermitted { event ->
                logger.error("CIRCUIT BREAKER OPEN - call not permitted")
            }

        retryRegistry.retry("moderationServiceGrpc").eventPublisher
            .onRetry { event ->
                logger.warn("RETRY ATTEMPT #${event.numberOfRetryAttempts} for moderationServiceGrpc")
            }
            .onSuccess { event ->
                logger.info("RETRY SUCCESSFUL after ${event.numberOfRetryAttempts} attempts for moderationServiceGrpc")
            }
            .onError { event ->
                logger.error("RETRY FAILED after ${event.numberOfRetryAttempts} attempts for moderationServiceGrpc: ${event.lastThrowable!!.message}")
            }

        circuitBreakerRegistry.circuitBreaker("moderationServiceGrpc").eventPublisher
            .onStateTransition { event ->
                logger.warn("CIRCUIT BREAKER STATE CHANGE for moderationServiceGrpc: ${event.stateTransition.fromState} -> ${event.stateTransition.toState}")
            }
            .onFailureRateExceeded { event ->
                logger.error("CIRCUIT BREAKER failure rate exceeded for moderationServiceGrpc: ${event.failureRate}%")
            }
            .onCallNotPermitted { event ->
                logger.error("CIRCUIT BREAKER OPEN for moderationServiceGrpc - call not permitted")
            }
    }
}
