package no.kartverket.nibas.inndelinger

import no.kartverket.nibas.api.v1.NasjonApi
import no.kartverket.nibas.api.v1.common.AdministrativEnhetNavn
import no.kartverket.nibas.api.v1.common.KontekstEnhetType
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.request.NasjonRequest
import no.kartverket.nibas.api.v1.response.HistorikkRef
import no.kartverket.nibas.api.v1.response.NasjonResponse
import no.kartverket.nibas.domain.GrenseKobling
import no.kartverket.nibas.domain.Nasjon
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.AdministrativenhetnavnRepository
import no.kartverket.nibas.repository.NasjonRepository
import no.kartverket.nibas.unwrap
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import no.kartverket.nibas.api.v1.response.NasjonRef as NasjonRefResponse

@RestController
class NasjonController @Autowired constructor(
    private val nasjonService: NasjonService,
    private val historikkService: HistorikkService,
    private val kodelisteService: KodelisteService
) : NasjonApi {

    override fun hentAlle(): List<NasjonRefResponse> = nasjonService.hentAlleNasjoner().map { toNasjonRef(it) }

    override fun hentEn(id: String): NasjonResponse {
        return when (val nasjon = nasjonService.hentNasjon(id)) {
            null -> throw ResponseStatusException(HttpStatus.NOT_FOUND)
            else -> toNasjonResponse(nasjon)
        }
    }

    override fun hentGrenser(id: String): FeatureCollection {
        return when (val grenser = nasjonService.hentGrenserForNasjon(id)) {
            null -> throw ResponseStatusException(HttpStatus.NOT_FOUND)
            else -> FeatureCollection(
                features = grenser.sortedBy { it.grenseKoblingAttributter.rekkefoelge }
                    .map { grenseKoblingToFeature(id, KontekstEnhetType.NASJON, it, kodelisteService) }
            )
        }
    }

    override fun oppdaterNasjon(id: String, oppdatertNasjon: NasjonRequest): NasjonResponse {
        return when (val lagret = nasjonService.oppdaterNasjon(id, oppdatertNasjon)) {
            null -> throw ResponseStatusException(HttpStatus.NOT_FOUND)
            else -> toNasjonResponse(lagret)
        }
    }

    override fun hentHistorikkForEn(id: String): List<HistorikkRef> =
        historikkService.hentHistorikk(id, Nasjon::class.java).map { toHistorikkRef(it) }

    override fun hentHistorikkRevisjon(id: String, revision: Int): NasjonResponse {
        return toNasjonResponse(
            historikkService.hentHistorikkRevisjon(id, revision, Nasjon::class.java)
        )
    }
}

@Service
class NasjonService @Autowired constructor(
    private val nasjonRepository: NasjonRepository,
    private val administrativenhetnavnRepository: AdministrativenhetnavnRepository
) {

    fun hentAlleNasjoner() = nasjonRepository.findAll().toList()
    fun hentNasjon(id: String) = nasjonRepository.findById(id).unwrap()

    fun hentGrenserForNasjon(id: String): Set<GrenseKobling>? {
        return nasjonRepository.findById(id).unwrap()?.let { nasjon ->
            nasjon.riksgrenser +
                    nasjon.territorialgrenser +
                    nasjon.avtaltavgrensningslinjer
        }

    }

    fun oppdaterNasjon(id: String, oppdatertNasjon: NasjonRequest): Nasjon? {
        return hentNasjon(id)?.let { gammel ->
            gammel.navnerom = oppdatertNasjon.navnerom
            gammel.lokalid = oppdatertNasjon.lokalid

            // Finner basert på språkkode, og oppdaterer den
            oppdatertNasjon.administrativenhetnavn.forEach { navn ->
                gammel.administrativenhetnavn.first { it.spraak?.id == navn.spraak }.let { gammeltNavn ->
                    gammeltNavn.navn = navn.navn
                    administrativenhetnavnRepository.save(gammeltNavn)
                }
            }
            nasjonRepository.save(gammel)
        }
    }
}

fun toNasjonRef(nasjon: Nasjon): NasjonRefResponse = NasjonRefResponse(
    id = nasjon.id,
    navn = nasjon.administrativenhetnavn.map { AdministrativEnhetNavn(it.navn!!, it.spraak?.id!!) },
    href = ServletUriComponentsBuilder.fromCurrentRequestUri().pathSegment(nasjon.id).build().toUriString()
)

fun toNasjonResponse(nasjon: Nasjon): NasjonResponse = NasjonResponse(
    id = nasjon.id,
    administrativenhetnavn = nasjon.administrativenhetnavn.map { AdministrativEnhetNavn(it.navn!!, it.spraak?.id!!) },
    lokalid = nasjon.lokalid,
    navnerom = nasjon.navnerom,
    features = FeatureCollection(features = listOf(posisjonToFeature(nasjon.posisjon), flateToFeature(nasjon.flate)))
)
