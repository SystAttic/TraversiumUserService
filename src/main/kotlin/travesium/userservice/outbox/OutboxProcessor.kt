package travesium.userservice.outbox

import jakarta.persistence.EntityManager
import org.apache.logging.log4j.kotlin.logger
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import traversium.commonmultitenancy.TenantContext
import traversium.commonmultitenancy.TenantUtils

/**
 * @author Maja Razinger
 */
@Component
@ConditionalOnProperty(name = ["spring.kafka.bootstrap-servers"])
class OutboxProcessor(
    private val outboxService: OutboxService,
    private val outboxProperties: OutboxProperties,
    private val entityManager: EntityManager
) {

    @Scheduled(fixedDelayString = "#{outboxProperties.processor.interval}")
    fun processOutboxEvents() {
        val schemaNames = getAllTenantSchemas()

        schemaNames.forEach { schemaName ->
            try {
                setSchemaForTenant(schemaName)
                outboxService.processEventsForCurrentTenant()
            } catch (e: Exception) {
                logger.error(e) { "Error processing outbox events for schema $schemaName: ${e.message}" }
            } finally {
                TenantContext.clear()
            }
        }
    }

    @Scheduled(cron = "#{outboxProperties.cleanup.cron}")
    fun cleanupProcessedEvents() {
        val schemaNames = getAllTenantSchemas()
        val retentionHours = outboxProperties.cleanup.retention.hours

        schemaNames.forEach { schemaName ->
            try {
                setSchemaForTenant(schemaName)
                outboxService.cleanupEventsForCurrentTenant(retentionHours)
            } catch (e: Exception) {
                logger.error(e) { "Error cleaning up outbox events for schema $schemaName: ${e.message}" }
            } finally {
                TenantContext.clear()
            }
        }
    }

    private fun setSchemaForTenant(schemaName: String) {
        var rawTenantId = schemaName
        while (rawTenantId.startsWith("tenant_")) {
            rawTenantId = rawTenantId.removePrefix("tenant_")
        }

        TenantContext.setTenant(TenantUtils.sanitizeTenantIdForSchema(rawTenantId))
    }

    private fun getAllTenantSchemas(): List<String> {
        return try {
            val query = entityManager.createNativeQuery(
                """
                SELECT schema_name
                FROM information_schema.schemata
                WHERE schema_name NOT IN ('pg_catalog', 'information_schema')
                AND schema_name NOT LIKE 'pg_%'
                ORDER BY schema_name
                """
            )
            @Suppress("UNCHECKED_CAST")
            query.resultList as List<String>
        } catch (e: Exception) {
            logger.error(e) { "Error fetching tenant schemas: ${e.message}" }
            emptyList()
        }
    }
}