package no.kartverket.nibas

import no.kartverket.nibas.service.avrundkoordinater.AvrundKoordinaterService
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Profile
import kotlin.system.exitProcess

@SpringBootApplication
@Profile("avrundkoordinater")
class NibasAvrundKoordinaterApplication(val avrundKoordinaterService: AvrundKoordinaterService) : CommandLineRunner {

    override fun run(vararg args: String?) {
        avrundKoordinaterService.avrundKoordinaterForAlleGrenser()
        exitProcess(0)
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) {
            SpringApplication.run(NibasAvrundKoordinaterApplication::class.java, *args)
        }
    }
}

