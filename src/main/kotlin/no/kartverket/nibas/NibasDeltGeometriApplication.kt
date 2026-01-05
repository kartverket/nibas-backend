package no.kartverket.nibas

import no.kartverket.nibas.service.deltgeometri.DeltGeometriService
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Profile
import kotlin.system.exitProcess

@SpringBootApplication
@Profile("deltgeometri")
class NibasDeltGeometriApplication(val deltGeometriService: DeltGeometriService) : CommandLineRunner {

    override fun run(vararg args: String) {
        deltGeometriService.dannDeltGeometriForAdministrativeGrenser()
        exitProcess(0)
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) {
            SpringApplication.run(NibasDeltGeometriApplication::class.java, *args)
        }
    }
}

