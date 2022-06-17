package no.kartverket.nibas.inndelinger

import no.kartverket.nibas.api.v1.NasjonApi
import no.kartverket.nibas.api.v1.common.AdministrativEnhetNavn
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.request.NasjonRequest
import no.kartverket.nibas.api.v1.response.NasjonResponse
import no.kartverket.nibas.api.v1.response.RevisjonRef
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
            else -> toNasjonResponse(nasjon, kodelisteService)
        }
    }

    override fun hentGrenser(id: String): FeatureCollection {
        val grenser = nasjonService.hentGrenserForNasjon(id)
        if (grenser.isEmpty()) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
        }
        return FeatureCollection(
            features = grenser
                // TODO: NIBAS-473 Hvordan sortere grensene nå?
//                .sortedBy { it.grenseKoblingAttributter.rekkefoelge }
                .map { grenseKoblingToFeature(it, kodelisteService) }
        )
    }

    override fun oppdaterNasjon(id: String, oppdatertNasjon: NasjonRequest): NasjonResponse {
        return when (val lagret = nasjonService.oppdaterNasjon(id, oppdatertNasjon)) {
            null -> throw ResponseStatusException(HttpStatus.NOT_FOUND)
            else -> toNasjonResponse(lagret, kodelisteService)
        }
    }

    override fun hentNasjonRevisjoner(id: String): List<RevisjonRef> =
        historikkService.hentRevisjoner(id, Nasjon::class.java).map { toRevisjonRef(it) }

    override fun hentNasjonRevisjon(id: String, revisjon: Int): NasjonResponse {
        return toNasjonResponse(
            historikkService.hentRevisjon(id, revisjon, Nasjon::class.java),
            kodelisteService
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

    fun hentGrenserForNasjon(id: String): Set<GrenseKobling> {
        return nasjonRepository.findById(id).unwrap()?.getGrenser() ?: emptySet()
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

fun toNasjonResponse(nasjon: Nasjon, kodelisteService: KodelisteService): NasjonResponse = NasjonResponse(
    id = nasjon.id,
    administrativenhetnavn = nasjon.administrativenhetnavn.map { AdministrativEnhetNavn(it.navn!!, it.spraak?.id!!) },
    lokalid = nasjon.lokalid,
    navnerom = nasjon.navnerom,
    features = FeatureCollection(features = listOf(posisjonToFeature(nasjon.posisjon), flateToFeature(nasjon, kodelisteService)))
)
