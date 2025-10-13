package no.kartverket.nibas.service.autosync

import no.kartverket.nibas.domain.utkast.UtkastStatus
import no.kartverket.nibas.service.utkast.UtkastService
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.RestController

@RestController
class CreateRettetUtkastController(
    private val createRettetUtkastService: CreateRettetUtkastService,
    private val utkastService: UtkastService
) : CreateRettetUtkastApi {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun genererRettetUtkast(antall: Int): String {
        createRettetUtkastService.autoFixAvvik(antall)
        return "Done! Check logs to see all steps."
    }

    override fun publiserAlleUtkast(): String {
        log.info("=== Publishing all OPPRETTET utkast ===")

        val opprettetUtkast = utkastService.hentUtkast(listOf(UtkastStatus.OPPRETTET), null)
        log.info("Found {} OPPRETTET utkast", opprettetUtkast.size)

        var successCount = 0
        var failCount = 0

        for (utkast in opprettetUtkast) {
            try {
                log.info("Publishing utkast: {} ({})", utkast.id, utkast.navn)
                utkastService.publiserUtkast(utkast.id)
                log.info("  SUCCESS")
                successCount++
            } catch (e: Exception) {
                log.error("  FAILED: {}", e.message)
                failCount++
            }
        }

        log.info("=== DONE ===")
        log.info("  Success: {}", successCount)
        log.info("  Failed: {}", failCount)

        return "Published: $successCount, Failed: $failCount"
    }
}
