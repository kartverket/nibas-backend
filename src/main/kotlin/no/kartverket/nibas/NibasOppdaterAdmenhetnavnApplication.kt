package no.kartverket.nibas

import no.kartverket.nibas.service.administrativenhetnavn.OppdaterAdmenhetnavnService
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Profile
import kotlin.system.exitProcess

@SpringBootApplication
@Profile("oppdaterAdmenhetnavn")
class NibasOppdaterAdmenhetnavnApplication(val oppdaterAdmenhetnavnService: OppdaterAdmenhetnavnService) : CommandLineRunner {

    override fun run(vararg args: String?) {
        oppdaterAdmenhetnavnService.oppdaterAdministrativenhetnavn()
        exitProcess(0)
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) {
            SpringApplication.run(NibasOppdaterAdmenhetnavnApplication::class.java, *args)
        }
    }
}

