package no.kartverket.nibas

import no.kartverket.nibas.kommuneogfylkesdeling.KommuneOgFylkesDelingJob
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Profile
import kotlin.system.exitProcess

@SpringBootApplication
@Profile("deling")
class NibasDelingApplication(val kommuneOgFylkesDelingJob: KommuneOgFylkesDelingJob) : CommandLineRunner {

    override fun run(vararg args: String?) {
        kommuneOgFylkesDelingJob.delFylkerOgKommuner()
        exitProcess(0)
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) {
            SpringApplication.run(NibasDelingApplication::class.java, *args)
        }
    }
}

