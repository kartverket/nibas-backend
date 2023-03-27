package no.kartverket.nibas.sosi.sosiimport

import no.kartverket.nibas.domain.Flate
import no.kartverket.nibas.repository.FylkeRepository
import no.kartverket.nibas.repository.KommuneRepository
import no.kartverket.nibas.repository.NasjonRepository
import no.kartverket.nibas.repository.findAllGyldigPaaDato
import no.kartverket.nibas.service.FlateService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class SosiImportFlateService @Autowired constructor(
    private val kommuneRepository: KommuneRepository,
    private val fylkeRepository: FylkeRepository,
    private val nasjonRepository: NasjonRepository,
    private val flateService: FlateService
) {

    private val logger = LoggerFactory.getLogger(this.javaClass.name)

    @Transactional
    fun settFlaterPaaAlleAdministrativeEnheter(gyldighetsdato: LocalDate = LocalDate.now()): List<Result<Unit>> {
        val resultatForOppdateringAvKommuneflater = kommuneRepository.findAllGyldigPaaDato(gyldighetsdato).toList().map {
            wrapOppdateringIResult(it)
        }

        val resultatForOppdateringAvFylkesflater = fylkeRepository.findAllGyldigPaaDato(gyldighetsdato).toList().map {
            wrapOppdateringIResult(it)
        }

        val resultatForOppdateringAvNasjonsflater = nasjonRepository.findAllGyldigPaaDato(gyldighetsdato).toList().map {
            wrapOppdateringIResult(it)
        }

        return resultatForOppdateringAvFylkesflater + resultatForOppdateringAvKommuneflater + resultatForOppdateringAvNasjonsflater
    }

    private fun wrapOppdateringIResult(flate: Flate): Result<Unit> {
        return try {
            flateService.oppdaterFlaterFor(setOf(flate))
            Result.success(Unit)
        } catch (ex: Exception) {
            logger.warn("Klarte ikke å oppdatere flate", ex)
            Result.failure(ex)
        }
    }

}
