package no.kartverket.nibas

import no.kartverket.nibas.config.DummyAuthenticationToken
import no.kartverket.nibas.service.bopliktgrensetransformasjon.BopliktgrenserTransformasjonService
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Profile
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import kotlin.system.exitProcess

@SpringBootApplication
@Profile("bopliktgrensertransformasjon")
class NibasBopliktgrenserTransformasjonApplication(val bopliktgrenserTransformasjonService: BopliktgrenserTransformasjonService) : CommandLineRunner {

    override fun run(vararg args: String?) {
        // Denne applikasjonen kjøres uten av systemet, så ingen http-filtre setter SecurityContext.
        // Vi trenger likevel en autentisert bruker for sporing.
        SecurityContextHolder.getContext().authentication = DummyAuthenticationToken(
            user = "bopliktgrenserapplication",
            credentials = "bopliktgrenserapplication",
            authorities = listOf(SimpleGrantedAuthority("NIBAS"))
        )

        bopliktgrenserTransformasjonService.transform()
        exitProcess(0)
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) {
            SpringApplication.run(NibasBopliktgrenserTransformasjonApplication::class.java, *args)
        }
    }
}
