package no.kartverket.nibas.vedtak2027

import no.kartverket.nibas.config.DummyAuthenticationToken
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.SpringApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Profile
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import kotlin.system.exitProcess

/**
 * Kjører [Vedtak2027JobService] når backend startes med profilen `vedtak2027`.
 **/
@Component
@Profile("vedtak2027")
class Vedtak2027JobRunner(
    private val jobService: Vedtak2027JobService,
    private val applicationContext: ApplicationContext,
) : ApplicationRunner {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        SecurityContextHolder.getContext().authentication = DummyAuthenticationToken(
            user = "vedtak2027-liabygda-job",
            credentials = Unit,
            authorities = listOf(SimpleGrantedAuthority("NIBAS")),
        )

        val plan = Vedtak2027Plan.LIABYGDA
        logger.info("Starter vedtak2027 for ${plan.kretsnavn} med virkningsdato ${plan.virkningsdato}")

        val exitCode = try {
            val rapport = jobService.flyttGrunnkretsOgStemmekrets(plan)
            logger.info("vedtak2027 ferdig, endringene er lagret:\n$rapport")
            0
        } catch (e: Vedtak2027GrunnlagException) {
            logger.error("vedtak2027 stoppet på en kontroll. Ingenting er lagret eller sendt til nibas-events.", e)
            1
        } catch (e: Throwable) {
            logger.error("vedtak2027 feilet. Databasen er rullet tilbake, men hendelsen kan være sendt til " +
                "nibas-events. Sjekk der før jobben kjøres på nytt.", e)
            1
        }

        exitProcess(SpringApplication.exit(applicationContext, { exitCode }))
    }
}
