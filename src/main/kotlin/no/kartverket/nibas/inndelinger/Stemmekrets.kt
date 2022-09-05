package no.kartverket.nibas.inndelinger

import no.kartverket.nibas.api.v1.StemmekretsApi
import no.kartverket.nibas.api.v1.common.KontekstEnhetType
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.common.geojson.metadata.Identifikasjon
import no.kartverket.nibas.api.v1.request.StemmekretsRequest
import no.kartverket.nibas.api.v1.response.RevisjonRef
import no.kartverket.nibas.api.v1.response.StemmekretsRef
import no.kartverket.nibas.api.v1.response.StemmekretsResponse
import no.kartverket.nibas.domain.GrenseKobling
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.KommuneRepository
import no.kartverket.nibas.repository.StemmekretsRepository
import no.kartverket.nibas.unwrap
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@RestController
class StemmekretsController(
    private val stemmekretsService: StemmekretsService,
    private val kodelisteService: KodelisteService,
    private val historikkService: HistorikkService,
    private val stemmekretsControllerService: StemmekretsControllerService
) : StemmekretsApi {

    override fun hentStemmekrets(id: String): StemmekretsResponse {
        return stemmekretsControllerService.hentEn(id)
    }

    override fun hentGrenserForStemmekrets(id: String): FeatureCollection {
        val grenser = stemmekretsService.hentGrenserForStemmekrets(id)
        if (grenser.isEmpty()) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
        }
        return FeatureCollection(
            features = grenser.sortedBy { it.grenseKoblingAttributter.rekkefoelge }
                .map { grenseKoblingToFeature(it, kodelisteService, KontekstEnhetType.STEMMEKRETS) }
        )
    }

    override fun oppdaterStemmekrets(id: String, oppdatertStemmekrets: StemmekretsRequest): StemmekretsResponse {
        return when (val lagret = stemmekretsService.oppdaterStemmekrets(id, oppdatertStemmekrets)) {
            null -> throw ResponseStatusException(HttpStatus.NOT_FOUND)
            else -> toStemmekretsResponse(lagret, kodelisteService)
        }
    }

    override fun hentStemmekretsRevisjoner(id: String): List<RevisjonRef> =
        historikkService.hentRevisjoner(id, Stemmekrets::class.java).map { toRevisjonRef(it) }


    override fun hentStemmekretsRevisjon(id: String, revisjon: Int): StemmekretsResponse {
        return toStemmekretsResponse(
            historikkService.hentRevisjon(id, revisjon, Stemmekrets::class.java),
            kodelisteService
        )
    }
}

@Service
class StemmekretsControllerService(private val stemmekretsService: StemmekretsService,
                                   private val kodelisteService: KodelisteService) {
    fun hentEn(id: String): StemmekretsResponse {
        return when (val stemmekrets = stemmekretsService.hentStemmekrets(id)) {
            null -> throw ResponseStatusException(HttpStatus.NOT_FOUND)
            else -> toStemmekretsResponse(stemmekrets, kodelisteService)
        }
    }
}

@Service
class StemmekretsService @Autowired constructor(private val stemmekretsRepository: StemmekretsRepository, private val kommuneRepository: KommuneRepository) {

    fun hentStemmekrets(id: String) = stemmekretsRepository.findById(id).unwrap()

    @Transactional
    fun hentGrenserForStemmekrets(id: String): Set<GrenseKobling> {
        return hentStemmekrets(id)?.grenser ?: emptySet()
    }

    fun oppdaterStemmekrets(id: String, oppdatertStemmekrets: StemmekretsRequest): Stemmekrets? {
        return hentStemmekrets(id)?.let { gammel: Stemmekrets ->
            gammel.stemmekretsnavn = oppdatertStemmekrets.stemmekretsnavn
            gammel.stemmekretsnummer = oppdatertStemmekrets.stemmekretsnummer
            gammel.tellekretsnavn = oppdatertStemmekrets.tellekretsnavn
            gammel.tellekretsnummer = oppdatertStemmekrets.tellekretsnummer
            gammel.valgdistriktsnummer = oppdatertStemmekrets.valgdistriktsnummer

            stemmekretsRepository.save(gammel)
        }
    }
}

fun toStemmekretsRef(stemmekrets: Stemmekrets): StemmekretsRef = StemmekretsRef(
    id = stemmekrets.id,
    navn = stemmekrets.stemmekretsnavn!!,
    href = ServletUriComponentsBuilder.fromCurrentRequestUri().pathSegment(stemmekrets.id).build().toUriString()
)

fun toStemmekretsResponse(stemmekrets: Stemmekrets, kodelisteService: KodelisteService): StemmekretsResponse = StemmekretsResponse(
    id = stemmekrets.id,
    stemmekretsnavn = stemmekrets.stemmekretsnavn!!,
    stemmekretsnummer = stemmekrets.stemmekretsnummer!!,
    identifikasjon = Identifikasjon(stemmekrets.lokalid, stemmekrets.navnerom, stemmekrets.versjonid),
    kommunenummer = stemmekrets.kommunenummer!!,
    valgdistriktsnummer = stemmekrets.valgdistriktsnummer,
    tellekretsnavn = stemmekrets.tellekretsnavn,
    tellekretsnummer = stemmekrets.tellekretsnummer,
    features = FeatureCollection(features = listOf(
        posisjonToFeature(stemmekrets.posisjon),
        flateToFeature(stemmekrets, kodelisteService)))
)

