package travesium.userservice.security

import org.hibernate.engine.jdbc.connections.internal.DatasourceConnectionProviderImpl
import org.hibernate.engine.jdbc.connections.spi.AbstractMultiTenantConnectionProvider
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import javax.sql.DataSource

/**
 * Test configuration that disables multi-tenancy for H2 tests
 * @author Maja Razinger
 */
@TestConfiguration
class TestMultitenancyConfig {

    @Bean
    @Primary
    fun testMultiTenantConnectionProvider(dataSource: DataSource) =
        object : AbstractMultiTenantConnectionProvider<String>() {

            private val connectionProvider = DatasourceConnectionProviderImpl().apply {
                configure(mapOf("hibernate.connection.datasource" to dataSource))
            }

            override fun getAnyConnectionProvider(): ConnectionProvider {
                return connectionProvider
            }

            override fun selectConnectionProvider(tenantIdentifier: String?): ConnectionProvider {
                return connectionProvider
            }
        }
}