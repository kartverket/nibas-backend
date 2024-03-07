package no.kartverket.nibas.controller.ekstern

import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.ekstern.StemmekretsApi
import no.kartverket.nibas.api.v1.ekstern.extensions.flate.toEksternKretsResponse
import no.kartverket.nibas.api.v1.ekstern.extensions.flate.toEksternStemmekretsResponse
import no.kartverket.nibas.api.v1.ekstern.response.EksternKretsResponse
import no.kartverket.nibas.api.v1.ekstern.response.EksternStemmekretsResponse
import no.kartverket.nibas.common.ResponseException.NotFoundException
import no.kartverket.nibas.common.createIdentifikator
import no.kartverket.nibas.extensions.geometry.grenseKoblingToFeature
import no.kartverket.nibas.service.StemmekretsService
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController("EksternStemmekretsController")
class StemmekretsController(
    private val stemmekretsService: StemmekretsService
) : StemmekretsApi {

    override fun hentStemmekrets(lokalid: String, gyldighetsdato: LocalDate?): EksternStemmekretsResponse {
        val identifikator = createIdentifikator(lokalid, gyldighetsdato)
        return stemmekretsService.hentStemmekrets(identifikator)?.toEksternStemmekretsResponse(identifikator)
            ?: throw NotFoundException("Stemmekrets $identifikator ble ikke funnet.")
    }

    override fun hentStemmekretserForIds(ids: Set<String>, gyldighetsdato: LocalDate?): List<EksternKretsResponse> {
        return stemmekretsService.hentStemmekretserForIds(ids, gyldighetsdato ?: LocalDate.now()).map { it.toEksternKretsResponse() }
    }

    override fun hentGrenserForStemmekrets(lokalid: String, gyldighetsdato: LocalDate?): FeatureCollection {
        val identifikator = createIdentifikator(lokalid, gyldighetsdato)
        val grensekoblinger = stemmekretsService.hentGrensekoblingerForStemmekrets(identifikator)
        if (grensekoblinger.isEmpty()) {
            throw NotFoundException("Fant ingen grenser for $identifikator")
        }
        return FeatureCollection(
            features = grensekoblinger
                .map { it.grenseKoblingToFeature(identifikator.gyldighetsdato) }
        )
    }
}
