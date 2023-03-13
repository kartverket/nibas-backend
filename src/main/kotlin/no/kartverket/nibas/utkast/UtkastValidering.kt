package no.kartverket.nibas.utkast

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.api.v1.common.Operasjoner
import no.kartverket.nibas.api.v1.common.geojson.Feature
import no.kartverket.nibas.common.CurrentTime
import no.kartverket.nibas.common.FeilVedOppdatering
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException
import no.kartverket.nibas.domain.utkast.Utkast
import no.kartverket.nibas.domain.utkast.UtkastStatus
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.service.GrunnkretsService
import no.kartverket.nibas.service.StemmekretsService
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class UtkastValidatorService(
    val grunnkretsService: GrunnkretsService,
    val stemmekretsService: StemmekretsService,
    val kodelisteService: KodelisteService,
    val currentTime: CurrentTime
) {

    fun validateGyldigeOperasjoner(operasjoner: Operasjoner): OperasjonerValidering {
        val endredeFeatures: Map<String, Feature> = operasjoner.grenseendringer.endredeFeatures

        val featuresSomIkkeErEditerbare = endredeFeatures.filterNot { Grensetype.valueOf(it.value.properties.type).editerbar }
        if (featuresSomIkkeErEditerbare.isNotEmpty()) {
            return OperasjonerValidering.UgyldigeOperasjoner(operasjoner, ResponseException.BadRequestException("Utkast inneholder ${featuresSomIkkeErEditerbare.size} ikke-redigerbare grenser"))
        }

        val kretsinfoValideringsfeil = validerKretsinfoEndringer(operasjoner)
        if (kretsinfoValideringsfeil != null) return kretsinfoValideringsfeil

        return OperasjonerValidering.GyldigeOperasjoner(operasjoner)
    }

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

        val operasjonerValideringResult = validateGyldigeOperasjoner(utkast.operasjoner!!)
        if (operasjonerValideringResult is OperasjonerValidering.UgyldigeOperasjoner) {
            return UtkastValidering.UgyldigUtkast(utkast, operasjonerValideringResult.feil)
        }

        val grunnkretsValideringsFeil = validerGrunnkretsEndringerKonflikt(utkast)
        if (grunnkretsValideringsFeil != null) {
            return UtkastValidering.UgyldigUtkast(utkast, grunnkretsValideringsFeil)
        }

        val stemmekretsValideringsFeil = validerStemmekretsEndringerKonflikt(utkast)
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

    fun validerKretsinfoEndringer(
        operasjoner: Operasjoner
    ): OperasjonerValidering.UgyldigeOperasjoner? {
        val harUgyldigMetadataendringPaaGrunnkrets =
            operasjoner.metadataendringer.grunnkretsendringer.values.any {
                isMetadataendringUgyldig(
                    Flatetype.GRUNNKRETS,
                    it.navn,
                    it.grunnkretsnummer,
                    it.identifikasjon.lokalid
                )
            }
        if (harUgyldigMetadataendringPaaGrunnkrets)
            return OperasjonerValidering.UgyldigeOperasjoner(
                operasjoner,
                ResponseException.BadRequestException(
                    "Utkast inneholder ugyldige metadataendringer på grunnkrets"
                )
            )

        val harUgyldigMetadataendringPaaStemmekrets =
            operasjoner.metadataendringer.stemmekretsendringer.values.any {
                isMetadataendringUgyldig(
                    Flatetype.STEMMEKRETS,
                    it.stemmekretsnavn,
                    it.stemmekretsnummer,
                    it.identifikasjon.lokalid
                )
            }
        if (harUgyldigMetadataendringPaaStemmekrets)
            return OperasjonerValidering.UgyldigeOperasjoner(
                operasjoner,
                ResponseException.BadRequestException(
                    "Utkast inneholder ugyldige metadataendringer på stemmekrets"
                )
            )

        val harUgyldigMetadataendringPaaStemmekretsSammenslaaing =
            if (operasjoner.stemmekretsSammenslaaingsendring != null)
                isMetadataendringUgyldig(
                    Flatetype.STEMMEKRETS,
                    operasjoner.stemmekretsSammenslaaingsendring.stemmekretsNavn,
                    operasjoner.stemmekretsSammenslaaingsendring.stemmekretsNummer,
                    operasjoner.stemmekretsSammenslaaingsendring.viderefoertStemmekrets.lokalId
                )
            else false
        if (harUgyldigMetadataendringPaaStemmekretsSammenslaaing)
            return OperasjonerValidering.UgyldigeOperasjoner(
                operasjoner,
                ResponseException.BadRequestException(
                    "Utkast inneholder ugyldige metadataendringer på sammenslåing"
                )
            )

        return null
    }

    private fun validerGrunnkretsEndringerKonflikt(utkast: Utkast): FeilVedOppdatering.OppdateringException? {
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

    private fun validerStemmekretsEndringerKonflikt(utkast: Utkast): FeilVedOppdatering.OppdateringException? {
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


    /** Tenkt som generisk metadataendring-sjekker,
     * men fungerer foreløpig kun for grunnkretser og stemmekretser **/
    private fun isMetadataendringUgyldig(flatetype: Flatetype, navn: String?, kretsnummer: String?, lokalid: String): Boolean {
        if (isUgyldigKretsnummerEllerNavn(kretsnummer, navn)) {
            return true
        }

        if (flatetype == Flatetype.GRUNNKRETS && isUgyldigGrunnkretsnummer(kretsnummer, lokalid)) {
            return true
        }

        if (flatetype == Flatetype.STEMMEKRETS && isUgyldigStemmekretsnummer(kretsnummer)) {
            return true
        }

        return false
    }
    private fun isUgyldigKretsnummerEllerNavn(kretsnummer: String?, kretsnavn: String?): Boolean {
        val numeriskKretsnummer = kretsnummer?.toIntOrNull()
        if (numeriskKretsnummer == null || numeriskKretsnummer == 0) {
            return true
        }

        return kretsnavn.isNullOrBlank()
    }

    private fun isUgyldigGrunnkretsnummer(nummer: String?, lokalid: String): Boolean {
        if (nummer?.length != 8) return true

        val grunnkrets =
            grunnkretsService.hentGrunnkrets(
                Identifikator(lokalid = Lokalid(lokalid), gyldighetsdato = LocalDate.now())
            )

        val kommunenummerUUID = grunnkrets?.kommune?.kommunenummer
        if (kommunenummerUUID.isNullOrBlank()) return true

        val kommunenummer = kodelisteService.getCodeValueByKommunenummerId(kommunenummerUUID)
        return nummer.take(4) != kommunenummer
    }

    private fun isUgyldigStemmekretsnummer(nummer: String?): Boolean {
        return nummer?.length!! > 4;
    }

}

fun Operasjoner.harEndringer(): Boolean {
    val metadata = this.metadataendringer
    return metadata.nasjonsendringer.isNotEmpty() ||
        metadata.fylkesendringer.isNotEmpty() ||
        metadata.kommuneendringer.isNotEmpty() ||
        metadata.grunnkretsendringer.isNotEmpty() ||
        metadata.stemmekretsendringer.isNotEmpty() ||
        this.grenseendringer.endredeFeatures.isNotEmpty() ||
        this.stemmekretsSammenslaaingsendring != null
}

fun Operasjoner.harIkkeEndringer(): Boolean = !this.harEndringer()

sealed class UtkastValidering {
    data class KlartForPublisering(val utkast: Utkast): UtkastValidering()
    data class UgyldigUtkast(val utkast: Utkast, val feil: Exception): UtkastValidering()
}

sealed class OperasjonerValidering {
    data class GyldigeOperasjoner(val operasjoner: Operasjoner): OperasjonerValidering()
    data class UgyldigeOperasjoner(val operasjoner: Operasjoner, val feil: Exception): OperasjonerValidering()
}
