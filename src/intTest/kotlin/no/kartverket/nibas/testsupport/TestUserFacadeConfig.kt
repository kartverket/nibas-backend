package no.kartverket.nibas.testsupport

import no.kartverket.nibas.sporing.UserFacade
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class TestUserFacadeConfig {
    @Bean
    @Primary
    fun testUserFacade(): UserFacade = object : UserFacade {
        override fun getAuthenticatedUser(): String = "DUMMY"
    }
}


