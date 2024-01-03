package no.kartverket.nibas.controller.ekstern

import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.ekstern.GrunnkretsApi
import no.kartverket.nibas.api.v1.response.KretsResponse
import no.kartverket.nibas.common.ResponseException
import no.kartverket.nibas.common.createIdentifikator
import no.kartverket.nibas.extensions.flate.toKretsResponse
import no.kartverket.nibas.extensions.geometry.grenseKoblingToFeature
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.service.GrunnkretsService
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController("EksternGrunnkretsController")
class GrunnkretsController(
    private val grunnkretsService: GrunnkretsService,
    private val kodelisteService: KodelisteService,
) : GrunnkretsApi {

    override fun hentGrunnkretserForIds(ids: Set<String>, gyldighetsdato: LocalDate?): List<KretsResponse> {
        return grunnkretsService.hentGrunnkretserForIds(ids, gyldighetsdato ?: LocalDate.now()).map { it.toKretsResponse() }
    }

    override fun hentGrenserForGrunnkrets(lokalid: String, gyldighetsdato: LocalDate?): FeatureCollection {
        val identifikator = createIdentifikator(lokalid, gyldighetsdato)
        val grensekoblinger = grunnkretsService.hentGrenserForGrunnkrets(identifikator)
        if (grensekoblinger.isEmpty()) {
            throw ResponseException.NotFoundException("Fant ingen grenser for $identifikator")
        }
        return FeatureCollection(
            features = grensekoblinger
                .sortedBy { it.grenseKoblingAttributter.rekkefoelge }
                .map { it.grenseKoblingToFeature(identifikator.gyldighetsdato) }
        )
    }
}
