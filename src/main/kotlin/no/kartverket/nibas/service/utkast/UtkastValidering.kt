package no.kartverket.nibas.service.utkast

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.api.v1.common.KontekstEnhetType
import no.kartverket.nibas.api.v1.common.Operasjoner
import no.kartverket.nibas.api.v1.common.UnikKretsDelingEndring
import no.kartverket.nibas.api.v1.common.geojson.Feature
import no.kartverket.nibas.api.v1.common.getUnikeKretsDelingEndringRequests
import no.kartverket.nibas.api.v1.request.KretsDelingEndringRequest
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
        val endredeFeatures: List<Feature> = operasjoner.grenseendringer.endredeFeatures
        val featuresSomGjentas = endredeFeatures.filter { it.id != null }.groupBy { it.id }.filterValues { it.size > 1 }
        if (featuresSomGjentas.isNotEmpty()) {
            return OperasjonerValidering.UgyldigeOperasjoner(operasjoner, ResponseException.BadRequestException("Utkast inneholder grenseendringer for samme grense(r) flere ganger. Grensene (representert ved lokalid) det gjelder er: ${featuresSomGjentas.map { it.key }.sortedBy { it }}"))
        }

        val featuresSomIkkeErEditerbare = endredeFeatures.filterNot { Grensetype.fromValue(it.properties.type).editerbar }
        if (featuresSomIkkeErEditerbare.isNotEmpty()) {
            return OperasjonerValidering.UgyldigeOperasjoner(operasjoner, ResponseException.BadRequestException("Utkast inneholder ${featuresSomIkkeErEditerbare.size} ikke-redigerbare grenser"))
        }

        val kretsinfoValideringsfeil = validerKretsinfoEndringer(operasjoner)
        if (kretsinfoValideringsfeil != null) return kretsinfoValideringsfeil

        return OperasjonerValidering.GyldigeOperasjoner(operasjoner)
    }

    fun sjekkUgyldigUtkastNavnOgEndringstype(navn: String, endringstype: String): Boolean {
        //Sjekker om navn og endringstype er ugyldige
        return (navn.isBlank() || endringstype.isBlank())
    }

    fun validateGyldigUtkast(utkast: Utkast): UtkastValidering {
        if (sjekkUgyldigUtkastNavnOgEndringstype(utkast.navn, utkast.auditInfo.typeEndring.value)) {
            return UtkastValidering.UgyldigUtkast(utkast, ResponseException.BadRequestException(
                "Husk å fylle inn både navnet og endringstypen på utkastet."))
        }

        if (utkast.status != UtkastStatus.OPPRETTET) {
            return UtkastValidering.UgyldigUtkast(utkast, ResponseException.BadRequestException(
                title = "Kan ikke oppdatere utkastet",
                msg = "Kan ikke oppdatere et utkast med status ${utkast.status}"
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

        return UtkastValidering.GyldigUtkast(utkast)
    }

    fun validateUtkastKlartForPublisering(utkast: Utkast): UtkastValidering {
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

        val ugyldigeNyeKretser = utkast.operasjoner?.kretsDelingEndringer?.flatMap { validerNyeKretserVilBliForsoektFlatedannet(utkast, it) } ?: emptyList()
        if (ugyldigeNyeKretser.isNotEmpty()) {
            return UtkastValidering.UgyldigUtkast(utkast, ResponseException.BadRequestException(
                title = "Kan ikke publisere utkastet",
                msg = "Krets(er) med navn: ${ugyldigeNyeKretser.joinToString(", ")} har ikke fått noen grenser tildelt. " +
                    "Dette er en feil, og den kan rettes opp ved å sette tilhørighet på de relevante grensene som skal høre til enten: ${ugyldigeNyeKretser.joinToString(", ")}"
            ))
        }

        val tilhorighetEndringerValideringsFeil = validerTilhorighetEndringer(utkast)
        if (tilhorighetEndringerValideringsFeil != null) {
            return tilhorighetEndringerValideringsFeil
        }

        val valideringResult = validateGyldigUtkast(utkast)
        if (valideringResult is UtkastValidering.UgyldigUtkast) {
            return UtkastValidering.UgyldigUtkast(utkast, valideringResult.feil)
        }

        return UtkastValidering.KlartForPublisering(utkast)
    }

    /**
     * Sekker om kontekstegenskapene på endrede features gir mening å prøve å endre på i utkastet
     */
    fun validerTilhorighetEndringer(utkast: Utkast): UtkastValidering.UgyldigUtkast? {
        val krevdAntallKontekstEgenskaper = 2

        return utkast.operasjoner?.grenseendringer?.endredeFeatures?.firstNotNullOfOrNull { feature ->
            val harFeilAntallKontekstEgenskaper = feature.properties.kontekstEgenskaper.size != krevdAntallKontekstEgenskaper
            val harToLikeKontekstEgenskaper = if (!harFeilAntallKontekstEgenskaper) {
                feature.properties.kontekstEgenskaper[0] == feature.properties.kontekstEgenskaper[1]
            } else {
                false
            }
            val kommuneGrenseHarTilhorigheterISammeKommune = feature.properties.type == Grensetype.Kommunegrense.value &&
                feature.properties.kontekstEgenskaper
                    .map { it.kommuneId?.lokalid?.value }
                    .toSet().size != 2
            if (harFeilAntallKontekstEgenskaper || harToLikeKontekstEgenskaper || kommuneGrenseHarTilhorigheterISammeKommune) {
                var errorMessage = "Grense med id: ${feature.id ?: "Ny grense, ingen id - "} har ugyldig tilhørighet. Årsak(er): \n"
                if (harFeilAntallKontekstEgenskaper) {
                    errorMessage += "\t- Grensen har ${feature.properties.kontekstEgenskaper.size} tilhørighet(er) satt, men må ha $krevdAntallKontekstEgenskaper.\n"
                }
                if (harToLikeKontekstEgenskaper) {
                    errorMessage += "\t- Grensen har tilhørighet til samme krets på hver side.\n"
                }
                if (kommuneGrenseHarTilhorigheterISammeKommune) {
                    errorMessage += "\t- Grensen er kommunegrense og har tilhørighet til to kretser i samme kommune.\n"
                }
                return UtkastValidering.UgyldigUtkast(utkast = utkast, feil = ResponseException.BadRequestException(
                    title = "Kan ikke publisere utkastet",
                    msg = errorMessage
                ))
            } else return null
        }
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
                    operasjoner.stemmekretsSammenslaaingsendring.navn,
                    operasjoner.stemmekretsSammenslaaingsendring.nummer,
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
                    flatetype = it.flatetype,
                    navn = it.kretsNavn,
                    kretsnummer = it.kretsNummer,
                    lokalid = it.opprinneligKrets.lokalId
                )
            }

        if (harUgyldigMetadataendringPaaKretsDeling) {
            return OperasjonerValidering.UgyldigeOperasjoner(
                operasjoner,
                ResponseException.BadRequestException(
                    "Utkast inneholder ugyldig metadataendringer på deling"
                ))
        }

        return null
    }

    private fun validerGrenseEndringerKonflikt(utkast: Utkast): FeilVedOppdatering.OppdateringException? {
        val endredeFeatures = utkast.operasjoner?.grenseendringer?.endredeFeatures
        if (endredeFeatures.isNullOrEmpty()) return null

        // Trenger ikke sjekke konflikter for nye grenser, så filtrerer ut grenser uten id
        val endredeFeaturesIder = endredeFeatures
            .filter { it.id != null }
            .map { it.id }

        return endredeFeaturesIder.firstNotNullOfOrNull { id ->
            if (id.isNullOrBlank()) return null

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

    private fun validerNyeKretserVilBliForsoektFlatedannet(utkast: Utkast, kretsDelingEndringRequest: KretsDelingEndringRequest): List<String> {
        return getUnikeKretsDelingEndringRequests(kretsDelingEndringRequest)
            .filter { !validerNyKretsVilBliForsoektFlatedannet(utkast, it) }
            .map { it.kretsNavn }
    }

    /** Validerer om vi kan gå videre med å forsøke å publisere en kretsdeling ved å se på grensejusteringene i utkastet.
     * Hvis det ikke er noen grensejusteringer som peker på den nyopprettede kretsen vil nyopprettet krets få null grenser, som skal kaste feil.
     **/

    private fun validerNyKretsVilBliForsoektFlatedannet(utkast: Utkast, kretsDelingEndringRequest: UnikKretsDelingEndring?): Boolean {
        if (kretsDelingEndringRequest == null) {
            return true
        }
        val endredeFeatures = utkast.operasjoner?.grenseendringer?.endredeFeatures
        if (endredeFeatures != null) {
            val kontekstEgenskaper = endredeFeatures.flatMap {
                it.properties.kontekstEgenskaper
            }

            val finnesGrenseendringerForNyopprettetKrets = kontekstEgenskaper.any {
                when (it.type) {
                    KontekstEnhetType.GRUNNKRETS ->
                        it.kretsNummer == kretsDelingEndringRequest.kretsNummer

                    KontekstEnhetType.STEMMEKRETS ->
                        it.kretsNummer == kretsDelingEndringRequest.kretsNummer
                            && it.kommuneId?.lokalid?.value == kretsDelingEndringRequest.kommuneId.lokalid.value
                }
            }
            return finnesGrenseendringerForNyopprettetKrets
        } else return false
    }


    /** Tenkt som generisk metadataendring-sjekker,
     * men fungerer foreløpig kun for grunnkretser og stemmekretser **/
    fun isMetadataendringUgyldig(flatetype: Flatetype, navn: String?, kretsnummer: String?, lokalid: String): Boolean {
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
    data class KlartForPublisering(val utkast: Utkast) : UtkastValidering()
    data class GyldigUtkast(val utkast: Utkast) : UtkastValidering()
    data class UgyldigUtkast(val utkast: Utkast, val feil: Exception) : UtkastValidering()
}

sealed class OperasjonerValidering {
    data class GyldigeOperasjoner(val operasjoner: Operasjoner) : OperasjonerValidering()
    data class UgyldigeOperasjoner(val operasjoner: Operasjoner, val feil: Exception) : OperasjonerValidering()
}
