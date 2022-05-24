package no.kartverket.nibas.testcontainer

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@Testcontainers
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
class TestWithDbContainer {
    companion object {
        val IMAGE_VERSION = "postgis/postgis:13-3.1-alpine"

        @Container
        val dbContainer = PostgreSQLContainer<Nothing>(
            DockerImageName.parse(IMAGE_VERSION).asCompatibleSubstituteFor("postgres")
        ).apply {
            withDatabaseName("postgres")
            withUsername("nibas")
            withPassword("nibas")
        }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", dbContainer::getJdbcUrl)
            registry.add("spring.datasource.username", dbContainer::getUsername)
            registry.add("spring.datasource.password", dbContainer::getPassword)
        }
    }
}
