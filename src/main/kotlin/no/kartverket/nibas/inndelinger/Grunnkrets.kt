package no.kartverket.nibas.inndelinger

import no.kartverket.nibas.api.v1.GrunnkretsApi
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.common.geojson.metadata.Identifikasjon
import no.kartverket.nibas.api.v1.request.GrunnkretsRequest
import no.kartverket.nibas.api.v1.response.GrunnkretsRef
import no.kartverket.nibas.api.v1.response.GrunnkretsResponse
import no.kartverket.nibas.api.v1.response.KretsResponse
import no.kartverket.nibas.api.v1.response.toGyldighetResponse
import no.kartverket.nibas.common.FeilVedOppdatering
import no.kartverket.nibas.common.HrefProvider
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException
import no.kartverket.nibas.common.ResponseException.NotFoundException
import no.kartverket.nibas.domain.ContainsSosiIdentifikasjon
import no.kartverket.nibas.domain.GrenseKobling
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.kodelister.createKommunenummerFromUUID
import no.kartverket.nibas.repository.GrunnkretsRepository
import no.kartverket.nibas.repository.OptimistiskLaasException
import no.kartverket.nibas.repository.findAllByLokalidFramITid
import no.kartverket.nibas.repository.findAllByLokalidSortedByGyldigFra
import no.kartverket.nibas.repository.findGyldigGrunnkretsForTidspunkt
import no.kartverket.nibas.repository.verifiserOptimistiskLaas
import no.kartverket.nibas.sporing.Gyldig
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.*
import java.util.Collections.emptySet

@RestController
class GrunnkretsController(
    private val grunnkretsService: GrunnkretsService,
    private val kodelisteService: KodelisteService,
) : GrunnkretsApi {

    override fun hentGrunnkrets(id: String, gyldighetsdato: Optional<LocalDate>): GrunnkretsResponse {
        return grunnkretsService.hentGrunnkrets(Lokalid(id), gyldighetsdato.orElse(LocalDate.now()))?.toGrunnkretsResponse(kodelisteService)
            ?: throw NotFoundException("Grunnkrets med id '$id' og gyldighetsdato '$gyldighetsdato' ble ikke funnet.")
    }

    override fun hentGrenserForGrunnkrets(id: String, gyldigDato: Optional<LocalDate>): FeatureCollection {
        val dato = gyldigDato.orElse(LocalDate.now())
        val grenser = grunnkretsService.hentGrenserForGrunnkrets(Lokalid(id), dato)
        if (grenser.isEmpty()) {
            throw NotFoundException("Fant ingen grenser for id '$id' med gyldig dato '$dato'")
        }
        return FeatureCollection(
            features = grenser.sortedBy { it.grenseKoblingAttributter.rekkefoelge }
                .map { grenseKoblingToFeature(it, kodelisteService) }
        )
    }

    override fun hentFramtidigeVersjonerForGrunnkrets(lokalid: String): List<GrunnkretsResponse> {
        return grunnkretsService.hentFramtidigeVersjonerForGrunnkrets(Lokalid(lokalid)).map { it.toGrunnkretsResponse(kodelisteService) }
    }

}

@Transactional
@Service
class GrunnkretsService(private val grunnkretsRepository: GrunnkretsRepository) {

    @Transactional(readOnly = true)
    fun hentGrunnkrets(id: Lokalid, gyldighetsdato: LocalDate): Grunnkrets? {
        val grunnkretser: List<Grunnkrets> = grunnkretsRepository.findAllByLokalid(id.value)
            .filter { it.gyldigPaaTidspunkt(gyldighetsdato) }

        if (grunnkretser.isEmpty()) return null
        if (grunnkretser.size > 1) throw RuntimeException("Flere gyldige grunnkretser på gitt tidspunkt.")

        return grunnkretser[0]
    }

    @Transactional(readOnly = true)
    fun hentGrenserForGrunnkrets(id: Lokalid, gyldigDato: LocalDate): Set<GrenseKobling> {
        return hentGrunnkrets(id, gyldigDato)?.grenser ?: emptySet()
    }

    /**
     * Id her må være lokalid.
     * Vet at grunnkretsRequestWrapper.gyldigFra er satt, og må være lik i dag eller fram i tid.
     */
    fun oppdaterGrunnkrets(lokalid: Lokalid, grunnkretsRequestWrapper: GrunnkretsRequestWrapper) {
        val request = grunnkretsRequestWrapper.grunnkretsRequest
        val gyldigFra = grunnkretsRequestWrapper.gyldigFra

        val grunnkretsHelper = GrunnkretsHelper(grunnkretsRepository.findAllByLokalidSortedByGyldigFra(lokalid))

        // Bare sjekke for tilstedeværelse av eksisterende rader med nyere gyldigFra-dato dersom innkommende objekt er et 'ikke-resolved' objekt
        if (!grunnkretsRequestWrapper.resolved) {
            grunnkretsHelper.checkForNewerGyldigFra(gyldigFra, lokalid)
        }

        // Må bruke grunnkrets som matcher lokalId på gyldigFra-tidspunkt
        grunnkretsHelper.findRelevantGrunnkretsForGyldigFra(gyldigFra, lokalid)
            .let { relevantGrunnkrets ->
                verifiserOptimistiskLaas(request.version, relevantGrunnkrets)?.let { throw OptimistiskLaasException(it) }

                // Her har optimistisk sjekk gått bra. Må også sjekke om gyldigFra-dato er blitt modifisert på en resolved forekomst.
                // Kan sjekke at innkommende gyldigFra aldri skal være senere enn en eksisterende dato.
                grunnkretsHelper.assertGyldigFraForResolvedEntry(grunnkretsRequestWrapper.resolved, gyldigFra)

                // Må oppdatere forrige rad på tidslinja
                relevantGrunnkrets.setGyldigTil(gyldigFra)

                // Uansett om gyldigFra er identisk med grunnkretsens gyldigFra eller fram i tid: Lage ny rad (ny uuid, samme lokalid).
                val nyGrunnkrets = grunnkretsRepository.save(Grunnkrets(lokalid = relevantGrunnkrets.lokalid)
                    // Mappe alle øvrige felter fra eksisterende grunnkrets
                    .applyMappingFromExistingGrunnkrets(relevantGrunnkrets)
                    .applyChangesFromRequest(request))
                nyGrunnkrets
                    .applyCascadingOperationsBasedOnExistingGrunnkrets(relevantGrunnkrets)
                    .apply {
                        setGyldigFra(gyldigFra)
                        if (grunnkretsRequestWrapper.resolved && grunnkretsHelper.existsRowsWithLaterGyldigFraDato(relevantGrunnkrets)) {
                            // Setter gyldigTil lik gyldigFra for neste rad
                            setGyldigTil(grunnkretsHelper.getNesteGyldigFraDato(relevantGrunnkrets))
                        }
                        this.typeEndring = grunnkretsRequestWrapper.endringstype
                        this.version = grunnkretsHelper.getNextVersion()
                    }
            }
    }

    /**
     * Returnerer liste med grunnkretser som matcher lokalid og gyldigtidspunkt.
     * -Første innslag er raden som er gyldig på gyldigTidspunkt, default dagens dato.
     * -Så følger de framtidige gyldige publiserte versjonene sortert på gyldigFra.
     */
    fun hentFramtidigeVersjonerForGrunnkrets(lokalid: Lokalid, gyldigTidspunkt: LocalDate = LocalDate.now()): List<Grunnkrets> {
        return listOf(grunnkretsRepository.findGyldigGrunnkretsForTidspunkt(lokalid, gyldigTidspunkt))
            .plus(grunnkretsRepository.findAllByLokalidFramITid(lokalid, gyldigTidspunkt))
    }

    fun hentAntallGyldigeVersjonerFramITidForLokalid(lokalid: Lokalid, gyldigTidspunkt: LocalDate = LocalDate.now()): Int =
        grunnkretsRepository.findAllByLokalidFramITid(lokalid, gyldigTidspunkt).size

}

private class GrunnkretsHelper(private val grunnkretserSortedByGyldigFra: List<Grunnkrets>) {

    fun findRelevantGrunnkretsForGyldigFra(utkastetsGyldigFra: LocalDate, id: Lokalid): Grunnkrets =
        grunnkretserSortedByGyldigFra.find { grunnkrets -> grunnkrets.gyldigPaaTidspunkt(utkastetsGyldigFra) }
            ?: throw NotFoundException("Fant ikke eksisterende grunnkrets for lokalid $id og gyldigFra $utkastetsGyldigFra.")

    fun existsRowsWithLaterGyldigFraDato(grunnkrets: Grunnkrets): Boolean {
        return findIndex(grunnkrets) < grunnkretserSortedByGyldigFra.lastIndex
    }

    fun getNesteGyldigFraDato(grunnkrets: Grunnkrets): LocalDate {
        return grunnkretserSortedByGyldigFra[findIndex(grunnkrets) + 1].gyldighet.gyldigFra
    }

    private fun findIndex(grunnkrets: Grunnkrets) =
        grunnkretserSortedByGyldigFra.indices.find { grunnkretserSortedByGyldigFra[it].id == grunnkrets.id }
            ?: throw RuntimeException("Did not find index for grunnkrets $grunnkrets")

    fun checkForNewerGyldigFra(gyldigFra: LocalDate, id: Lokalid) {
        checkForPresenceOfRowsWithNewerGyldigFra(grunnkretserSortedByGyldigFra, gyldigFra, id, FeilVedOppdatering.Type.GRUNNKRETS)
    }

    fun assertGyldigFraForResolvedEntry(isResolvedEntry: Boolean, gyldigFra: LocalDate) {
        val latestGyldigFra = getLatestGyldigFra()
        if (isResolvedEntry && gyldigFra.isAfter(latestGyldigFra)) {
            throw ResponseException.BadRequestException("Ikke lov å modifisere gyldigFra-tidspunkt ved resolving av en framtidig versjon-konflikt. " +
                "Siste kjente gyldigFra for dette objektet er: $latestGyldigFra ($gyldigFra).")
        }
    }

    fun getLatestGyldigFra(): LocalDate {
        return grunnkretserSortedByGyldigFra.maxBy { it.gyldighet.gyldigFra }.gyldighet.gyldigFra
    }

    fun getNextVersion() = grunnkretserSortedByGyldigFra.maxBy { it.version }.version + 1

}

// Hvis det finnes en eller flere rader med nyere gyldigFra-dato, kast feil
// Bruker en intersection type som både er Gyldig og ContainsSosiIdentifikasjon
fun <T> checkForPresenceOfRowsWithNewerGyldigFra(
    gyldigeObjekterMedLokalid: List<T>,
    utkastetsGyldigFra: LocalDate,
    lokalid: Lokalid,
    type: FeilVedOppdatering.Type
) where T : Gyldig, T : ContainsSosiIdentifikasjon {
    val objekterMedNyereGyldigFra = gyldigeObjekterMedLokalid.filter { it.gyldighet.gyldigFra.isAfter(utkastetsGyldigFra) }
    if (objekterMedNyereGyldigFra.isNotEmpty()) {
        throw FeilVedOppdatering.OppdateringException(FeilVedOppdatering.OppdateringInfo(
            id = Identifikator(lokalid, utkastetsGyldigFra),
            type = type,
            affectedIds = objekterMedNyereGyldigFra.map { Identifikator(Lokalid(it.lokalid), it.gyldighet.gyldigFra) }
        ))
    }
}

// Skiller ut kallene som fører til cascading-effekt i egen funksjon. Disse går i beina på repository.save() dersom de kjøres først.
private fun Grunnkrets.applyCascadingOperationsBasedOnExistingGrunnkrets(relevantGrunnkrets: Grunnkrets): Grunnkrets {
    return this.apply {
        // Må legge til denne Grunnkrets på eksisterende Grunnkrets sin kommune
        relevantGrunnkrets.kommune.addGrunnkrets(this)
        // For eksisterende Grunnkrets sine informasjonselementer (1 i tallet), lag nye koblinger med denne Grunnkrets
        addInformasjonselementerFrom(relevantGrunnkrets.informasjonselementer)
        // For eksisterende Grunnkrets sine grensekoblinger, lag nye koblinger fra grense og grensekoblingAttributter
        addGrenseKoblingerFrom(relevantGrunnkrets.grenser)
    }
}

private fun Grunnkrets.applyMappingFromExistingGrunnkrets(relevantGrunnkrets: Grunnkrets): Grunnkrets {
    return this.apply {
        datafangstdato = relevantGrunnkrets.datafangstdato
        endringsflagg = relevantGrunnkrets.endringsflagg
        informasjon = relevantGrunnkrets.informasjon
        kopidata = relevantGrunnkrets.kopidata
        opphav = relevantGrunnkrets.opphav
        registreringsversjon = relevantGrunnkrets.registreringsversjon
        sluttdato = relevantGrunnkrets.sluttdato
        posisjon = relevantGrunnkrets.posisjon
    }
}

// Setter feltene som faktisk kan endres i request
private fun Grunnkrets.applyChangesFromRequest(request: GrunnkretsRequest): Grunnkrets {
    return this.apply {
        grunnkretsnavn = request.navn
        grunnkretsnummer = request.grunnkretsnummer
    }
}

fun Grunnkrets.toGrunnkretsRef(antallFramtidigeVersjoner: Int = 0, hrefProvider: HrefProvider): GrunnkretsRef {
    val lokalid = this.lokalid
    return GrunnkretsRef(
        id = lokalid,
        navn = this.grunnkretsnavn!!,
        grunnkretsnummer = this.grunnkretsnummer!!,
        href = hrefProvider.getHref(lokalid),
        antallFramtidigeVersjoner = antallFramtidigeVersjoner
    )
}

fun Grunnkrets.toGrunnkretsResponse(kodelisteService: KodelisteService): GrunnkretsResponse {
    val lokalid = this.lokalid
    return GrunnkretsResponse(
        id = lokalid,
        identifikasjon = Identifikasjon(lokalid),
        navn = this.grunnkretsnavn!!,
        grunnkretsnummer = this.grunnkretsnummer!!,
        gyldighet = this.gyldighet.toGyldighetResponse(),
        oppdateringsdato = this.auditInfo.oppdateringsdato,
        features = FeatureCollection(features = listOf(
            posisjonToFeature(this.posisjon, this.version),
            flateToFeature(this, kodelisteService, this.version))),
        version = this.version
    )
}

fun Grunnkrets.toKretsResponse(kodelisteService: KodelisteService): KretsResponse = KretsResponse(
    id = this.lokalid,
    navn = this.grunnkretsnavn!!,
    nummer = this.grunnkretsnummer!!,
    kommunenummer = kodelisteService.createKommunenummerFromUUID(this.kommune.kommunenummer),
    features = FeatureCollection(features = listOf(
        posisjonToFeature(this.posisjon, this.version),
        flateToFeature(this, kodelisteService, this.version))),
)

data class GrunnkretsRequestWrapper(
    val grunnkretsRequest: GrunnkretsRequest,
    val gyldigFra: LocalDate,
    // TODO: Bør være en enumerert type når vi får oppdatert sosimodell inn i vår domenemodell
    val endringstype: String,
    val resolved: Boolean = false
)
