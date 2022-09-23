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
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@RestController
class GrunnkretsController(
    private val grunnkretsService: GrunnkretsService,
    private val kodelisteService: KodelisteService,
    private val historikkService: HistorikkService,
    private val grunnkretsControllerService: GrunnkretsControllerService
) : GrunnkretsApi {

    override fun hentGrunnkrets(id: String): GrunnkretsResponse {
        return grunnkretsControllerService.hentEn(id)
    }

    override fun hentGrenserForGrunnkrets(id: String): FeatureCollection {
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
        return grunnkretsService.oppdaterGrunnkrets(id, oppdatertGrunnkrets)?.toGrunnkretsResponse(kodelisteService)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

    override fun hentGrunnkretsRevisjoner(id: String): List<RevisjonRef> =
        historikkService.hentRevisjoner(id, Grunnkrets::class.java).map { toRevisjonRef(it) }


    override fun hentGrunnkretsRevisjon(id: String, revisjon: Int): GrunnkretsResponse =
        historikkService.hentRevisjon(id, revisjon, Grunnkrets::class.java).toGrunnkretsResponse(kodelisteService)

}

@Service
class GrunnkretsControllerService(private val grunnkretsService: GrunnkretsService,
                                  private val kodelisteService: KodelisteService) {
    fun hentEn(id: String): GrunnkretsResponse =
        grunnkretsService.hentGrunnkrets(id)?.toGrunnkretsResponse(kodelisteService)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)

}


@Service
class GrunnkretsService(private val grunnkretsRepository: GrunnkretsRepository) {

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
    grunnkretsnummer = grunnkrets.grunnkretsnummer!!,
    href = ServletUriComponentsBuilder.fromCurrentRequestUri().pathSegment(grunnkrets.id).build().toUriString()
)

fun Grunnkrets.toGrunnkretsResponse(kodelisteService: KodelisteService): GrunnkretsResponse = GrunnkretsResponse(
    id = this.id,
    navn = this.grunnkretsnavn!!,
    grunnkretsnummer = this.grunnkretsnummer!!,
    identifikasjon = Identifikasjon(this.lokalid, this.navnerom, this.versjonid),
    kommunenummer = this.kommunenummer!!,
    features = FeatureCollection(features = listOf(
        posisjonToFeature(this.posisjon),
        flateToFeature(this, kodelisteService)))
)

