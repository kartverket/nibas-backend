package no.kartverket.nibas.utkast

import no.kartverket.nibas.api.v1.common.Operasjoner
import no.kartverket.nibas.common.CurrentTime
import no.kartverket.nibas.common.FeilVedOppdatering
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException
import no.kartverket.nibas.domain.utkast.Utkast
import no.kartverket.nibas.domain.utkast.UtkastStatus
import no.kartverket.nibas.service.GrunnkretsService
import no.kartverket.nibas.service.StemmekretsService
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class UtkastValidatorService(
    val grunnkretsService: GrunnkretsService,
    val stemmekretsService: StemmekretsService,
    val currentTime: CurrentTime
) {
    fun validateUtkastKlartForPublisering(utkast: Utkast): UtkastValidering {
        if (utkast.status != UtkastStatus.OPPRETTET) {
            return UtkastValidering.UgyldigUtkast(utkast, ResponseException.BadRequestException("Kan ikke publisere et utkast i status ${utkast.status}."))
        }

        if (utkast.gyldigFra.isBefore(currentTime.asLocalDate())) {
            return UtkastValidering.UgyldigUtkast(utkast, ResponseException.BadRequestException("Kan ikke publisere et utkast som har gyldigFra-dato i fortiden. GyldigFra er satt til: ${utkast.gyldigFra}"))
        }

        val utkastIkkeHarEndringer = utkast.operasjoner?.harIkkeEndringer() ?: true
        if (utkastIkkeHarEndringer) {
            return UtkastValidering.UgyldigUtkast(utkast, ResponseException.BadRequestException("Kan ikke publisere et utkast uten endringer."))
        }

        val grunnkretsValideringsFeil = validerGrunnkretsEndringer(utkast)
        if (grunnkretsValideringsFeil != null) {
            return UtkastValidering.UgyldigUtkast(utkast, grunnkretsValideringsFeil)
        }

        val stemmekretsValideringsFeil = validerStemmekretsEndringer(utkast)
        if (stemmekretsValideringsFeil != null) {
            return UtkastValidering.UgyldigUtkast(utkast, stemmekretsValideringsFeil)
        }

        return UtkastValidering.KlartForPublisering(utkast)
    }

    fun datoErEtterSisteGyldigFraDatoForGrunnkrets(lokalid: String, dato: LocalDate): Boolean {
        val sisteGyldigeGrunnkrets = grunnkretsService.hentSisteGyldigeGrunnkrets(lokalid)
            ?: throw ResponseException.NotFoundException("Fant ikke siste gyldige grunnkrets for lokalid: $lokalid")
        return dato.isAfter(sisteGyldigeGrunnkrets.gyldighet.gyldigFra)
    }

    fun datoErEtterSisteGyldigFraDatoForStemmekrets(lokalid: String, dato: LocalDate): Boolean {
        val sisteGyldigeStemmekrets = stemmekretsService.hentSisteGyldigeStemmekrets(lokalid)
            ?: throw ResponseException.NotFoundException("Fant ikke siste gyldige stemmekrets for lokalid: $lokalid")
        return dato.isAfter(sisteGyldigeStemmekrets.gyldighet.gyldigFra)
    }

    private fun validerGrunnkretsEndringer(utkast: Utkast): FeilVedOppdatering.OppdateringException? {
        return utkast.operasjoner?.metadataendringer?.grunnkretsendringer?.firstNotNullOfOrNull { grunnkretsendring ->
            val senerePubliserteEndringer = grunnkretsService.hentAlleVersjonerMedGyldigFraEtterGittDato(lokalId = Lokalid(grunnkretsendring.key), dato = utkast.gyldigFra)

            if (senerePubliserteEndringer.isNotEmpty()) {
                FeilVedOppdatering.OppdateringException(FeilVedOppdatering.OppdateringInfo(
                    id = Identifikator(Lokalid(grunnkretsendring.key), utkast.gyldigFra),
                    type = FeilVedOppdatering.Type.GRUNNKRETS,
                    affectedIds = senerePubliserteEndringer.map { Identifikator(Lokalid(it.lokalid), it.gyldighet.gyldigFra) }
                ))
            } else {
                null
            }
        }
    }

    private fun validerStemmekretsEndringer(utkast: Utkast): FeilVedOppdatering.OppdateringException? {
        return utkast.operasjoner?.metadataendringer?.stemmekretsendringer?.firstNotNullOfOrNull { stemmekretsendring ->
            val senerePubliserteEndringer = stemmekretsService.hentAlleVersjonerMedGyldigFraEtterGittDato(lokalId = Lokalid(stemmekretsendring.key), dato = utkast.gyldigFra)

            if (senerePubliserteEndringer.isNotEmpty()) {
                FeilVedOppdatering.OppdateringException(FeilVedOppdatering.OppdateringInfo(
                    id = Identifikator(Lokalid(stemmekretsendring.key), utkast.gyldigFra),
                    type = FeilVedOppdatering.Type.STEMMEKRETS,
                    affectedIds = senerePubliserteEndringer.map { Identifikator(Lokalid(it.lokalid), it.gyldighet.gyldigFra) }
                ))
            } else {
                null
            }
        }
    }

}

fun Operasjoner.harEndringer(): Boolean {
    val metadata = this.metadataendringer
    return metadata.nasjonsendringer.isNotEmpty() ||
        metadata.fylkesendringer.isNotEmpty() ||
        metadata.kommuneendringer.isNotEmpty() ||
        metadata.grunnkretsendringer.isNotEmpty() ||
        metadata.stemmekretsendringer.isNotEmpty() ||
        this.grenseendringer.endredeFeatures.isNotEmpty()
}

fun Operasjoner.harIkkeEndringer(): Boolean = !this.harEndringer()

sealed class UtkastValidering {
    data class KlartForPublisering(val utkast: Utkast): UtkastValidering()
    data class UgyldigUtkast(val utkast: Utkast, val feil: Exception): UtkastValidering()
}
