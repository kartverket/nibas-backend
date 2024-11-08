package no.kartverket.nibas.service.utkast

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.api.v1.common.KontekstEnhetType
import no.kartverket.nibas.api.v1.common.Operasjoner
import no.kartverket.nibas.api.v1.common.UnikKretsDelingEndring
import no.kartverket.nibas.api.v1.common.geojson.Feature
import no.kartverket.nibas.api.v1.common.geojson.KontekstEgenskaper
import no.kartverket.nibas.api.v1.common.getUnikeKretsDelingEndringRequests
import no.kartverket.nibas.api.v1.request.KretsDelingEndringRequest
import no.kartverket.nibas.common.CurrentTime
import no.kartverket.nibas.common.FeilVedOppdatering
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.toNorskFormat
import no.kartverket.nibas.domain.utkast.Utkast
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

    fun validateGyldigeOperasjoner(operasjoner: Operasjoner): List<String> {
        val errorMessages = mutableListOf<String>()
        val endredeFeatures: List<Feature> = operasjoner.grenseendringer.endredeFeatures

        val featuresSomGjentas = endredeFeatures.filter { it.id != null }.groupBy { it.id }.filterValues { it.size > 1 }
        if (featuresSomGjentas.isNotEmpty()) {
            errorMessages.add("Utkastet inneholder grenseendringer for samme grense(r) flere ganger. Grensene (representert ved lokalid) det gjelder er: ${featuresSomGjentas.map { it.key }.sortedBy { it }}.")
        }

        val featuresSomIkkeErEditerbare = endredeFeatures.filterNot { Grensetype.fromValue(it.properties.type).editerbar }
        if (featuresSomIkkeErEditerbare.isNotEmpty()) {
            errorMessages.add("Utkastet inneholder ${featuresSomIkkeErEditerbare.size} ikke-redigerbare grenser.")
        }

        errorMessages.addAll(validerKretsinfoEndringer(operasjoner))

        return errorMessages
    }

    fun validateUtkastAttributter(navn: String, endringstype: String, gyldigFra: LocalDate): String? {
        var errorMessage: String? = null
        if (navn.isBlank()) {
            errorMessage = "Utkastet mangler navn."
        }
        if (endringstype.isBlank()) {
            errorMessage += " Utkastet mangler endringstype."
        }
        if (gyldigFra.isBefore(currentTime.asLocalDate())) {
            errorMessage += " Utkastet kan ikke være gyldig fra en tidligere dato."
        }
        return errorMessage
    }

    fun validateUtkastMedOperasjoner(utkast: Utkast, endringstype: String, gyldighetDato: LocalDate): List<String> {
        val errorMessages = mutableListOf<String>()

        val utkastNavnOgEndringstypeMessage = validateUtkastAttributter(utkast.navn, endringstype, gyldighetDato)
        if (utkastNavnOgEndringstypeMessage != null) {
            errorMessages.add(utkastNavnOgEndringstypeMessage)
        }

        errorMessages.addAll(validateGyldigeOperasjoner(utkast.operasjoner!!))

        return errorMessages
    }

    fun validerFremtidigeEndringer(utkast: Utkast, gyldighetDato: LocalDate): FeilVedOppdatering.OppdateringException? {
        validerGrunnkretsEndringerKonflikt(utkast, gyldighetDato)?.let { return it }
        validerStemmekretsEndringerKonflikt(utkast, gyldighetDato)?.let { return it }
        validerGrenseEndringerKonflikt(utkast, gyldighetDato)?.let { return it }
        validerStemmekretsSammenslaaingerKonflikt(utkast, gyldighetDato)?.let { return it }
        validerStemmekretsDelingerKonflikt(utkast, gyldighetDato)?.let { return it }
        validerGrunnkretsDelingerKonflikt(utkast, gyldighetDato)?.let { return it }

        return null
    }

    fun validateUtkastKlartForPublisering(utkast: Utkast, gyldighetDato: LocalDate): List<String> {
        val errorMessages = mutableListOf<String>()
        errorMessages.addAll(validateUtkastMedOperasjoner(utkast, utkast.auditInfo.typeEndring.value, gyldighetDato))

        if (gyldighetDato.isBefore(currentTime.asLocalDate())) {
            errorMessages.add("Kan ikke publisere et utkast som trer i kraft før dagens dato. Utkastet er satt til å tre i kraft ${gyldighetDato.toNorskFormat()}")
        }

        if (utkast.operasjoner!!.harIkkeEndringer()) {
            errorMessages.add("Kan ikke publisere et utkast uten endringer.")
        }

        val ugyldigeNyeKretser = utkast.operasjoner?.kretsDelingEndringer?.flatMap { validerNyeKretserVilBliForsoektFlatedannet(utkast, it) } ?: emptyList()
        if (ugyldigeNyeKretser.isNotEmpty()) {
            errorMessages.add("Krets(er) med navn: ${ugyldigeNyeKretser.joinToString(", ")} har ikke fått noen grenser tildelt. " +
                "Dette er en feil, og den kan rettes opp ved å sette tilhørighet på de relevante grensene som skal høre til enten: ${ugyldigeNyeKretser.joinToString(", ")}")
        }

        errorMessages.addAll(validerTilhorighetEndringer(utkast))
        errorMessages.addAll(validateTellekretserOgValgdistrikt(utkast.operasjoner!!))

        return errorMessages
    }

    fun validateTellekretserOgValgdistrikt(operasjoner: Operasjoner): List<String> {
        val errorMessages = mutableListOf<String>()

        operasjoner.metadataendringer.stemmekretsendringer.values.forEach {
            val tellekretsnummer = it.tellekretsnummer
            val stemmekretstekst = if (it.nummer.isNullOrBlank()) "" else " (stemmekrets ${it.nummer})"

            if (!tellekretsnummer.isNullOrBlank()) {
                val numeriskTellekretsnummer = tellekretsnummer.toIntOrNull()

                if (numeriskTellekretsnummer == null) {
                    errorMessages.add("Tellekretsnummer ($tellekretsnummer) kan bare inneholde sifre$stemmekretstekst.")
                } else if (numeriskTellekretsnummer == 0) {
                    errorMessages.add("Tellekretsnummer ($tellekretsnummer) kan ikke være 0$stemmekretstekst.")
                } else if (numeriskTellekretsnummer < 0) {
                    errorMessages.add("Tellekretsnummer ($tellekretsnummer) kan ikke være negativt$stemmekretstekst.")
                } else if (tellekretsnummer.length > 4) {
                    errorMessages.add("Tellekretsnummer ($tellekretsnummer) kan ikke være på flere enn fire tegn$stemmekretstekst.")
                }
                if (it.tellekretsnavn.isNullOrBlank()) {
                    errorMessages.add("Tellekretsnavn må gis når tellekretsnummer er gitt ($tellekretsnummer)$stemmekretstekst.")
                }
            }
            if (tellekretsnummer.isNullOrBlank() && !it.tellekretsnavn.isNullOrBlank()) {
                errorMessages.add("Tellekretsnummer må gis når tellekretsnavn er gitt (${it.tellekretsnavn})$stemmekretstekst.")
            }
            if (it.valgdistriktsnummer.isNullOrBlank()) {
                errorMessages.add("Valgdistriktsnummer må gis$stemmekretstekst.")
            }
        }
        return errorMessages
    }

    /**
     * Sekker om kontekstegenskapene på endrede features gir mening å prøve å endre på i utkastet.
     * Returnerer liste med eventuelle feilmeldinger
     */
    fun validerTilhorighetEndringer(utkast: Utkast): List<String> {
        val errorMessages = mutableListOf<String>()
        utkast.operasjoner?.grenseendringer?.endredeFeatures?.forEach { feature ->
            val innsendteTilhorigheter = feature.properties.kontekstEgenskaper
            //Kommunegrense og fylkesgrense skal ha 4 tilhørigheter (2 grunnkretser og 2 stemmekretser), andre skal bare ha 2
            val grensetype = Grensetype.fromValue(feature.properties.type)
            val erKommuneEllerFylkesGrense = grensetype == Grensetype.Kommunegrense || grensetype == Grensetype.Fylkesgrense
            val krevdAntallKontekstEgenskaper = if (erKommuneEllerFylkesGrense) 4 else 2
            val krevdAntallTilhorighetstyper = if (Grensetype.erAdministrativGrense(grensetype)) 2 else 1

            val harFeilAntallKontekstEgenskaper = feature.properties.kontekstEgenskaper.size != krevdAntallKontekstEgenskaper

            val tilhorighetstyper = innsendteTilhorigheter.map { it.type.name }.distinct()
            val harFeilAntallTilhorighetstyper = tilhorighetstyper.size != krevdAntallTilhorighetstyper

            var duplisertKontekstEgenskaper: KontekstEgenskaper? = null
            innsendteTilhorigheter.forEachIndexed { index, element ->
                innsendteTilhorigheter.filterIndexed { i, _ -> i > index && duplisertKontekstEgenskaper == null }.forEach { indreElement ->
                    if (erSammeTilhorighet(element, indreElement)) {
                        duplisertKontekstEgenskaper = element
                    }
                }
            }

            val kommuneGrenseHarTilhorigheterISammeKommune =
                if (erKommuneEllerFylkesGrense && duplisertKontekstEgenskaper == null) {
                    val map = mutableMapOf<Pair<KontekstEnhetType, String>, Int>()
                    feature.properties.kontekstEgenskaper
                        .forEach {
                            val pair = Pair(it.type, it.kommuneId?.lokalid?.value ?: "")
                            map[pair] = map.getOrDefault(pair, 0) + 1
                        }
                    map.values.any { it > 1 }
                } else false

            if (harFeilAntallKontekstEgenskaper || duplisertKontekstEgenskaper != null || kommuneGrenseHarTilhorigheterISammeKommune || harFeilAntallTilhorighetstyper) {
                errorMessages.add("Grense med id: ${feature.id ?: "Ny grense, ingen id - "} har ugyldig tilhørighet. Årsak(er):")
                if (harFeilAntallKontekstEgenskaper) {
                    errorMessages.add("\t- Grensen har ${feature.properties.kontekstEgenskaper.size} tilhørighet(er) satt, men må ha $krevdAntallKontekstEgenskaper.")
                }
                if (harFeilAntallTilhorighetstyper) {
                    errorMessages.add("\t- Grensen har ${tilhorighetstyper.size} tilhørighetstyper satt, men må ha $krevdAntallTilhorighetstyper.")
                }
                if (duplisertKontekstEgenskaper != null) {
                    errorMessages.add("\t- Grensen har flere tilhørigheter til samme krets. Gjelder kretsnr ${duplisertKontekstEgenskaper?.kretsNummer} med lokalid ${duplisertKontekstEgenskaper?.id?.lokalid?.value}.")
                }
                if (kommuneGrenseHarTilhorigheterISammeKommune) {
                    errorMessages.add("\t- Grensen er kommune-/fylkesgrense og har tilhørighet til to kretser i samme kommune.")
                }

            }
        }

        return errorMessages
    }

    private fun erSammeTilhorighet(tilhorighet1: KontekstEgenskaper, tilhorighet2: KontekstEgenskaper): Boolean {
        //Hvis begge lokalid-er er gitt og er ulike, er det ikke samme tilhørighet
        //Hvis begge lokalid-er er gitt og ulike, er det samme tilhørighet
        if (tilhorighet1.id?.lokalid != null && tilhorighet2.id?.lokalid != null) {
            return tilhorighet1.id.lokalid == tilhorighet2.id.lokalid
        }

        // Hvis en er null er det ny(e) krets(er), vi må da sammenligne kretsnummer (og kommune for stemmekrets)
        if (tilhorighet1.id?.lokalid == null || tilhorighet2.id?.lokalid == null) {
            if (tilhorighet1.type == KontekstEnhetType.GRUNNKRETS) {
                return tilhorighet1.kretsNummer == tilhorighet2.kretsNummer
            }
            return tilhorighet1.kretsNummer == tilhorighet2.kretsNummer && tilhorighet1.kommuneId?.lokalid == tilhorighet2.kommuneId?.lokalid
        }
        return false
    }

    fun validerKretsinfoEndringer(operasjoner: Operasjoner): List<String> {
        val errorMessages = mutableListOf<String>()

        operasjoner.metadataendringer.grunnkretsendringer.values.forEach {
            errorMessages.addAll(validateMetadataendringUgyldig(
                Flatetype.GRUNNKRETS,
                it.navn,
                it.nummer,
                it.identifikasjon.lokalid
            ))
        }

        if (operasjoner.grunnkretsSammenslaaingsendring != null) {
            errorMessages.addAll(validateMetadataendringUgyldig(
                Flatetype.GRUNNKRETS,
                operasjoner.grunnkretsSammenslaaingsendring.navn,
                operasjoner.grunnkretsSammenslaaingsendring.nummer,
                operasjoner.grunnkretsSammenslaaingsendring.viderefoertGrunnkrets.lokalId
            ))
        }

        operasjoner.metadataendringer.stemmekretsendringer.values.forEach {
            errorMessages.addAll(validateMetadataendringUgyldig(
                Flatetype.STEMMEKRETS,
                it.navn,
                it.nummer,
                it.identifikasjon.lokalid
            ))
        }

        if (operasjoner.stemmekretsSammenslaaingsendring != null)
            errorMessages.addAll(validateMetadataendringUgyldig(
                Flatetype.STEMMEKRETS,
                operasjoner.stemmekretsSammenslaaingsendring.navn,
                operasjoner.stemmekretsSammenslaaingsendring.nummer,
                operasjoner.stemmekretsSammenslaaingsendring.viderefoertStemmekrets.lokalId
            ))

        getUnikeKretsDelingEndringRequests(operasjoner.kretsDelingEndringer)
            .forEach {
                errorMessages.addAll(validateMetadataendringUgyldig(
                    flatetype = it.flatetype,
                    navn = it.kretsNavn,
                    kretsnummer = it.kretsNummer,
                    lokalid = it.opprinneligKrets.lokalId
                ))
            }

        return errorMessages
    }

    private fun validerGrenseEndringerKonflikt(utkast: Utkast, gyldighetDato: LocalDate): FeilVedOppdatering.OppdateringException? {
        val endredeFeatures = utkast.operasjoner?.grenseendringer?.endredeFeatures
        if (endredeFeatures.isNullOrEmpty()) return null

        // Trenger ikke sjekke konflikter for nye grenser, så filtrerer ut grenser uten id
        val endredeFeaturesIder = endredeFeatures
            .filter { it.id != null }
            .map { it.id }

        val grenseOppdateringException = endredeFeaturesIder.firstNotNullOfOrNull { id ->
            if (id.isNullOrBlank()) return null

            val senerePubliserteGrenser = grenseService.hentAlleVersjonerMedGyldigFraEtterGittDato(id, gyldighetDato)
                .map { Identifikator(Lokalid(it.lokalid), it.gyldighet.gyldigFra) }
            lagEventueltOppdateringException(senerePubliserteGrenser, gyldighetDato, FeilVedOppdatering.Type.GRENSE)
        }
        if (grenseOppdateringException != null) {
            return grenseOppdateringException
        }

        val unikeKontekstegenskaper = endredeFeatures.flatMap { it.properties.kontekstEgenskaper }.toSet()
        return unikeKontekstegenskaper.firstNotNullOfOrNull {
            if (it.id == null) return null
            when (it.type) {
                KontekstEnhetType.STEMMEKRETS -> finnEventueltSenerePubliserteStemmekretserOgLagOppdateringException(it.id.lokalid.value, gyldighetDato)
                KontekstEnhetType.GRUNNKRETS -> finnEventueltSenerePubliserteGrunnkretserOgLagOppdateringException(it.id.lokalid.value, gyldighetDato)
            }
        }
    }

    private fun validerGrunnkretsEndringerKonflikt(utkast: Utkast, gyldighetDato: LocalDate): FeilVedOppdatering.OppdateringException? {
        return utkast.operasjoner?.metadataendringer?.grunnkretsendringer?.firstNotNullOfOrNull { grunnkretsendring ->
            finnEventueltSenerePubliserteGrunnkretserOgLagOppdateringException(grunnkretsendring.key, gyldighetDato)
        }
    }

    private fun validerStemmekretsEndringerKonflikt(utkast: Utkast, gyldighetDato: LocalDate): FeilVedOppdatering.OppdateringException? {
        return utkast.operasjoner?.metadataendringer?.stemmekretsendringer?.firstNotNullOfOrNull { stemmekretsendring ->
            finnEventueltSenerePubliserteStemmekretserOgLagOppdateringException(stemmekretsendring.key, gyldighetDato)
        }
    }

    private fun validerStemmekretsDelingerKonflikt(utkast: Utkast, gyldighetDato: LocalDate): FeilVedOppdatering.OppdateringException? {
        return utkast.operasjoner?.kretsDelingEndringer?.filter { it.flatetype == Flatetype.STEMMEKRETS }?.firstNotNullOfOrNull { request ->
            finnEventueltSenerePubliserteStemmekretserOgLagOppdateringException(request.opprinneligKrets.lokalId, gyldighetDato)
        }
    }

    private fun validerGrunnkretsDelingerKonflikt(utkast: Utkast, gyldighetDato: LocalDate): FeilVedOppdatering.OppdateringException? {
        return utkast.operasjoner?.kretsDelingEndringer?.filter { it.flatetype == Flatetype.GRUNNKRETS }?.firstNotNullOfOrNull { request ->
            finnEventueltSenerePubliserteGrunnkretserOgLagOppdateringException(request.opprinneligKrets.lokalId, gyldighetDato)
        }
    }

    private fun validerStemmekretsSammenslaaingerKonflikt(utkast: Utkast, gyldighetDato: LocalDate): FeilVedOppdatering.OppdateringException? {
        val stemmekretsLokalids = mutableListOf<String>()
        utkast.operasjoner?.stemmekretsSammenslaaingsendring?.let { it ->
            stemmekretsLokalids.add(it.viderefoertStemmekrets.lokalId)
            stemmekretsLokalids.addAll(it.stemmekretserTilSammenslaaing.map { it.lokalId })
        }

        return stemmekretsLokalids.firstNotNullOfOrNull { stemmekretsLokalid ->
            finnEventueltSenerePubliserteStemmekretserOgLagOppdateringException(stemmekretsLokalid, gyldighetDato)
        }
    }

    private fun finnEventueltSenerePubliserteStemmekretserOgLagOppdateringException(stemmekretsLokalid: String, gyldighetDato: LocalDate)
        : FeilVedOppdatering.OppdateringException? {
        val senerePubliserteEndringer =
            stemmekretsService.hentAlleVersjonerMedGyldigFraEtterGittDato(lokalId = Lokalid(stemmekretsLokalid), dato = gyldighetDato)
                .map { Identifikator(Lokalid(it.lokalid), it.gyldighet.gyldigFra) }

        return lagEventueltOppdateringException(senerePubliserteEndringer, gyldighetDato, FeilVedOppdatering.Type.STEMMEKRETS)
    }

    private fun finnEventueltSenerePubliserteGrunnkretserOgLagOppdateringException(grunnkretsLokalid: String, gyldighetDato: LocalDate)
        : FeilVedOppdatering.OppdateringException? {
        val senerePubliserteEndringer =
            grunnkretsService.hentAlleVersjonerMedGyldigFraEtterGittDato(lokalId = Lokalid(grunnkretsLokalid), dato = gyldighetDato)
                .map { Identifikator(Lokalid(it.lokalid), it.gyldighet.gyldigFra) }

        return lagEventueltOppdateringException(senerePubliserteEndringer, gyldighetDato, FeilVedOppdatering.Type.GRUNNKRETS)
    }

    private fun lagEventueltOppdateringException(senerePubliserteEndringer: List<Identifikator>, gyldighetDato: LocalDate,
                                                 feilVedOppdateringType: FeilVedOppdatering.Type) =
        if (senerePubliserteEndringer.isNotEmpty()) {
            FeilVedOppdatering.OppdateringException(FeilVedOppdatering.OppdateringInfo(
                id = Identifikator(Lokalid(senerePubliserteEndringer.first().lokalid.value), gyldighetDato),
                type = feilVedOppdateringType,
                affectedIds = senerePubliserteEndringer.map { Identifikator(Lokalid(it.lokalid.value), it.gyldighetsdato) }
            ))
        } else {
            null
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

    fun validateMetadataendringUgyldig(flatetype: Flatetype, navn: String?, kretsnummer: String?, lokalid: String): List<String> {
        val errorMessages = mutableListOf<String>()

        if (kretsnummer.isNullOrBlank()) {
            errorMessages.add("Kretsnummer kan ikke være tomt.")
        } else {
            val numeriskKretsnummer = kretsnummer.toIntOrNull()
            if (numeriskKretsnummer == null) {
                errorMessages.add("Kretsnummer ($kretsnummer) kan bare inneholde sifre.")
            } else if (numeriskKretsnummer == 0) {
                errorMessages.add("Kretsnummer ($kretsnummer) kan ikke være 0.")
            } else if (numeriskKretsnummer < 0) {
                errorMessages.add("Kretsnummer ($kretsnummer) kan ikke være negativt.")
            } else if (flatetype == Flatetype.STEMMEKRETS && kretsnummer.length > 4) {
                errorMessages.add("Stemmekretsnummer ($kretsnummer) kan ikke være på flere enn fire tegn.")
            } else if (flatetype == Flatetype.GRUNNKRETS) {
                if (kretsnummer.length != 8) {
                    errorMessages.add("Grunnkretsnummer ($kretsnummer) må være på åtte tegn.")
                } else {
                    sjekkUgyldigKommuneIGrunnkretsnummer(kretsnummer, lokalid)?.let {
                        errorMessages.add("De fire første sifrene i grunnkretsnummer ($kretsnummer) må være kommunenummer ($it).")
                    }
                }
            }
        }

        if (navn.isNullOrBlank()) {
            errorMessages.add("Kretsnavn kan ikke være tomt.")
        }

        return errorMessages
    }

    private fun sjekkUgyldigKommuneIGrunnkretsnummer(nummer: String, lokalid: String): String? {
        val grunnkrets = grunnkretsService.hentGrunnkrets(Identifikator(lokalid = Lokalid(lokalid), gyldighetsdato = LocalDate.now()))

        val kommunenummer = grunnkrets?.getKommune(grunnkrets.gyldighet.gyldigFra)?.kommunenummer
        if (kommunenummer.isNullOrBlank()) {
            return ""
        } else if (nummer.take(4) != kommunenummer) {
            return kommunenummer
        }
        return null
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
        this.grunnkretsSammenslaaingsendring != null ||
        this.kretsDelingEndringer.isNotEmpty()
}

private fun Operasjoner.harIkkeEndringer(): Boolean = !this.harEndringer()

