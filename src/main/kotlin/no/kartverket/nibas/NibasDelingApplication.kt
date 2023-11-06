package no.kartverket.nibas

import no.kartverket.nibas.sosi.SosiImportService
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Profile
import kotlin.system.exitProcess

@SpringBootApplication
@Profile("deling")
class NibasDelingApplication(val sosiImportService: SosiImportService) : CommandLineRunner {

    override fun run(vararg args: String?) {
        sosiImportService.importNibas()
        exitProcess(0)
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) {
            SpringApplication.run(NibasDelingApplication::class.java, *args)
        }
    }
}

