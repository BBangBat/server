package com.bbangbat.store.support

import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Import
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@DataJpaTest(properties = ["spring.jpa.hibernate.ddl-auto=create-drop"])
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JdslTestConfiguration::class)
@Testcontainers
abstract class AbstractContainerBaseTest {
    companion object {
        @Container
        @ServiceConnection
        @JvmField
        val mysql: MySQLContainer<*> =
            MySQLContainer("mysql:8.4")
                .withTmpFs(mapOf("/var/lib/mysql" to "rw"))
                .withUrlParam("allowPublicKeyRetrieval", "true")
                .withUrlParam("useSSL", "false")
    }
}
