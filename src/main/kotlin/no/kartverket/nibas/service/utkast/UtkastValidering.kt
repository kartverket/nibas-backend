package no.kartverket.nibas.service.utkast

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.api.v1.common.Operasjoner
import no.kartverket.nibas.api.v1.common.geojson.Feature
import no.kartverket.nibas.api.v1.common.getUnikeKretsDelingEndringRequests
import no.kartverket.nibas.common.CurrentTime
import no.kartverket.nibas.common.FeilVedOppdatering
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException
import no.kartverket.nibas.domain.utkast.Utkast
import no.kartverket.nibas.domain.utkast.UtkastStatus
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.service.GrenseService
import no.kartverket.nibas.service.GrunnkretsService
import no.kartverket.nibas.service.StemmekretsService
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class UtkastValidatorService(
    val grunnkretsService: GrunnkretsService,
    val grenseService: GrenseService,
    val stemmekretsService: StemmekretsService,
    val kodelisteService: KodelisteService,
    val currentTime: CurrentTime
) {

    fun validateGyldigeOperasjoner(operasjoner: Operasjoner): OperasjonerValidering {
        val endredeFeatures: Map<String, Feature> = operasjoner.grenseendringer.endredeFeatures

        val featuresSomIkkeErEditerbare = endredeFeatures.filterNot { Grensetype.fromValue(it.value.properties.type).editerbar }
        if (featuresSomIkkeErEditerbare.isNotEmpty()) {
            return OperasjonerValidering.UgyldigeOperasjoner(operasjoner, ResponseException.BadRequestException("Utkast inneholder ${featuresSomIkkeErEditerbare.size} ikke-redigerbare grenser"))
        }

        val kretsinfoValideringsfeil = validerKretsinfoEndringer(operasjoner)
        if (kretsinfoValideringsfeil != null) return kretsinfoValideringsfeil

        return OperasjonerValidering.GyldigeOperasjoner(operasjoner)
    }

    fun validateUtkastKlartForPublisering(utkast: Utkast): UtkastValidering {
        if (utkast.status != UtkastStatus.OPPRETTET) {
            return UtkastValidering.UgyldigUtkast(utkast, ResponseException.BadRequestException(
                title = "Kan ikke publisere utkastet",
                msg = "Kan ikke publisere et utkast i status ${utkast.status}."
            ))
        }

        if (utkast.gyldigFra.isBefore(currentTime.asLocalDate())) {
            return UtkastValidering.UgyldigUtkast(utkast, ResponseException.BadRequestException(
                title = "Kan ikke publisere utkastet",
                msg = "Kan ikke publisere et utkast som trer i kraft før dagens dato. Utkastet er satt til å tre i kraft ${utkast.gyldigFra}"
            ))
        }

        val utkastIkkeHarEndringer = utkast.operasjoner?.harIkkeEndringer() ?: true
        if (utkastIkkeHarEndringer) {
            return UtkastValidering.UgyldigUtkast(utkast, ResponseException.BadRequestException(
                title = "Kan ikke publisere utkastet",
                msg = "Kan ikke publisere et utkast uten endringer."
            ))
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

        val grenseeendringerValideringsFeil = validerGrenseEndringerKonflikt(utkast)
        if (grenseeendringerValideringsFeil != null) {
            return UtkastValidering.UgyldigUtkast(utkast, grenseeendringerValideringsFeil)
        }

        return UtkastValidering.KlartForPublisering(utkast)
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

        val harUgyldigMetadataendringPaaKretsDeling = getUnikeKretsDelingEndringRequests(operasjoner.kretsDelingEndringer)
                .any {
                    isMetadataendringUgyldig(
                        it.flatetype,
                        it.kretsNavn,
                        it.kretsNummer,
                        it.opprinneligKrets.lokalId
                    )
                }

        if (harUgyldigMetadataendringPaaKretsDeling){
            return OperasjonerValidering.UgyldigeOperasjoner(
                operasjoner,
                ResponseException.BadRequestException(
                    "Utkast inneholder ugyldig metadataendringer på deling"
            ))
        }

        return null
    }

    private fun validerGrenseEndringerKonflikt(utkast: Utkast): FeilVedOppdatering.OppdateringException? {
        return utkast.operasjoner?.grenseendringer?.endredeFeatures?.keys?.firstNotNullOfOrNull { id ->
            val senerePubliserteGrenser = grenseService.hentAlleVersjonerMedGyldigFraEtterGittDato(id, utkast.gyldigFra)

            if (senerePubliserteGrenser.isNotEmpty()) {
                FeilVedOppdatering.OppdateringException(FeilVedOppdatering.OppdateringInfo(
                    id = Identifikator(Lokalid(id), utkast.gyldigFra),
                    type = FeilVedOppdatering.Type.GRENSE,
                    affectedIds = senerePubliserteGrenser.map { Identifikator(Lokalid(it.lokalid), it.gyldighet.gyldigFra) }
                ))
            } else {
                null
            }
        }
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
    internal fun isMetadataendringUgyldig(flatetype: Flatetype, navn: String?, kretsnummer: String?, lokalid: String): Boolean {
        if (navn == null || kretsnummer == null) {
            return true
        }

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
    private fun isUgyldigKretsnummerEllerNavn(kretsnummer: String, kretsnavn: String): Boolean {
        val numeriskKretsnummer = kretsnummer.toIntOrNull()
        if (numeriskKretsnummer == null || numeriskKretsnummer <= 0) {
            return true
        }

        return kretsnavn.isBlank()
    }

    private fun isUgyldigGrunnkretsnummer(nummer: String, lokalid: String): Boolean {
        if (nummer.length != 8) return true

        val grunnkrets = grunnkretsService.hentGrunnkrets(Identifikator(lokalid = Lokalid(lokalid), gyldighetsdato = LocalDate.now()))

        val kommunenummer = grunnkrets?.getKommune(grunnkrets.gyldighet.gyldigFra)?.kommunenummer
        if (kommunenummer.isNullOrBlank()) return true

        return nummer.take(4) != kommunenummer
    }

    private fun isUgyldigStemmekretsnummer(nummer: String): Boolean {
        return nummer.length > 4
    }
}

private fun Operasjoner.harEndringer(): Boolean {
    val metadata = this.metadataendringer
    return metadata.nasjonsendringer.isNotEmpty() ||
        metadata.fylkesendringer.isNotEmpty() ||
        metadata.kommuneendringer.isNotEmpty() ||
        metadata.grunnkretsendringer.isNotEmpty() ||
        metadata.stemmekretsendringer.isNotEmpty() ||
        this.grenseendringer.endredeFeatures.isNotEmpty() ||
        this.stemmekretsSammenslaaingsendring != null ||
        this.kretsDelingEndringer.isNotEmpty()
}

private fun Operasjoner.harIkkeEndringer(): Boolean = !this.harEndringer()

sealed class UtkastValidering {
    data class KlartForPublisering(val utkast: Utkast): UtkastValidering()
    data class UgyldigUtkast(val utkast: Utkast, val feil: Exception): UtkastValidering()
}

sealed class OperasjonerValidering {
    data class GyldigeOperasjoner(val operasjoner: Operasjoner): OperasjonerValidering()
    data class UgyldigeOperasjoner(val operasjoner: Operasjoner, val feil: Exception): OperasjonerValidering()
}
