package no.kartverket.nibas.inndelinger

import no.kartverket.nibas.api.v1.GrunnkretsApi
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.common.geojson.metadata.Identifikasjon
import no.kartverket.nibas.api.v1.request.GrunnkretsRequest
import no.kartverket.nibas.api.v1.response.GrunnkretsRef
import no.kartverket.nibas.api.v1.response.GrunnkretsResponse
import no.kartverket.nibas.api.v1.response.RevisjonRef
import no.kartverket.nibas.domain.GrenseKobling
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.GrunnkretsRepository
import no.kartverket.nibas.unwrap
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@RestController
class GrunnkretsController @Autowired constructor(
    private val grunnkretsService: GrunnkretsService,
    private val kodelisteService: KodelisteService,
    private val historikkService: HistorikkService
) : GrunnkretsApi {

    override fun hentEn(id: String): GrunnkretsResponse {
        return when (val grunnkrets = grunnkretsService.hentGrunnkrets(id)) {
            null -> throw ResponseStatusException(HttpStatus.NOT_FOUND)
            else -> toGrunnkretsResponse(grunnkrets, kodelisteService)
        }
    }

    override fun hentGrenser(id: String): FeatureCollection {
        val grenser = grunnkretsService.hentGrenserForGrunnkrets(id)
        if (grenser.isEmpty()) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
        }
        return FeatureCollection(
            features = grenser.sortedBy { it.grenseKoblingAttributter.rekkefoelge }
                .map { grenseKoblingToFeature(it, kodelisteService) }
        )
    }

    override fun oppdaterGrunnkrets(id: String, oppdatertGrunnkrets: GrunnkretsRequest): GrunnkretsResponse {
        return when (val lagret = grunnkretsService.oppdaterGrunnkrets(id, oppdatertGrunnkrets)) {
            null -> throw ResponseStatusException(HttpStatus.NOT_FOUND)
            else -> toGrunnkretsResponse(lagret, kodelisteService)
        }
    }

    override fun hentGrunnkretsRevisjoner(id: String): List<RevisjonRef> =
        historikkService.hentRevisjoner(id, Grunnkrets::class.java).map { toRevisjonRef(it) }


    override fun hentGrunnkretsRevisjon(id: String, revisjon: Int): GrunnkretsResponse {
        return toGrunnkretsResponse(
            historikkService.hentRevisjon(id, revisjon, Grunnkrets::class.java),
            kodelisteService
        )
    }
}

@Service
class GrunnkretsService constructor(private val grunnkretsRepository: GrunnkretsRepository) {

    fun hentGrunnkrets(id: String) = grunnkretsRepository.findById(id).unwrap()

    @Transactional
    fun hentGrenserForGrunnkrets(id: String): Set<GrenseKobling> {
        return hentGrunnkrets(id)?.grenser ?: emptySet()
    }

    fun oppdaterGrunnkrets(id: String, oppdatertGrunnkrets: GrunnkretsRequest): Grunnkrets? {
        return hentGrunnkrets(id)?.let { gammel: Grunnkrets ->
            gammel.grunnkretsnavn = oppdatertGrunnkrets.navn
            gammel.grunnkretsnummer = oppdatertGrunnkrets.grunnkretsnummer

            grunnkretsRepository.save(gammel)
        }
    }
}

fun toGrunnkretsRef(grunnkrets: Grunnkrets): GrunnkretsRef = GrunnkretsRef(
    id = grunnkrets.id,
    navn = grunnkrets.grunnkretsnavn!!,
    href = ServletUriComponentsBuilder.fromCurrentRequestUri().pathSegment(grunnkrets.id).build().toUriString()
)

fun toGrunnkretsResponse(grunnkrets: Grunnkrets, kodelisteService: KodelisteService): GrunnkretsResponse = GrunnkretsResponse(
    id = grunnkrets.id,
    navn = grunnkrets.grunnkretsnavn!!,
    grunnkretsnummer = grunnkrets.grunnkretsnummer!!,
    identifikasjon = Identifikasjon(grunnkrets.lokalid, grunnkrets.navnerom, grunnkrets.versjonid),
    kommunenummer = grunnkrets.kommunenummer!!,
    features = FeatureCollection(features = listOf(
        posisjonToFeature(grunnkrets.posisjon),
        flateToFeature(grunnkrets, kodelisteService)))
)

