package no.kartverket.nibas.inndelinger

import no.kartverket.nibas.api.v1.GrunnkretsApi
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.common.geojson.metadata.Identifikasjon
import no.kartverket.nibas.api.v1.request.GrunnkretsRequest
import no.kartverket.nibas.api.v1.response.GrunnkretsRef
import no.kartverket.nibas.api.v1.response.GrunnkretsResponse
import no.kartverket.nibas.api.v1.response.RevisjonRef
import no.kartverket.nibas.common.FeilVedOppdatering
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
import no.kartverket.nibas.repository.findAllByLokalidSortedByGyldigFra
import no.kartverket.nibas.repository.verifiserOptimistiskLaas
import no.kartverket.nibas.sporing.Gyldig
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.time.LocalDate
import java.util.Optional
import java.util.Collections.emptySet

@RestController
class GrunnkretsController(
    private val grunnkretsService: GrunnkretsService,
    private val kodelisteService: KodelisteService,
    private val historikkService: HistorikkService,
    private val grunnkretsControllerService: GrunnkretsControllerService
) : GrunnkretsApi {

    override fun hentGrunnkrets(id: String, gyldigDato: Optional<LocalDate>): GrunnkretsResponse {
        return grunnkretsControllerService.hentEn(Lokalid(id), gyldigDato.orElse(LocalDate.now()))
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

    override fun hentGrunnkretsRevisjoner(id: String): List<RevisjonRef> =
        historikkService.hentRevisjonerForLokalid(Lokalid(id), Grunnkrets::class.java).map { toRevisjonRef(it) }

    override fun hentGrunnkretsRevisjon(id: String, revisjon: Int): GrunnkretsResponse =
        historikkService.hentRevisjonForLokalId(Lokalid(id), revisjon, Grunnkrets::class.java).toGrunnkretsResponse(kodelisteService)

}

@Transactional(readOnly = true)
@Service
class GrunnkretsControllerService(private val grunnkretsService: GrunnkretsService,
                                  private val kodelisteService: KodelisteService) {
    fun hentEn(id: Lokalid, gyldigDato: LocalDate): GrunnkretsResponse =
        grunnkretsService.hentGrunnkrets(id, gyldigDato)?.toGrunnkretsResponse(kodelisteService)
            ?: throw NotFoundException("Grunnkrets med id '$id' og gyldigDato '$gyldigDato' ble ikke funnet.")
}


@Transactional
@Service
class GrunnkretsService(private val grunnkretsRepository: GrunnkretsRepository) {

    @Transactional(readOnly = true)
    fun hentGrunnkrets(id: Lokalid, gyldigDato: LocalDate): Grunnkrets? {
        val grunnkretser: List<Grunnkrets> = grunnkretsRepository.findAllByLokalid(id.value)
            .filter { it.gyldigPaaTidspunkt(gyldigDato) }

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
    fun oppdaterGrunnkrets(id: Lokalid, grunnkretsRequestWrapper: GrunnkretsRequestWrapper) {
        val request = grunnkretsRequestWrapper.grunnkretsRequest
        val gyldigFra = grunnkretsRequestWrapper.gyldigFra

        val grunnkretsHelper = GrunnkretsHelper(grunnkretsRepository.findAllByLokalidSortedByGyldigFra(id.value))

        // Bare sjekke for tilstedeværelse av eksisterende rader med nyere gyldigFra-dato dersom innkommende objekt er et 'ikke-resolved' objekt
        if (!grunnkretsRequestWrapper.resolved) {
            grunnkretsHelper.checkForNewerGyldigFra(gyldigFra, id)
        }

        // Må bruke grunnkrets som matcher lokalId på gyldigFra-tidspunkt
        grunnkretsHelper.findRelevantGrunnkretsForGyldigFra(gyldigFra, id)
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

fun toGrunnkretsRef(grunnkrets: Grunnkrets): GrunnkretsRef {
    val lokalid = grunnkrets.lokalid
    return GrunnkretsRef(
        id = lokalid,
        navn = grunnkrets.grunnkretsnavn!!,
        grunnkretsnummer = grunnkrets.grunnkretsnummer!!,
        href = ServletUriComponentsBuilder.fromCurrentRequestUri().pathSegment(lokalid).build().toUriString()
    )
}

fun Grunnkrets.toGrunnkretsResponse(kodelisteService: KodelisteService): GrunnkretsResponse {
    val lokalid = this.lokalid
    return GrunnkretsResponse(
        id = lokalid,
        navn = this.grunnkretsnavn!!,
        grunnkretsnummer = this.grunnkretsnummer!!,
        identifikasjon = Identifikasjon(lokalid),
        kommunenummer = kodelisteService.createKommunenummerFromUUID(this.kommune.kommunenummer),
        features = FeatureCollection(features = listOf(
            posisjonToFeature(this.posisjon, this.version),
            flateToFeature(this, kodelisteService, this.version))),
        version = this.version
    )
}

data class GrunnkretsRequestWrapper(
    val grunnkretsRequest: GrunnkretsRequest,
    val gyldigFra: LocalDate,
    // TODO: Bør være en enumerert type når vi får oppdatert sosimodell inn i vår domenemodell
    val endringstype: String,
    val resolved: Boolean = false
)
