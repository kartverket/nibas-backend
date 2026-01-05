package no.kartverket.nibas

import no.kartverket.nibas.service.administrativenhetnavn.OppdaterFylkesnavnService
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Profile
import kotlin.system.exitProcess

@SpringBootApplication
@Profile("oppdaterfylkesnavn")
class NibasOppdaterFylkesnavnApplication(val oppdaterFylkesnavnService: OppdaterFylkesnavnService) : CommandLineRunner {

    override fun run(vararg args: String) {
        oppdaterFylkesnavnService.samiskeNavnForInnlandetOgNordland()
        exitProcess(0)
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) {
            SpringApplication.run(NibasOppdaterFylkesnavnApplication::class.java, *args)
        }
    }
}

