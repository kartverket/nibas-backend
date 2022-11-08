package no.kartverket.nibas.testcontainer

import no.kartverket.nibas.repository.GrenseRepository
import no.kartverket.nibas.repository.GrunnkretsRepository
import no.kartverket.nibas.repository.SpraakkodeRepository
import no.kartverket.nibas.repository.UtkastRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import javax.sql.DataSource

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
class TestWithDbContainer {

    @Autowired
    lateinit var grunnkretsRepository: GrunnkretsRepository
    @Autowired
    lateinit var utkastRepository: UtkastRepository
    @Autowired
    lateinit var spraakkodeRepository: SpraakkodeRepository
    @Autowired
    lateinit var grenseRepository: GrenseRepository
    @Autowired
    lateinit var dataSource: DataSource

    companion object dbContainer : PostgreSQLContainer<dbContainer>(
        DockerImageName.parse("postgis/postgis:14-3.2-alpine").asCompatibleSubstituteFor("postgres")
    ) {
        init {
            withDatabaseName("nibas")
            withUsername("nibas")
            withPassword("nibas")
            withCreateContainerCmdModifier { cmd -> cmd.withName("nibas-test-container") }
            start()
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
