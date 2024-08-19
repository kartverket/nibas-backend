package no.kartverket.nibas

import no.kartverket.nibas.vedtak2025.Vedtak2025JobService
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Profile
import kotlin.system.exitProcess

@SpringBootApplication
@Profile("vedtak2025")
class NibasVedtak2025Application (val vedtak2025JobService: Vedtak2025JobService) : CommandLineRunner{

    override fun run(vararg args: String?) {
        vedtak2025JobService.setupDummyUser()
        vedtak2025JobService.doVedtakt2025Job()
        exitProcess(0)
    }
    companion object {
        @JvmStatic fun main(args: Array<String>) {
            SpringApplication.run(NibasVedtak2025Application::class.java, *args)
        }
    }
}
