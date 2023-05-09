package no.kartverket.nibas.controller.ekstern

import no.kartverket.nibas.api.v1.common.KontekstEnhetType
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.ekstern.StemmekretsApi
import no.kartverket.nibas.api.v1.response.KretsResponse
import no.kartverket.nibas.api.v1.response.StemmekretsResponse
import no.kartverket.nibas.common.ResponseException.NotFoundException
import no.kartverket.nibas.common.createIdentifikator
import no.kartverket.nibas.extensions.flate.toKretsResponse
import no.kartverket.nibas.extensions.flate.toStemmekretsResponse
import no.kartverket.nibas.extensions.geometry.grenseKoblingToFeature
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.service.StemmekretsService
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController("EksternStemmekretsController")
class StemmekretsController(
    private val stemmekretsService: StemmekretsService,
    private val kodelisteService: KodelisteService,
) : StemmekretsApi {

    override fun hentStemmekrets(lokalid: String, gyldighetsdato: LocalDate?): StemmekretsResponse {
        val identifikator = createIdentifikator(lokalid, gyldighetsdato)
        return stemmekretsService.hentStemmekrets(identifikator)?.toStemmekretsResponse(identifikator, kodelisteService)
            ?: throw NotFoundException("Stemmekrets $identifikator ble ikke funnet.")
    }

    override fun hentStemmekretserForIds(ids: Set<String>, gyldighetsdato: LocalDate?): List<KretsResponse> {
        return stemmekretsService.hentStemmekretserForIds(ids, gyldighetsdato ?: LocalDate.now()).map { it.toKretsResponse(kodelisteService) }
    }

    override fun hentGrenserForStemmekrets(lokalid: String, gyldighetsdato: LocalDate?): FeatureCollection {
        val identifikator = createIdentifikator(lokalid, gyldighetsdato)
        val grenser = stemmekretsService.hentGrenserForStemmekrets(identifikator)
        if (grenser.isEmpty()) {
            throw NotFoundException("Fant ingen grenser for $identifikator")
        }
        return FeatureCollection(
            features = grenser
                .sortedBy { it.grenseKoblingAttributter.rekkefoelge }
                .map { it.grenseKoblingToFeature(KontekstEnhetType.STEMMEKRETS, identifikator.gyldighetsdato) }
        )
    }
}
