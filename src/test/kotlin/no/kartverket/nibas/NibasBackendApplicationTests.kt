package no.kartverket.nibas

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("unittest")
@Import(TestConfiguration::class)
class NibasBackendApplicationTests {

	@Test
    @Suppress("EmptyFunctionBlock")
	fun contextLoads() {
	}

}
