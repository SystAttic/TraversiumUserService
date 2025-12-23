package travesium.userservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import
import traversium.commonmultitenancy.FlywayTenantMigration
import traversium.commonmultitenancy.MultiTenantAutoConfiguration

@SpringBootApplication(exclude = [KafkaAutoConfiguration::class])
@Import(MultiTenantAutoConfiguration::class, FlywayTenantMigration::class)
class UserServiceApplication

fun main(args: Array<String>) {
    runApplication<UserServiceApplication>(*args)
}
