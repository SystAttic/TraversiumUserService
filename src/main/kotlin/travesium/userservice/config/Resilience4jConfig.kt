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
        retryRegistry.allRetries.forEach { retry ->
            registerRetryListeners(retry.name)
        }

        circuitBreakerRegistry.allCircuitBreakers.forEach { circuitBreaker ->
            registerCircuitBreakerListeners(circuitBreaker.name)
        }
    }

    private fun registerRetryListeners(name: String) {
        retryRegistry.retry(name).eventPublisher
            .onRetry { event ->
                logger.warn("RETRY ATTEMPT #${event.numberOfRetryAttempts} for $name")
            }
            .onSuccess { event ->
                logger.info("RETRY SUCCESSFUL after ${event.numberOfRetryAttempts} attempts for $name")
            }
            .onError { event ->
                logger.error("RETRY FAILED after ${event.numberOfRetryAttempts} attempts for $name: ${event.lastThrowable!!.message}")
            }
    }

    private fun registerCircuitBreakerListeners(name: String) {
        circuitBreakerRegistry.circuitBreaker(name).eventPublisher
            .onStateTransition { event ->
                logger.warn("CIRCUIT BREAKER STATE CHANGE for $name: ${event.stateTransition.fromState} -> ${event.stateTransition.toState}")
            }
            .onFailureRateExceeded { event ->
                logger.error("CIRCUIT BREAKER failure rate exceeded for $name: ${event.failureRate}%")
            }
            .onCallNotPermitted { event ->
                logger.error("CIRCUIT BREAKER OPEN for $name - call not permitted")
            }
    }
}
