package no.kartverket.nibas.inndelinger

import no.kartverket.nibas.api.v1.GrunnkretsApi
import no.kartverket.nibas.api.v1.common.KontekstEnhetType
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.request.GrunnkretsRequest
import no.kartverket.nibas.api.v1.response.GrunnkretsRef
import no.kartverket.nibas.api.v1.response.GrunnkretsResponse
import no.kartverket.nibas.api.v1.response.KretsResponse
import no.kartverket.nibas.api.v1.response.toGyldighetResponse
import no.kartverket.nibas.api.v1.response.toObjektIdentifikator
import no.kartverket.nibas.common.FeilVedOppdatering
import no.kartverket.nibas.common.HrefProvider
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException
import no.kartverket.nibas.common.ResponseException.NotFoundException
import no.kartverket.nibas.common.UgyldigTilstandException
import no.kartverket.nibas.common.createIdentifikator
import no.kartverket.nibas.domain.ContainsSosiIdentifikasjon
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.GrenseKobling
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.kodelister.createKommunenummerFromUUID
import no.kartverket.nibas.repository.GrunnkretsRepository
import no.kartverket.nibas.repository.OptimistiskLaasException
import no.kartverket.nibas.repository.findAllByLokalidSortedByGyldigFra
import no.kartverket.nibas.repository.findAllFramtidigeVersjonerByIdentifikator
import no.kartverket.nibas.repository.findAntallFramtidigeVersjonerForIdentifikator
import no.kartverket.nibas.repository.findGyldigObjektForIdentifikator
import no.kartverket.nibas.repository.verifiserOptimistiskLaas
import no.kartverket.nibas.sporing.Gyldig
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
class GrunnkretsController(
    private val grunnkretsService: GrunnkretsService,
) : GrunnkretsApi {

    override fun hentGrunnkrets(lokalid: String, gyldighetsdato: LocalDate?): GrunnkretsResponse {
        val identifikator = createIdentifikator(lokalid, gyldighetsdato)
        return grunnkretsService.hentGrunnkrets(identifikator)?.toGrunnkretsResponse(identifikator)
            ?: throw NotFoundException("Grunnkrets $identifikator ble ikke funnet.")
    }

    override fun hentGrenserForGrunnkrets(lokalid: String, gyldighetsdato: LocalDate?): FeatureCollection {
        val identifikator = createIdentifikator(lokalid, gyldighetsdato)
        val grensekoblinger = grunnkretsService.hentGrenserForGrunnkrets(identifikator)
        if (grensekoblinger.isEmpty()) {
            throw NotFoundException("Fant ingen grenser for $identifikator")
        }
        return FeatureCollection(
            features = grensekoblinger
                .sortedBy { it.grenseKoblingAttributter.rekkefoelge }
                .map { grenseKoblingToFeature(it, KontekstEnhetType.GRUNNKRETS, identifikator.gyldighetsdato) }
        )
    }

    override fun hentFramtidigeVersjonerForGrunnkrets(lokalid: String, gyldighetsdato: LocalDate?): List<GrunnkretsResponse> {
        val identifikatorForGrunnkrets = createIdentifikator(lokalid, gyldighetsdato)
        return grunnkretsService.hentFramtidigeVersjonerForGrunnkrets(identifikatorForGrunnkrets)
            .map {
                it.toGrunnkretsResponse(
                    Identifikator(
                        lokalid = Lokalid(it.lokalid),
                        gyldighetsdato = it.gyldighet.gyldigFra)
                )
            }
    }

}

@Transactional
@Service
class GrunnkretsService(private val grunnkretsRepository: GrunnkretsRepository) {

    @Transactional(readOnly = true)
    fun hentGrunnkrets(identifikator: Identifikator): Grunnkrets? = grunnkretsRepository.findGyldigObjektForIdentifikator(identifikator)

    @Transactional(readOnly = true)
    fun hentGrenserForGrunnkrets(identifikator: Identifikator): List<GrenseKobling> {
        return hentGrunnkrets(identifikator)?.getGrenser(identifikator.gyldighetsdato) ?: emptyList()
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
                        this.auditInfo.typeEndring = grunnkretsRequestWrapper.endringstype
                        this.version = grunnkretsHelper.getNextVersion()
                    }
            }
    }

    /**
     * Returnerer liste med grunnkretser som matcher lokalid og gyldighetsdato.
     * Nåværende gyldig rad og fremtidig publiserte gyldige rader.
     *
     * -Første innslag er raden som er gyldig på gyldighetsdato, default dagens dato.
     * -Så følger de framtidige gyldige publiserte versjonene sortert på gyldigFra.
     */
    fun hentFramtidigeVersjonerForGrunnkrets(identifikator: Identifikator): List<Grunnkrets> {
        val gyldigGrunnkretsForDagensDato = grunnkretsRepository.findGyldigObjektForIdentifikator(identifikator)
            ?: throw UgyldigTilstandException("Det finnes ingen gyldig grunnkrets for identifikator $identifikator")
        return listOf(gyldigGrunnkretsForDagensDato)
            .plus(grunnkretsRepository.findAllFramtidigeVersjonerByIdentifikator(identifikator))
    }

    fun hentAntallGyldigeVersjonerFramITidFor(identifikator: Identifikator): Int = grunnkretsRepository.findAntallFramtidigeVersjonerForIdentifikator(identifikator)

}

private class GrunnkretsHelper(private val grunnkretserSortedByGyldigFra: List<Grunnkrets>) {

    fun findRelevantGrunnkretsForGyldigFra(utkastetsGyldigFra: LocalDate, id: Lokalid): Grunnkrets =
        grunnkretserSortedByGyldigFra.find { grunnkrets -> grunnkrets.gyldigPaaDato(utkastetsGyldigFra) }
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
        opphav = relevantGrunnkrets.opphav
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

fun Grunnkrets.toGrunnkretsRef(identifikator: Identifikator, antallFramtidigeVersjoner: Int = 0, hrefProvider: HrefProvider): GrunnkretsRef =
    GrunnkretsRef(
        id = identifikator.toObjektIdentifikator(),
        navn = this.grunnkretsnavn!!,
        grunnkretsnummer = this.grunnkretsnummer!!,
        href = hrefProvider.getHref(identifikator),
        antallFramtidigeVersjoner = antallFramtidigeVersjoner
    )


fun Grunnkrets.toGrunnkretsResponse(identifikator: Identifikator): GrunnkretsResponse =
    GrunnkretsResponse(
        id = identifikator.toObjektIdentifikator(),
        navn = this.grunnkretsnavn!!,
        grunnkretsnummer = this.grunnkretsnummer!!,
        gyldighet = this.gyldighet.toGyldighetResponse(),
        oppdateringsdato = this.auditInfo.endretdato,
        endringstype = this.auditInfo.typeEndring.value,
        features = FeatureCollection(features = listOf(
            posisjonToFeature(this.posisjon, this.version),
            flateToFeature(this, this.version))),
        version = this.version
    )


fun Grunnkrets.toKretsResponse(kodelisteService: KodelisteService): KretsResponse = KretsResponse(
    id = this.lokalid,
    navn = this.grunnkretsnavn!!,
    nummer = this.grunnkretsnummer!!,
    kommunenummer = kodelisteService.createKommunenummerFromUUID(this.kommune.kommunenummer),
    features = FeatureCollection(
        features = listOf(
            posisjonToFeature(this.posisjon, this.version),
            flateToFeature(this, this.version)
        )
    )
)

data class GrunnkretsRequestWrapper(
    val grunnkretsRequest: GrunnkretsRequest,
    val gyldigFra: LocalDate,
    val endringstype: Endringstype,
    val resolved: Boolean = false
)
