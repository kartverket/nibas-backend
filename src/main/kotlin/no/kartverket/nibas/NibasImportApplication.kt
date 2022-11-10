package no.kartverket.nibas

import no.kartverket.nibas.sosi.SosiImportService
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.SpringApplication.exit
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Profile
import kotlin.system.exitProcess

@SpringBootApplication
@Profile("import")
class NibasImportApplication(val sosiImportService: SosiImportService) : CommandLineRunner {

    override fun run(vararg args: String?) {
        sosiImportService.importNibas()
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) {
            exitProcess(exit(SpringApplication.run(NibasImportApplication::class.java, *args)))
        }
    }
}

